package com.poco.a_day_exercise

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
	private lateinit var auth: FirebaseAuth
	val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		// Initialize Firebase Auth
		auth = Firebase.auth

		val fragmentList = listOf(MainFragment(), StatisticsFragment(), SearchFragment(), AddFriendFragment(), SettingFragment())
		val adapter = FragmentAdapter(this)
		adapter.fragmentList = fragmentList
		binding.viewPager.adapter = adapter

		binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				super.onPageSelected(position)
				// BottomNavigationView의 선택 항목 변경
				binding.bnview.menu.getItem(position).isChecked = true
			}
		})
		// BottomNavigationView의 선택 이벤트 처리
		binding.bnview.setOnItemSelectedListener { menuItem ->
			when (menuItem.itemId) {
				R.id.home -> {
					binding.viewPager.currentItem = 0
					true
				}
				R.id.statistics -> {
					binding.viewPager.currentItem = 1
					true
				}
				R.id.search -> {
					binding.viewPager.currentItem = 2
					true
				}
				R.id.friend -> {
					binding.viewPager.currentItem = 3
					true
				}
				R.id.settings -> {
					binding.viewPager.currentItem = 4
					true
				}
				else -> false
			}
		}
		binding.bnview.selectedItemId = R.id.home
	}
}