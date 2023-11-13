package com.poco.a_day_exercise

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.poco.a_day_exercise.databinding.ActivityModifyRoutineNameBinding

class ModifyRoutineNameActivity : ComponentActivity() {

	val binding by lazy { ActivityModifyRoutineNameBinding.inflate(layoutInflater) }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		val rtname = intent.getStringExtra("RoutineName")
		binding.currnetRoutineName.text = rtname

		binding.cancleButton1.setOnClickListener {
			finish()
		}

		binding.saveButton1.setOnClickListener {
			// rtname이란 이름을 가진 루틴의 이름을 변경 (RoutineList와 컬렉션 이름 변경)
			Toast.makeText(this, "추후 지원 예정입니다.", Toast.LENGTH_SHORT).show()
		}
	}
}