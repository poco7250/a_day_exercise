package com.poco.a_day_exercise

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
			Toast.makeText(requireContext(), "선택된 날짜 : $selectedDate", Toast.LENGTH_SHORT).show()
		}

		return binding.root
	}
}