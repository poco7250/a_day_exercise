package com.poco.a_day_exercise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.AddExerciseBinding
import com.poco.a_day_exercise.databinding.ItemExerciseBinding
import com.poco.a_day_exercise.databinding.ItemFriendBinding

data class Exercise (
	val exname: String? = null,
	val setnumber: String? = null,
	val weight: String? = null,
	val numofexercise: String? = null
)

class AddExerciseActivity : AppCompatActivity() {
	// ExerciseAdapter 인스턴스를 멤버 변수로 선언
	private lateinit var exerciseAdapter: ExerciseAdapter

	private val binding by lazy { AddExerciseBinding.inflate(layoutInflater)}

	// 임시 아이템을 저장할 리스트
	private val temporaryExercises = mutableListOf<Exercise>()
	// 선택된 아이템을 저장
	private var selectedRoutine: Routine? = null
	// 아이템 위치 추적
	private var selectedItemPosition = -1

	companion object {
		const val ADD_EXERCISE_REQUEST_CODE = 111 // 원하는 값으로 설정
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		onBackPressedDispatcher.addCallback(this,onBackPressedCallback) // 뒤로가기 콜백

		// ExerciseAdapter 인스턴스 초기화
		exerciseAdapter = ExerciseAdapter(temporaryExercises)
		// 리니어 레이아웃 매니저 설정
		val layoutManager = LinearLayoutManager(this)
		binding.exerciseRecyclerView.layoutManager = layoutManager
		binding.exerciseRecyclerView.adapter = exerciseAdapter

		val routinename = intent.getStringExtra("userroutinename")
		val selectedroutine = intent.getStringExtra("selectedRoutine")
		val rtname = intent.getStringExtra("RoutineName")

		if (routinename != null) {
			checkAndLoadExercises(routinename)
		} else if (selectedroutine != null){
			checkAndLoadExercises(selectedroutine)
		} else {
			checkAndLoadExercises(rtname!!)
		}
		binding.addExercise.setOnClickListener {
			val userExerciseName: String = binding.insertExerciseName.text.toString()
			val userSetNumber: String = binding.numOfSet.text.toString()
			val userWeight: String = binding.weightuser.text.toString()
			val userNumOfExercise: String = binding.numOfEx.text.toString()

			addExercise(userExerciseName, userSetNumber, userWeight, userNumOfExercise)
			exerciseAdapter.notifyDataSetChanged()
		}

		binding.saveExercise.setOnClickListener {
			saveExercise()
		}

		binding.successButton1.setOnClickListener {
			setResult(Activity.RESULT_OK) // 데이터가 변경되었음을 알림
			finish()
		}

//		binding.mrnButton.setOnClickListener {
//			// 다음 액티비티로 전달
//			val intent = Intent(this, ModifyRoutineNameActivity::class.java)
//			intent.putExtra("RoutineName", rtname)
//			startActivity(intent)
//		}

		binding.root.setOnClickListener {
			selectedItemPosition = -1 // 선택 해제
			exerciseAdapter.notifyDataSetChanged()
		}
	}

