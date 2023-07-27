package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.ActivitySignupBinding

class signupActivity : AppCompatActivity() {
	private lateinit var auth: FirebaseAuth
	val binding by lazy { ActivitySignupBinding.inflate((layoutInflater)) } // 뷰바인딩 설정

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)

		auth = Firebase.auth

		// 계정 생성 버튼
		binding.signupbutton.setOnClickListener {
			createAccount(binding.emailtext.text.toString(),binding.passwordtext.text.toString(), binding.nameText.text.toString())
		}
	}

	// 계정 생성
	private fun createAccount(email: String, password: String, name: String) {

		if (email.isNotEmpty() && password.isNotEmpty()) {
			auth?.createUserWithEmailAndPassword(email, password)
				?.addOnCompleteListener(this) { task ->
					if (task.isSuccessful) {
						val user = task.result?.user
						user?.let {
							// 사용자 계정이 성공적으로 생성되었으므로, 파이어스토어에 사용자 정보를 저장합니다.
							saveUserInfoToFirestore(it.uid, name, email)
							Toast.makeText(this, "계정 생성 완료.", Toast.LENGTH_SHORT).show()
							finish() // 가입창 종료
						}
					} else {
						Toast.makeText(
							this, "계정 생성 실패",
							Toast.LENGTH_SHORT
						).show()
					}
				}
		}
	}

	private fun saveUserInfoToFirestore(uid: String, name: String, email: String) {
		// 파이어스토어 데이터베이스를 참조합니다.
		val db = FirebaseFirestore.getInstance()

		// 사용자 정보를 저장할 문서의 경로를 지정합니다. (예: "users" 컬렉션 아래에 UID로 된 문서)
		val userDocRef = db.collection("Users").document(uid)

		// 사용자 정보를 HashMap으로 만듭니다.
		val user = hashMapOf(
			"name" to name,
			"email" to email
			// 추가적인 사용자 정보가 있다면 여기에 추가할 수 있습니다.
		)

		// 사용자 정보를 파이어스토어에 저장합니다.
		userDocRef.set(user)
			.addOnSuccessListener {
				Log.d("MyTag", "사용자 정보 저장 성공")
			}
			.addOnFailureListener { e ->
				Log.w("MyTag", "사용자 정보 저장 실패", e)
			}
	}
}