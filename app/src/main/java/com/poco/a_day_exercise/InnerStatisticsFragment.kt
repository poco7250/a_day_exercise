package com.poco.a_day_exercise

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.FragmentInnerStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InnerStatisticsFragment : Fragment() {

	val binding by lazy { FragmentInnerStatisticsBinding.inflate(layoutInflater) }

	// 현재 날짜 가져오기
	val currentDate = Date()

	// 원하는 날짜 포맷을 정의
	val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

	// 날짜를 문자열로 변환
	var selectedDate = dateFormat.format(currentDate)

	private lateinit var sharedViewModel: SharedViewModel
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment

		sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

		binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
			// 날짜 선택 이벤트 처리
			val newSelectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
			selectedDate = newSelectedDate // 선택한 날짜로 업데이트
			sharedViewModel.setSelectedDate(newSelectedDate)
			loadSelectedDateExerciseTime(selectedDate)
			loadSelectedMonthExerciseTime(selectedDate)
			loadSelectedYearExerciseTime(selectedDate)
			Toast.makeText(requireContext(), "선택된 날짜 : $selectedDate", Toast.LENGTH_SHORT).show()
		}

		loadSelectedDateExerciseTime(selectedDate)
		loadSelectedMonthExerciseTime(selectedDate)
		loadSelectedYearExerciseTime(selectedDate)

		return binding.root
	}

	fun formatElapsedTimeKorean(seconds: Long): String {
		val days = seconds / 86400
		val hours = (seconds % 86400) / 3600
		val minutes = (seconds % 3600) / 60
		val remainingSeconds = seconds % 60

		val formattedTime = StringBuilder()

		if (days > 0) {
			formattedTime.append("${days}일 ")
		}

		if (hours > 0) {
			formattedTime.append("${hours}시간 ")
		}

		if (minutes > 0) {
			formattedTime.append("${minutes}분 ")
		}

		formattedTime.append("${remainingSeconds}초")

		return formattedTime.toString()
	}



	fun loadSelectedDateExerciseTime(selectedDate: String) {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email
		val (year, month, day) = selectedDate.split("-")
		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection("time")
			.whereEqualTo("year", year)
			.whereEqualTo("month", month)
			.whereEqualTo("day", day)
			.get()
			.addOnSuccessListener { querySnapshot ->
				var totalseconds: Long = 0
				for (document in querySnapshot) {
					totalseconds += document.getLong("totalseconds") ?: 0
				}
				val formattedTime = formatElapsedTimeKorean(totalseconds)
				binding.todayExerciseTime.text = formattedTime
			}
			.addOnFailureListener { exception ->
				// 데이터를 가져오는 중에 오류가 발생한 경우
				Log.d("dateloaderror", "시간을 불러오는데 오류가 발생했습니다.")
			}
	}

	fun loadSelectedMonthExerciseTime(selectedDate: String) {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email
		val (year, month, day) = selectedDate.split("-")
		Log.d("datecheck", "${year}, ${month}, ${day}")
		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection("time")
			.whereEqualTo("year", year)
			.whereEqualTo("month", month)
			.get()
			.addOnSuccessListener { querySnapshot ->
				var totalseconds: Long = 0
				for (document in querySnapshot) {
					totalseconds += document.getLong("totalseconds") ?: 0
				}
				val formattedTime = formatElapsedTimeKorean(totalseconds)
				binding.monthExerciseTime.text = formattedTime
			}
			.addOnFailureListener { exception ->
				// 데이터를 가져오는 중에 오류가 발생한 경우
				Log.d("dateloaderror", "시간을 불러오는데 오류가 발생했습니다.")
			}
	}
	fun loadSelectedYearExerciseTime(selectedDate: String) {
		val db = FirebaseFirestore.getInstance()
		val currentUserEmail = Firebase.auth.currentUser?.email
		val (year, month, day) = selectedDate.split("-")
		db.collection("Exercises")
			.document(currentUserEmail!!)
			.collection("time")
			.whereEqualTo("year", year)
			.get()
			.addOnSuccessListener { querySnapshot ->
				var totalseconds: Long = 0
				for (document in querySnapshot) {
					totalseconds += document.getLong("totalseconds") ?: 0
				}
				val formattedTime = formatElapsedTimeKorean(totalseconds)
				binding.yearExerciseTime.text = formattedTime
			}
			.addOnFailureListener { exception ->
				// 데이터를 가져오는 중에 오류가 발생한 경우
				Log.d("dateloaderror", "시간을 불러오는데 오류가 발생했습니다.")
			}
	}
}