package com.poco.a_day_exercise

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

	var fragmentList = listOf<Fragment>()

	override fun getItemCount(): Int {
		return fragmentList.size
	}

	override fun createFragment(position: Int): Fragment {
		return when (position) {
			0 -> StatisticsFragment()
			1 -> SearchFragment()
			2 -> MainFragment()
			3 -> AddFriendFragment()
			else -> SettingFragment()
		}
	}


}