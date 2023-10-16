package com.poco.a_day_exercise

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.ActivitySelectMenuBinding

class SelectMenuActivity : AppCompatActivity() {

	private lateinit var auth: FirebaseAuth
	val binding by lazy { ActivitySelectMenuBinding.inflate(layoutInflater) }
	private lateinit var splashScreen: SplashScreen
	@RequiresApi(Build.VERSION_CODES.S)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		splashScreen = this.installSplashScreen()
		startSplash()
		setContentView(binding.root)

		auth = Firebase.auth

		binding.LButton.setOnClickListener {
			val intent = Intent(this, LoginActivity::class.java)
			startActivity(intent)
			finish()
		}

		binding.SButton.setOnClickListener {
			val intent = Intent(this, SignupActivity::class.java)
			startActivity(intent)
		}
	}

	// 로그아웃하지 않을 시 자동 로그인 , 회원가입시 바로 로그인 됨
	public override fun onStart() {
		super.onStart()
		moveMainPage(auth?.currentUser)
	}

	// 유저정보 넘겨주고 메인 액티비티 호출
	private fun moveMainPage(user: FirebaseUser?){
		if( user!= null){
			startActivity(Intent(this,MainActivity::class.java))
			finish()
		}
	}

	@RequiresApi(Build.VERSION_CODES.S)
	private fun startSplash() {
		val splashScreenWidth = resources.getDimensionPixelSize(R.dimen.splash_screen_width) // 고정된 값으로 설정

		splashScreen.setOnExitAnimationListener { splashScreenView ->
			val translationX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, -splashScreenWidth.toFloat(), splashScreenWidth.toFloat()) // 왼쪽에서 오른쪽으로 이동하도록 설정

			val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)

			ObjectAnimator.ofPropertyValuesHolder(splashScreenView.iconView, translationX, alpha).run {
				duration = 1000L
				interpolator = AccelerateInterpolator()
				doOnEnd {
					splashScreenView.remove()
				}
				start()
			}
		}
	}
}