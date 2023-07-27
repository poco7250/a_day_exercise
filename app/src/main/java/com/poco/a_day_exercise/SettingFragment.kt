package com.poco.a_day_exercise

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.FragmentSettingBinding


class SettingFragment : Fragment() {

	private lateinit var auth: FirebaseAuth
	private val binding by lazy { FragmentSettingBinding.inflate(layoutInflater) }

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		auth = Firebase.auth

		// 로그아웃 버튼
		binding.logoutbutton.setOnClickListener {
			// 로그인 화면으로
			logout()
		}

		return binding.root
	}

	private fun logout() {
		// FirebaseUI를 사용하여 구글 로그아웃 처리
		AuthUI.getInstance()
			.signOut(requireContext())
			.addOnCompleteListener {
				// 일반 로그아웃 처리
				auth.signOut()
				// 로그인 화면으로 이동
				val intent = Intent(requireContext(), LoginActivity::class.java)
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
				startActivity(intent)
				requireActivity().finish()
			}
	}

}