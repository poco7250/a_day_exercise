package com.poco.a_day_exercise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.FragmentMainBinding
import com.poco.a_day_exercise.databinding.ItemExerciseBinding
import com.poco.a_day_exercise.databinding.ItemRoutineBinding
import com.poco.a_day_exercise.databinding.MainRoutineBinding

class RoutineComponent(
	val rtname: String? = null,
	var exercises: List<Exercises>? = null
)

data class Exercises (
	val exname: String? = null,
	val setnumber: String? = null,
	val weight: String? = null,
	val numofexercise: String? = null,
	val isDeletable: Boolean = false // 추가: 삭제 버튼 표시 여부를 결정하는 속성
)

class MainFragment : Fragment() {

	private val binding by lazy { FragmentMainBinding.inflate(layoutInflater)} // 뷰바인딩 설정

	// RoutineAdapter 인스턴스를 멤버 변수로 선언
	private lateinit var routineAdapter: RecyclerViewRoutineAdapter

	// 임시 아이템을 저장할 리스트
	private val temporaryRoutineComponent = mutableListOf<RoutineComponent>()

	// 루틴 이름을 저장할 리스트
	private val routineNamesList = mutableListOf<String>()

	// 아이템 위치 추적
	private var selectedItemPosition = -1

	// 다른 함수에서 사용할 수 있도록 클래스 레벨 변수로 rtname 저장
	private var selectedRoutineName: String? = null


	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment

		routineAdapter = RecyclerViewRoutineAdapter(temporaryRoutineComponent)
		// 리니어 레이아웃 매니저 설정
		val layoutManager = LinearLayoutManager(requireContext())
		binding.mainrcview.layoutManager = layoutManager
		binding.mainrcview.adapter = routineAdapter

		binding.startExercise.setOnClickListener {
			val intent = Intent(activity, AddRoutineActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			startActivity(intent)
		}

		binding.recordTextButton.setOnClickListener {
			val intent = Intent(activity, DateActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			startActivity(intent)
		}

		binding.recordWatchButton.setOnClickListener {
			val intent = Intent(activity,  RecordWatchActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			startActivity(intent)
		}

		// 파이어스토어에서 데이터 불러오기
		loadRoutineNames()

//		// RecyclerViewRoutineAdapter에 클릭 리스너 설정
//		routineAdapter.setOnItemClickListener(object : RecyclerViewRoutineAdapter.OnItemClickListener {
//			override fun onItemClick(position: Int) {
//				// 클릭한 아이템의 위치(position)을 이용하여 다음 액티비티로 데이터 전달
//				val clickedItem = temporaryRoutineComponent[position]
//				selectedRoutineName = clickedItem.rtname // rtname을 클래스 변수에 저장
//				val intent = Intent(requireActivity(), AddExerciseActivity::class.java)
//
//				// 클릭한 아이템의 데이터를 다음 액티비티로 전달 (예: 루틴 이름)
//				intent.putExtra("RoutineName", selectedRoutineName)
//
//				// 다음 액티비티로 이동
//				startActivity(intent)
//			}
//		})

		return binding.root
	}

	class RecyclerViewRoutineAdapter(private val items: List<RoutineComponent>) : RecyclerView.Adapter<RecyclerViewRoutineAdapter.RoutineViewHolder>() {

		// 이 변수를 추가해보세요.
		private val temporaryRoutineComponent = mutableListOf<RoutineComponent>()
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
			val binding = MainRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			return RoutineViewHolder(binding)
		}

		override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
			val currentItem = items[position]
			holder.routinename.text = currentItem.rtname
			holder.itemView.setBackgroundResource(R.drawable.item_border_background)

			// 삭제 버튼 클릭 이벤트 처리
			holder.plusbutton.setOnClickListener {
				// 클릭된 아이템의 정보를 가져와서 인텐트를 생성하고 액티비티를 시작
				val clickedItem = items[position]
				val selectedRoutineName = clickedItem.rtname // 예: 루틴 이름

				val intent = Intent(holder.itemView.context, AddExerciseActivity::class.java)
				intent.putExtra("RoutineName", selectedRoutineName)

				holder.itemView.context.startActivity(intent)
			}

			// 이너 리사이클러뷰 어댑터와 데이터 설정
			holder.setExerciseData(currentItem.exercises ?: emptyList())

			 // 아이템 클릭 이벤트 처리
			holder.itemView.setOnClickListener {
				// 클릭한 아이템의 위치(position)를 가져와 리스너에 전달
				onItemClickListener?.onItemClick(position)
			}
		}

		inner class RoutineViewHolder(private val binding: MainRoutineBinding) :
			RecyclerView.ViewHolder(binding.root){
			val routinename: TextView = binding.rtname
			val mainrcv: RecyclerView = binding.rcview
			val plusbutton: ImageButton = binding.plusButton

			fun setExerciseData(exercises: List<Exercises>) {

				// 리니어 레이아웃 매니저 설정
				val layoutManager = LinearLayoutManager(binding.root.context)
				val innerAdapter = ExerciseAdapter(exercises)
				mainrcv.layoutManager = layoutManager
				mainrcv.adapter = innerAdapter
			}


		}

		override fun getItemCount(): Int {
			return items.size
		}

		// 아이템 클릭 리스너 인터페이스 정의
		interface OnItemClickListener {
			fun onItemClick(position: Int)
		}

		private var onItemClickListener: OnItemClickListener? = null

		// 클릭 리스너 설정 메서드
		fun setOnItemClickListener(listener: OnItemClickListener) {
			this.onItemClickListener = listener
		}

		// 리사이클러 뷰
		inner class ExerciseAdapter(private val exercises: List<Exercises>) :
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
	}

	private fun loadRoutineNames() {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email

		if (currentUserEmail != null) {
			db.collection("Exercises")
				.document(currentUserEmail)
				.collection("RoutineList")
				.get()
				.addOnSuccessListener { documents ->
					for (document in documents) {
						val routineName = document.id
						routineNamesList.add(routineName)
					}
					// 루틴 이름들을 모두 가져온 후, 각 루틴 데이터를 불러올 수 있는 함수를 호출
					loadRoutineDataForAllRoutines(currentUserEmail)
				}
				.addOnFailureListener { exception ->
					Log.d("loadRoutineNames", "Error getting documents: ", exception)
				}
		}
	}

	private fun loadRoutineDataForAllRoutines(userEmail: String) {
		for (routineName in routineNamesList) {
			loadExerciseDataForRoutine(userEmail, routineName)
		}
	}

	private fun loadExerciseDataForRoutine(userEmail: String, routineName: String) {
		val db = FirebaseFirestore.getInstance()

		db.collection("Exercises")
			.document(userEmail)
			.collection(routineName) // 해당 루틴 이름의 컬렉션에 접근
			.get()
			.addOnSuccessListener { exerciseDocuments ->
				val exercisesList = mutableListOf<Exercises>()
				for (exerciseDocument in exerciseDocuments) {
					val exname = exerciseDocument.id
					val setnumber = exerciseDocument.getString("setnumber")
					val weight = exerciseDocument.getString("weight")
					val numofexercise = exerciseDocument.getString("numofexercise")
					val exerciseData = Exercises(exname, setnumber, weight, numofexercise)
					exercisesList.add(exerciseData)
				}

				// 어댑터에 데이터 설정
				val routineComponent = RoutineComponent(routineName, exercisesList)
				temporaryRoutineComponent.add(routineComponent)
				routineAdapter.notifyDataSetChanged()

			}
			.addOnFailureListener { exception ->
				Log.d("loadExerciseData", "Error getting documents: ", exception)
			}
	}

}