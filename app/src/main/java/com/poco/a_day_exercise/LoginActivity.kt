package com.poco.a_day_exercise

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.poco.a_day_exercise.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

	private lateinit var auth: FirebaseAuth
	private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
	private var googleSignInClient : GoogleSignInClient? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		// Firebase Auth 시작
		auth = Firebase.auth

		val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.default_web_client_id))
			.requestEmail()
			.build()
		googleSignInClient = GoogleSignIn.getClient(this, gso)

		// 회원가입 버튼
		binding.signup.setOnClickListener {
			startActivity(Intent(this,signupActivity::class.java))
		}

		// 로그인 버튼
		binding.loginButton.setOnClickListener {
			signIn(binding.emailText.text.toString(),binding.PasswordText.text.toString())
		}

		// 구글 로그인 버튼
		binding.googleLoginButton.setOnClickListener {
			googleLogin()
		}
	}

	// 로그아웃하지 않을 시 자동 로그인 , 회원가입시 바로 로그인 됨
	public override fun onStart() {
		super.onStart()
		moveMainPage(auth?.currentUser)
	}

	private fun signIn(email: String, password: String) {
		if (email.isNotEmpty() && password.isNotEmpty()) {
			auth?.signInWithEmailAndPassword(email, password)
				?.addOnCompleteListener(this) { task ->
					if (task.isSuccessful) {
						Toast.makeText(
							baseContext, "로그인에 성공 하였습니다.",
							Toast.LENGTH_SHORT
						).show()
						moveMainPage(auth?.currentUser)
					} else {
						Toast.makeText(
							baseContext, "로그인에 실패 하였습니다.",
							Toast.LENGTH_SHORT
						).show()
					}
				}
		}
	}

	// 유저정보 넘겨주고 메인 액티비티 호출
	private fun moveMainPage(user: FirebaseUser?){
		if( user!= null){
			startActivity(Intent(this,MainActivity::class.java))
			finish()
		}
	}

	private fun googleLogin() {
		val signInIntent = googleSignInClient?.signInIntent
		if (signInIntent != null) {
			googleSignInLauncher.launch(signInIntent)
		} else {
			Log.w(TAG, "Google Sign-In Intent is null.")
			Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
		}
	}

	private val googleSignInLauncher =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val data: Intent? = result.data
				if (data != null) {
					val task = GoogleSignIn.getSignedInAccountFromIntent(data)
					handleGoogleSignInResult(task)
				}
			} else {
				// 구글 로그인 실패
				Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
			}
		}

	private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
		try {
			val account = task.getResult(ApiException::class.java)
			Log.d("check", "$account")
			Log.d("check", "성공")
			firebaseAuthWithGoogle(account)
		} catch (e: ApiException) {
			// 구글 로그인 실패
			Log.w(TAG, "구글 로그인 실패", e)
			// 실패 처리 수행
			Toast.makeText(this,"로그인 실패",Toast.LENGTH_SHORT).show()
		}
	}
	private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
		val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
		auth.signInWithCredential(credential)
			.addOnCompleteListener(this) { task ->
				if (task.isSuccessful) {
					val user = task.result?.user
					val uid = user?.uid
					val email = account?.email
					val name = account?.displayName

					val storage = FirebaseStorage.getInstance()
					val storageRef = storage.reference
					val imageRef = storageRef.child("user.png")

					imageRef.downloadUrl.addOnSuccessListener { uri ->
						val imageUrl = uri.toString()

						// Firestore에서 사용자 정보 가져오기
						val db = FirebaseFirestore.getInstance()
						val userRef = db.collection("Users").document(uid!!)

						userRef.get().addOnSuccessListener { document ->
							if (document.exists()) {
								// 이미 사용자 정보가 Firestore에 있는 경우
								// 여기서는 아무 작업도 수행하지 않고 메인 페이지로 이동
								moveMainPage(user)
							} else {
								// Firestore에 사용자 정보가 없는 경우 (새로운 사용자)
								val userMap = hashMapOf(
									"email" to email,
									"name" to name,
									"userImageURL" to imageUrl
								)

								// 사용자 정보 Firestore에 추가
								userRef.set(userMap).addOnSuccessListener {
									// 정보 추가 성공 후 메인 페이지로 이동
									moveMainPage(user)
								}
							}
						}
					}.addOnFailureListener { e ->
						// 이미지 다운로드 URL 가져오기 실패
						Log.w("MyTag", "이미지 불러오기 실패", e)
					}
				}
			}
	}
}