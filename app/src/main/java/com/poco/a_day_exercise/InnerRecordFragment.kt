package com.poco.a_day_exercise

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.FragmentInnerRecordBinding
import com.poco.a_day_exercise.databinding.ItemExerciseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordExercise (
	val exname: String? = null,
	val setnumber: String? = null,
	val weight: String? = null,
	val numofexercise: String? = null
)

class InnerRecordFragment : Fragment() {

	val binding by lazy { FragmentInnerRecordBinding.inflate(layoutInflater) }

	private lateinit var recordexerciseAdapter: RecordExerciseAdapter

	// 임시 아이템을 저장할 리스트
	private val temporaryrecordExercises = mutableListOf<RecordExercise>()

	private lateinit var sharedViewModel: SharedViewModel

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		recordexerciseAdapter = RecordExerciseAdapter(temporaryrecordExercises)
		// 리니어 레이아웃 매니저 설정
		val layoutManager = LinearLayoutManager(requireContext())
		binding.exRecyclerView.layoutManager = layoutManager
		binding.exRecyclerView.adapter = recordexerciseAdapter

		sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

		sharedViewModel.getSelectedDate().observe(viewLifecycleOwner, Observer { selectedDate ->
			// 선택한 날짜를 사용하여 데이터 업데이트
			updateDataForSelectedDate(selectedDate)
		})

		val today = getCurrentDate()

		// 파이어스토어에서 데이터 불러오기
		updateDataForSelectedDate(today)

		return binding.root
	}

	private fun updateDataForSelectedDate(selectedDate: String) {
		// 선택한 날짜를 기반으로 필요한 정보 업데이트
		// 예: 데이터베이스에서 해당 날짜의 운동 기록을 가져와 화면에 표시
		val datetext = getString(R.string.datetext, selectedDate)
		binding.Datetext.text = datetext

		temporaryrecordExercises.clear()
		loadRecords(selectedDate)
	}

	fun getCurrentDate(): String {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
		val currentDate = Date()
		return dateFormat.format(currentDate)
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

	private fun loadRecords(selectedDate: String) {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email
		Log.d("checkDate", selectedDate)

		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection(selectedDate) // 해당 루틴 이름의 컬렉션에 접근
			.get()
			.addOnSuccessListener { exerciseDocuments ->
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