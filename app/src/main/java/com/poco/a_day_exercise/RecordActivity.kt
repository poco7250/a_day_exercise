package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.ActivityRecordBinding
import com.poco.a_day_exercise.databinding.FragmentStatisticsBinding
import com.poco.a_day_exercise.databinding.ItemExerciseBinding

data class RecordExercise (
	val exname: String? = null,
	val setnumber: String? = null,
	val weight: String? = null,
	val numofexercise: String? = null
)

class RecordActivity : AppCompatActivity() {
	private val binding by lazy { ActivityRecordBinding.inflate(layoutInflater)} // 뷰바인딩 설정

	private lateinit var recordexerciseAdapter: RecordExerciseAdapter

	// 임시 아이템을 저장할 리스트
	private val temporaryrecordExercises = mutableListOf<RecordExercise>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		val selectedDate = intent.getStringExtra("selectedDate")

		recordexerciseAdapter = RecordExerciseAdapter(temporaryrecordExercises)
		// 리니어 레이아웃 매니저 설정
		val layoutManager = LinearLayoutManager(this)
		binding.exRecyclerView.layoutManager = layoutManager
		binding.exRecyclerView.adapter = recordexerciseAdapter

		// 파이어스토어에서 데이터 불러오기
		loadRecords()

	}

	// 리사이클러 뷰
	class RecordExerciseAdapter(private val exercises: List<RecordExercise>) :
		RecyclerView.Adapter<RecordExerciseAdapter.ExerciseViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
			val binding =
				ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			return ExerciseViewHolder(binding)
		}

		override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
			val currentExercise = exercises[position]
			holder.exercisename.text = currentExercise.exname
			holder.numofset.text = currentExercise.setnumber
			holder.exweight.text = currentExercise.weight
			holder.numofex.text = currentExercise.numofexercise
			holder.deleteButton.visibility = View.GONE
		}

		override fun getItemCount() = exercises.size

		inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
			RecyclerView.ViewHolder(binding.root){
			val exercisename: TextView = binding.exName
			val numofset: TextView = binding.setNumber
			val exweight: TextView = binding.weight
			val numofex: TextView = binding.numberOfTime
			val deleteButton: Button = binding.deleteExercise
		}

	}

	private fun loadRecords() {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email
		val selectedDate = intent.getStringExtra("selectedDate")
		Log.d("checkDate", "$selectedDate")

		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection(selectedDate!!) // 해당 루틴 이름의 컬렉션에 접근
			.get()
			.addOnSuccessListener { exerciseDocuments ->
				val exercisesList = mutableListOf<RecordExercise>()
				for (exerciseDocument in exerciseDocuments) {
					val exname = exerciseDocument.id
					val setnumber = exerciseDocument.getString("setnumber")
					val weight = exerciseDocument.getString("weight")
					val numofexercise = exerciseDocument.getString("numofexercise")
					val exerciseData = RecordExercise(exname, setnumber, weight, numofexercise)
					temporaryrecordExercises.add(exerciseData)
				}
				recordexerciseAdapter.notifyDataSetChanged()
			}
			.addOnFailureListener { exception ->
				Log.d("loadExerciseData", "Error getting documents: ", exception)
			}
	}

}