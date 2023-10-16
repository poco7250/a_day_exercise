package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.poco.a_day_exercise.databinding.ActivityFriendInformationBinding

class FriendInformationActivity : AppCompatActivity() {

	val binding by lazy { ActivityFriendInformationBinding.inflate(layoutInflater) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		val useremail = intent.getStringExtra("useremail")
		val username = intent.getStringExtra("username")
		Log.d("checkfriendemail", useremail!!)

		val friendrecordtext = getString(R.string.friendrecord, username)

		binding.friendrecord.text = friendrecordtext

		val calendarFragmentadapter = CalendarFragmentAdapter(this)
		binding.viewpagerfriend.adapter = calendarFragmentadapter

		TabLayoutMediator(binding.taplayoutfriend, binding.viewpagerfriend) { tab, position ->
			// 탭과 페이지를 연결
			tab.text = when (position) {
				0 -> "통계" // 첫 번째 탭 제목
				1 -> "기록" // 두 번째 탭 제목
				else -> ""
			}
		}.attach()

//		// FriendstatisticsFragment로 useremail 데이터 전달
//		val statisticsFragment = FriendStatisticsFragment()
//		val statisticsArgs = Bundle()
//		statisticsArgs.putString("useremail", useremail)
//		Log.d("checkfriendemail2", useremail)
//		statisticsFragment.arguments = statisticsArgs
//
//		// FriendRecordFragment로 useremail 데이터 전달
//		val recordFragment = FriendRecordFragment()
//		val recordArgs = Bundle()
//		recordArgs.putString("useremail", useremail)
//		Log.d("checkfriendemail3", useremail)
//		recordFragment.arguments = recordArgs
	}
}