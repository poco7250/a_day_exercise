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
			0 -> MainFragment()
			1 -> StatisticsFragment()
			2 -> SearchFragment()
			3 -> AddFriendFragment()
			else -> SettingFragment()
		}
	}


}