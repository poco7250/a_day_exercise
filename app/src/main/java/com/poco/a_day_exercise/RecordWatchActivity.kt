package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.poco.a_day_exercise.databinding.ActivityRecordWatchBinding

class RecordWatchActivity : AppCompatActivity() {

	private val binding by lazy { ActivityRecordWatchBinding.inflate(layoutInflater) }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
	}
}