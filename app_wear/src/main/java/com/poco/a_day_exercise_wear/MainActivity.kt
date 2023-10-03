package com.poco.a_day_exercise_wear

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.poco.a_day_exercise_wear.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

	private var isTimerRunning: Boolean = false
	private var baseTime: Long = 0L // 타이머가 시작된 기준 시간을 저장할 변수
	private var elapsedTime: Long = 0L // 경과 시간을 저장할 변수
	private val handler = Handler(Looper.getMainLooper()) // Main 스레드에서 실행되도록 설정

	val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		// 시작/일시정지 버튼 클릭 이벤트 핸들러를 설정합니다.
		binding.startButton.setOnClickListener {
			if (!isTimerRunning) {
				// 타이머가 실행 중이 아닌 경우
				// 타이머를 시작하고 아이콘을 일시정지로 변경합니다.
				startTimer()
				binding.startButton.setImageResource(android.R.drawable.ic_media_pause)
			} else {
				// 타이머가 실행 중인 경우
				// 타이머를 일시정지하고 아이콘을 시작으로 변경합니다.
				pauseTimer()
				binding.startButton.setImageResource(android.R.drawable.ic_media_play)
			}
		}

		// 초기화 버튼 클릭 이벤트 핸들러를 설정합니다.
		binding.resetButton.setOnClickListener {
			// 타이머를 초기화하고 아이콘을 시작으로 변경합니다.
			resetTimer()
			binding.startButton.setImageResource(android.R.drawable.ic_media_play)
		}
	}

	private fun startTimer() {
		if (!isTimerRunning) {
			val currentTime = SystemClock.elapsedRealtime()
			if (baseTime == 0L) {
				// 타이머가 처음 시작되는 경우 또는 초기화된 경우
				baseTime = currentTime
				binding.timer.base = baseTime // baseTime을 설정할 때 타이머 시간을 초기화하지 않습니다.
			} else {
				// 타이머가 일시정지된 후 재개되는 경우
				baseTime += currentTime - elapsedTime
			}
			binding.timer.start()
			isTimerRunning = true

			handler.postDelayed(updateTimer, 10)
		}
	}

	private fun pauseTimer() {
		if (isTimerRunning) {
			// 타이머가 실행 중인 경우에만 일시정지
			binding.timer.stop()
			elapsedTime = SystemClock.elapsedRealtime()
			isTimerRunning = false
			handler.removeCallbacks(updateTimer)
		}
	}

	private fun resetTimer() {
		binding.timer.base = SystemClock.elapsedRealtime()
		binding.timer.stop()
		elapsedTime = 0L
		isTimerRunning = false
		handler.removeCallbacks(updateTimer)
	}

	private val updateTimer = object : Runnable {
		override fun run() {
			if (isTimerRunning) {
				val timeElapsed = SystemClock.elapsedRealtime() - baseTime
				val hours = (timeElapsed / 3600000).toInt()
				val minutes = ((timeElapsed - hours * 3600000) / 60000).toInt()
				val seconds = ((timeElapsed - hours * 3600000 - minutes * 60000) / 1000).toInt()
				val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
				binding.timer.text = formattedTime
				handler.postDelayed(this, 10)
			}
		}
	}
}