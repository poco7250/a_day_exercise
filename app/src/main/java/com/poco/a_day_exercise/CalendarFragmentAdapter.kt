package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CalendarFragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

	private val fragmentList = listOf(
		FriendStatisticsFragment(),
		FriendRecordFragment()
	)

	override fun getItemCount(): Int = fragmentList.size

	override fun createFragment(position: Int): Fragment {
		return when (position) {
			0 -> FriendStatisticsFragment()
			1 -> FriendRecordFragment()
			else -> throw IllegalArgumentException("Invalid position: $position")
		}
	}
}