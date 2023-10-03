package com.poco.a_day_exercise

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.poco.a_day_exercise.databinding.ActivityDateBinding
import com.poco.a_day_exercise.databinding.FragmentStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateActivity : AppCompatActivity() {
	private val binding by lazy { ActivityDateBinding.inflate(layoutInflater) } // 뷰바인딩 설정

	// 현재 날짜 가져오기
	val currentDate = Date()

	// 원하는 날짜 포맷을 정의
	val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

	// 날짜를 문자열로 변환
	var selectedDate = dateFormat.format(currentDate)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		binding.calendarView2.setOnDateChangeListener { view, year, month, dayOfMonth ->
			// 날짜 선택 이벤트 처리
			val newSelectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
			selectedDate = newSelectedDate // 선택한 날짜로 업데이트
			Toast.makeText(this, "선택된 날짜 : $selectedDate", Toast.LENGTH_SHORT).show()
		}

		binding.registerExercise.setOnClickListener {
			// 선택한 날짜 데이터를 Intent에 추가
			val intent = Intent(this, RecordTextActivity::class.java)
			intent.putExtra("selectedDate", selectedDate)
			startActivity(intent)
		}

		binding.dateCancleButton.setOnClickListener {
			finish()
		}
	}
}