	// 리사이클러 뷰
	inner class ExerciseAdapter(private val exercises: List<Exercise>) :
		RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

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
			holder.deleteexbutton.setOnClickListener {
				// 버튼을 누를 시 리사이클러 뷰에서 삭제
				deleteExercise(position, currentExercise)
			}
			holder.itemView.setOnClickListener {
				// 선택된 아이템의 위치를 갱신하고 갱신된 위치에 대한 UI 업데이트 요청
				selectedItemPosition = holder.adapterPosition
				notifyDataSetChanged()
			}
			// 아이템이 선택된 위치와 현재 위치가 같을 때 배경 변경
			if (selectedItemPosition == position) {
				holder.itemView.setBackgroundResource(R.drawable.item_border_background)
			} else {
				holder.itemView.setBackgroundResource(R.drawable.normal_item_background)
			}
		}

		private var onItemClickListener: ((Routine) -> Unit)? = null

		fun setOnItemClickListener(listener: (Routine) -> Unit) {
			onItemClickListener = listener
		}

		override fun getItemCount() = exercises.size

		inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
		RecyclerView.ViewHolder(binding.root){
			val exercisename: TextView = binding.exName
			val numofset: TextView = binding.setNumber
			val exweight: TextView = binding.weight
			val numofex: TextView = binding.numberOfTime
			val deleteexbutton: Button = binding.deleteExercise
		}

		private fun deleteItem(position: Int) {
			if (position >= 0 && position < exercises.size) {
				temporaryExercises.removeAt(position)
				notifyItemRemoved(position)
			}
		}

		private fun deleteExercise(position: Int, exercise: Exercise)
		{
			val db = FirebaseFirestore.getInstance()
			val routinename = intent.getStringExtra("selectedRoutine")
			Log.d("namecheck", "$routinename")
			val currentUserEmail = Firebase.auth.currentUser?.email

			if (currentUserEmail != null) {
				val selectedExerciseName = exercise.exname

				db.collection("Exercises")
					.document(currentUserEmail)
					.collection(routinename!!)
					.document(selectedExerciseName!!)
					.delete()
					.addOnSuccessListener {
						// 선택한 아이템 삭제
						deleteItem(position)
						// 문서 삭제 성공
						Toast.makeText(this@AddExerciseActivity, "데이터가 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
					}
					.addOnFailureListener {	exception ->
						// 문서 조회 실패
						Log.d("DeleteDocument", "Error fetching documents", exception)
						Toast.makeText(this@AddExerciseActivity, "문서 조회에 실패했습니다.", Toast.LENGTH_SHORT).show()

					}
			}
		}
	}

	// 추가 버튼 클릭 시 호출되는 함수
	private fun addExercise(userexname: String, usersetnums: String, userexweight: String, usernumofex: String) {
		// 임시 아이템 생성
		val exercise = Exercise(userexname, usersetnums, userexweight, usernumofex)

		if (userexname.isNotEmpty() && usersetnums.isNotEmpty() && userexweight.isNotEmpty() && usernumofex.isNotEmpty()) {// 임시 아이템을 리스트에 추가
			// 중복 체크
			if (!temporaryExercises.any { it.exname == userexname }) {
				// 임시 아이템을 리스트에 추가
				temporaryExercises.add(exercise)
				binding.insertExerciseName.text = null
				binding.numOfSet.text = null
				binding.weightuser.text = null
				binding.numOfEx.text = null
				Log.d("AddExercise", "Exercise added: $exercise")
			} else {
				Toast.makeText(this, "동일한 이름의 운동이 이미 존재합니다.", Toast.LENGTH_SHORT).show()
			}
		} else {
			Toast.makeText(this, "정보를 바르게 입력해주세요.", Toast.LENGTH_SHORT).show()
		}
	}

	private fun saveExercise() {
		val db = FirebaseFirestore.getInstance()
		val routinename = intent.getStringExtra("userroutinename") ?: intent.getStringExtra("selectedRoutine")
		val currentUserEmail = Firebase.auth.currentUser?.email

		if (temporaryExercises.isEmpty())
		{
			Toast.makeText(this, "운동을 추가해주세요.", Toast.LENGTH_SHORT).show()
		} else
		{
			// 루틴 이름 저장
			db.collection("Exercises")
				.document(currentUserEmail!!)
				.collection("RoutineList")
				.document(routinename!!)
				.set(hashMapOf<String, Any>())
				.addOnSuccessListener {
					// 저장 성공 시 처리
					Log.d("savecheck", "루틴 저장이 성공적으로 완료되었습니다.")
				}
				.addOnFailureListener {
					// 저장 실패 시 처리
					Log.d("savecheck", "루틴 저장에 실패하였습니다.")
				}

			// 파이어스토어에 운동 정보 저장
			for (exercise in temporaryExercises) {
				// 현재 로그인한 사용자의 이메일을 가져옵니다.
				val exercisename = exercise.exname
				val newexercises = hashMapOf(
					"setnumber" to exercise.setnumber,
					"weight" to exercise.weight,
					"numofexercise" to exercise.numofexercise
				)
				db.collection("Exercises")
					.document(currentUserEmail!!)
					.collection(routinename!!)
					.document(exercisename!!)
					.set(newexercises)
					.addOnSuccessListener {
						// 저장 성공 시 처리
						Log.d("savecheck", "저장이 성공적으로 완료되었습니다.")
						Toast.makeText(this, "저장이 완료되었습니다.", Toast.LENGTH_SHORT).show()
						setResult(Activity.RESULT_OK)
						finish()
					}
					.addOnFailureListener {
						// 저장 실패 시 처리
						Log.d("savecheck", "저장에 실패하였습니다.")
					}
			}
		}
	}

	// 루틴 이름 확인 후 데이터 불러오는 함수
	private fun checkAndLoadExercises(routinename: String) {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email

		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection(routinename)
			.get()
			.addOnSuccessListener { documents ->
				if (documents.isEmpty) {
					// 루틴 이름이 파이어스토어에 없는 경우 처리
					Log.d("checkAndLoadExercises", "No data found for the routine.")
					// 이 부분에서 필요한 처리를 하세요.
				} else {
					// 루틴 이름이 파이어스토어에 있는 경우 데이터 불러오기
					for (document in documents) {
						val exercisename = document.id
						val setnumber = document.getString("setnumber")
						val weight = document.getString("weight")
						val numofexercise = document.getString("numofexercise")
						val exercise = Exercise(exercisename, setnumber, weight, numofexercise)
						temporaryExercises.add(exercise)
					}
					exerciseAdapter.notifyDataSetChanged() // 데이터 불러온 후 업데이트
				}
			}
			.addOnFailureListener { exception ->
				Log.d("checkAndLoadExercises", "Error getting documents: ", exception)
			}
	}

	private val onBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			// 뒤로가기 시 실행할 코드
			Toast.makeText(this@AddExerciseActivity, "완료 혹은 취소버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
		}
	}

}