package com.poco.a_day_exercise

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.tabs.TabLayoutMediator
import com.poco.a_day_exercise.databinding.FragmentMainBinding
import com.poco.a_day_exercise.databinding.FragmentStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsFragment : Fragment() {
	private val binding by lazy { FragmentStatisticsBinding.inflate(layoutInflater)} // 뷰바인딩 설정

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment

		val innerFragmentAdapter = InnerFragmentAdapter(this)
		binding.viewPagerstatistics.adapter = innerFragmentAdapter

		TabLayoutMediator(binding.tabLayoutstatistics, binding.viewPagerstatistics) { tab, position ->
			// 탭과 페이지를 연결
			tab.text = when (position) {
				0 -> "통계" // 첫 번째 탭 제목
				1 -> "기록" // 두 번째 탭 제목
				else -> ""
			}
		}.attach()

		return binding.root
	}
}