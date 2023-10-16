package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class InnerFragmentAdapter(fragment: Fragment) :
	FragmentStateAdapter(fragment) {

	private val fragmentList = listOf(
		InnerStatisticsFragment(),
		InnerRecordFragment()
	)

	override fun getItemCount(): Int = fragmentList.size

	override fun createFragment(position: Int): Fragment {
		return when (position) {
			0 -> InnerStatisticsFragment()
			1 -> InnerRecordFragment()
			else -> throw IllegalArgumentException("Invalid position: $position")
		}
	}
}