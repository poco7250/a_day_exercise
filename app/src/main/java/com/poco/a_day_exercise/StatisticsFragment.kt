package com.poco.a_day_exercise

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.poco.a_day_exercise.databinding.FragmentMainBinding
import com.poco.a_day_exercise.databinding.FragmentStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {
	private val binding by lazy { FragmentStatisticsBinding.inflate(layoutInflater)} // 뷰바인딩 설정

	// 현재 날짜 가져오기
	val currentDate = Date()

	// 원하는 날짜 포맷을 정의
	val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

	// 날짜를 문자열로 변환
	var selectedDate = dateFormat.format(currentDate)

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment

		binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
			// 날짜 선택 이벤트 처리
			val newSelectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
			selectedDate = newSelectedDate // 선택한 날짜로 업데이트
			Toast.makeText(requireContext(), "선택된 날짜 : $selectedDate", Toast.LENGTH_SHORT).show()
		}

		binding.recordConfirmationButton.setOnClickListener {
			// 선택한 날짜 데이터를 Intent에 추가
			val intent = Intent(requireContext(), RecordActivity::class.java)
			intent.putExtra("selectedDate", selectedDate)
			startActivity(intent)
		}
		return binding.root
	}
}