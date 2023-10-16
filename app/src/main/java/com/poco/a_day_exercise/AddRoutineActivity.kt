package com.poco.a_day_exercise

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.AddRoutineBinding
import com.poco.a_day_exercise.databinding.ItemExerciseBinding
import com.poco.a_day_exercise.databinding.ItemRoutineBinding
import java.nio.file.Files.exists

data class Routine (
	val rtname: String? = null
)

class AddRoutineActivity : AppCompatActivity() {
	// RoutineAdapter 인스턴스를 멤버 변수로 선언
	private lateinit var routineAdapter: AddRoutineActivity.RoutineAdapter

	private val binding by lazy { AddRoutineBinding.inflate(layoutInflater)}

	// 임시 아이템을 저장할 리스트 (리사이클러뷰를 위한 리스트)
	private val temporaryRoutines = mutableListOf<Routine>()
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
		routineAdapter = RoutineAdapter(temporaryRoutines)
		// 리니어 레이아웃 매니저 설정
		val layoutManager = LinearLayoutManager(this)
		binding.routineRecyclerView.layoutManager = layoutManager
		binding.routineRecyclerView.adapter = routineAdapter

		loadRoutineNames()

		// 리사이클러뷰 아이템 클릭 리스너 추가
		routineAdapter.setOnItemClickListener { selectedRoutine ->
			val intent = Intent(this@AddRoutineActivity, AddExerciseActivity::class.java)
			intent.putExtra("selectedRoutine", selectedRoutine.rtname)
			startActivity(intent)
		}

		binding.addExerciseButton.setOnClickListener {
			val userroutinename = binding.insertRoutineName.text.toString()

			if (userroutinename.isNotEmpty())
			{
				Log.d("check", "루틴 이름 등록 완료")
				val intent = Intent(this@AddRoutineActivity, AddExerciseActivity::class.java)
				intent.putExtra("userroutinename", userroutinename)
				startActivity(intent)
			} else
			{
				Log.d("check", "루틴 이름 등록 실패")
				makeText(this@AddRoutineActivity, "루틴 이름이 정해지지 않았습니다.", LENGTH_SHORT).show()
			}
		}

		binding.modifyRoutine.setOnClickListener {
			if (selectedItemPosition != -1) {
				// 선택된 루틴의 위치를 가져와서 사용
				val selectedRoutineName = temporaryRoutines[selectedItemPosition].rtname
				// 다음 액티비티로 전달
				val intent = Intent(this@AddRoutineActivity, AddExerciseActivity::class.java)
				intent.putExtra("selectedRoutine", selectedRoutineName)
				startActivity(intent)
			} else {
				// 선택된 루틴이 없을 때 처리
				Toast.makeText(this, "루틴을 선택해주세요.", Toast.LENGTH_SHORT).show()
			}
		}

		binding.successButton.setOnClickListener {
			finish()
		}

		binding.root.setOnClickListener {
			selectedItemPosition = -1 // 선택 해제
			routineAdapter.notifyDataSetChanged()
		}
	}

	// 리사이클러 뷰
	inner class RoutineAdapter(private val routines: List<Routine>) :
		RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
			val binding =
				ItemRoutineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			return RoutineViewHolder(binding)
		}

		override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
			val currentRoutine = routines[position]
			holder.rtname.text = currentRoutine.rtname

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

			holder.deletertbutton.setOnClickListener {
				val alertDialogBuilder = AlertDialog.Builder(this@AddRoutineActivity)
				alertDialogBuilder.setMessage("삭제하시겠습니까?")
				alertDialogBuilder.setPositiveButton("예") { dialog, which ->
					deleteRoutine(position, currentRoutine)
				}
				alertDialogBuilder.setNegativeButton("아니오") { dialog, which ->
					// 아무 작업도 하지 않고 다이얼로그 닫기
					dialog.dismiss()
				}
				val alertDialog = alertDialogBuilder.create()
				alertDialog.show()
			}
		}

		private var onItemClickListener: ((Routine) -> Unit)? = null

		fun setOnItemClickListener(listener: (Routine) -> Unit) {
			onItemClickListener = listener
		}
		override fun getItemCount() = routines.size

		inner class RoutineViewHolder(private val binding: ItemRoutineBinding) :
			RecyclerView.ViewHolder(binding.root){
			val rtname: TextView = binding.rtName
			val deletertbutton: Button = binding.deleteRoutine
		}

		private fun deleteItem(position: Int) {
			if (position >= 0 && position < routines.size) {
				temporaryRoutines.removeAt(position)
				notifyItemRemoved(position)
			}
		}

		private fun deleteRoutine(position: Int, routine: Routine) {
			val db = FirebaseFirestore.getInstance()
			val currentUserEmail = Firebase.auth.currentUser?.email

			if (currentUserEmail != null) {
				val selectedRoutineName = routine.rtname

				db.collection("Exercises")
					.document(currentUserEmail)
					.collection("RoutineList")
					.document(selectedRoutineName!!)
					.delete()
					.addOnSuccessListener {
						// 모든 문서가 삭제된 후에 컬렉션도 삭제
						db.collection("Exercises")
							.document(currentUserEmail)
							.collection(selectedRoutineName)
							.get()
							.addOnSuccessListener { documents ->
								for (document in documents) {
									// 각 문서를 삭제
									document.reference.delete()
								}
								// 컬렉션 삭제 시도
								db.collection("Exercises")
									.document(currentUserEmail)
									.collection(selectedRoutineName)
									.parent?.delete()
									?.addOnSuccessListener {
										// 선택한 아이템 삭제
										deleteItem(position)
										// 컬렉션 삭제 성공
										Toast.makeText(this@AddRoutineActivity, "데이터가 성공적으로 삭제되었습니다.", Toast.LENGTH_SHORT).show()
									}
									?.addOnFailureListener { exception ->
										// 컬렉션 삭제 실패
										Log.d("DeleteCollection", "Error deleting collection", exception)
										Toast.makeText(this@AddRoutineActivity, "컬렉션 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
									}
							}
							.addOnFailureListener { exception ->
								// 문서 조회 실패
								Log.d("DeleteDocument", "Error fetching documents", exception)
								Toast.makeText(this@AddRoutineActivity, "문서 조회에 실패했습니다.", Toast.LENGTH_SHORT).show()
							}
					}
					.addOnFailureListener { exception ->
						// 데이터 삭제 실패한 경우 처리
						Log.d("DeleteRoutine", "Error deleting document", exception)
						Toast.makeText(this@AddRoutineActivity, "데이터 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
					}
			}
		}

	}

	private fun loadRoutineNames() {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email

		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection("RoutineList") // 루틴이 저장된 컬렉션
			.get()
			.addOnSuccessListener { documents ->
				temporaryRoutines.clear() // 기존의 값을 모두 지움
				for (document in documents) {
					val routineName = document.id
					val routine = Routine(routineName)
					temporaryRoutines.add(routine)
				}
				routineAdapter.notifyDataSetChanged() // 데이터 불러온 후 업데이트
			}
			.addOnFailureListener { exception ->
				Log.d("loadRoutineNames", "Error getting routines: ", exception)
			}
	}

	override fun onResume() {
		super.onResume()
		loadRoutineNames() // 데이터를 다시 불러와서 갱신
	}

	private val onBackPressedCallback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			// 뒤로가기 시 실행할 코드
			Toast.makeText(this@AddRoutineActivity, "완료 혹은 취소버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
		}
	}
}