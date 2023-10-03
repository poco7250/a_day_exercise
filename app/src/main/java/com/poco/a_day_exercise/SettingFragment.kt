package com.poco.a_day_exercise

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.poco.a_day_exercise.databinding.FragmentSettingBinding
import java.io.FileInputStream


class SettingFragment : Fragment() {

	private lateinit var auth: FirebaseAuth
	private val binding by lazy { FragmentSettingBinding.inflate(layoutInflater) }

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		auth = Firebase.auth

		val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

		if (currentUserEmail != null) {
			val db = FirebaseFirestore.getInstance()
			val usersCollection = db.collection("Users")

			usersCollection
				.whereEqualTo("email", currentUserEmail)
				.get()
				.addOnSuccessListener { querySnapshot ->
					if (!querySnapshot.isEmpty) {
						val userDocument = querySnapshot.documents[0]
						val name = userDocument.getString("name")
						val email = userDocument.getString("email")
						val imageUrl = userDocument.getString("userImageURL")

						// 데이터를 TextView와 ImageView에 연결
						binding.myNameText.text = name
						binding.myEmailText.text = email

						// 이미지 로딩 시작
						loadAndSetImage(imageUrl)
					}
				}
				.addOnFailureListener { e ->
					// 오류 처리
					Log.w("MyTag", "사용자 정보 불러오기 실패", e)
				}
		}

		// 이미지 변경 버튼
		binding.changePictureButton.setOnClickListener {
			showImageChangeDialog()
		}

		// 이름 변경 버튼
		binding.modifyNameButton.setOnClickListener {
			modifyName()
		}

		// 로그아웃 버튼
		binding.logoutbutton.setOnClickListener {
			// 로그인 화면으로
			logout()
		}

		// 회원 탈퇴 버튼
		binding.withdrawButton.setOnClickListener {
			val alertDialogBuilder = AlertDialog.Builder(requireContext())
			alertDialogBuilder.setMessage("삭제하시겠습니까?")
			alertDialogBuilder.setPositiveButton("예") { dialog, which ->
				membershipWithdrawl(currentUserEmail!!)
			}
			alertDialogBuilder.setNegativeButton("아니오") { dialog, which ->
				// 아무 작업도 하지 않고 다이얼로그 닫기
				dialog.dismiss()
			}
			val alertDialog = alertDialogBuilder.create()
			alertDialog.show()
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


	// 이미지 로딩을 시작하고 이미지뷰에 설정
	private fun loadAndSetImage(imageUrl: String?) {
		if (imageUrl != null) {
			Glide.with(this)
				.load(imageUrl)
				.into(binding.imageView)
		}
	}


	// onActivityResult에서 사진을 업로드하고 Firestore에 저장
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (resultCode == Activity.RESULT_OK) {
			when (requestCode) {
				REQUEST_GALLERY -> {
					val selectedImageUri = data?.data
					if (selectedImageUri != null) {
						uploadImageAndSaveToFirestore(selectedImageUri)
					}
				}
			}
		}
	}

	private fun changeToDefaultProfilePicture() {
		val storage = FirebaseStorage.getInstance()
		val storageRef = storage.reference
		val currentUser = FirebaseAuth.getInstance().currentUser
		val currentUserEmail = currentUser?.email

		// 이메일을 사용하여 사용자 문서 참조를 가져오기
		val db = FirebaseFirestore.getInstance()
		val usersCollection = db.collection("Users")

		if (currentUserEmail != null) {
			usersCollection
				.whereEqualTo("email", currentUserEmail)
				.get()
				.addOnSuccessListener { querySnapshot ->
					if (!querySnapshot.isEmpty) {
						val userDocument = querySnapshot.documents[0]

						// "user.png" 이미지에 대한 참조를 만듭니다.
						val imageRef = storageRef.child("user.png")

						imageRef
							.downloadUrl
							.addOnSuccessListener { uri ->
								val imageUrl = uri.toString()
								Log.d("checkurl", imageUrl)

								// 사용자의 프로필 이미지 URL을 기본 이미지 URL로 업데이트
								userDocument.reference
									.update("userImageURL", imageUrl)
									.addOnSuccessListener {
										// 업데이트 성공
										Log.d("MyTag", "프로필 이미지 업데이트 성공")
										loadAndSetImage(imageUrl) // 이미지 뷰 업데이트
									}
									.addOnFailureListener { e ->
										// 업데이트 실패
										Log.e("MyTag", "프로필 이미지 업데이트 실패", e)
									}
							}
							.addOnFailureListener { e ->
								// 이미지 다운로드 URL 가져오기 실패
								Log.w("MyTag", "이미지 불러오기 실패", e)
							}
					}
				}
				.addOnFailureListener { e ->
					// 오류 처리
					Log.w("MyTag", "사용자 정보 불러오기 실패", e)
				}
		}
	}


	// 갤러리에서 사진 선택하기
	private fun choosePictureFromGallery() {
		val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
		startActivityForResult(intent, REQUEST_GALLERY)
	}

	// 갤러리에서 선택한 사진을 Firebase Storage에 업로드하고 Firestore에 저장
	private fun uploadImageAndSaveToFirestore(imageUri: Uri) {
		val storage = FirebaseStorage.getInstance()
		val storageRef = storage.reference
		val currentUser = FirebaseAuth.getInstance().currentUser
		val currentUserEmail = currentUser?.email

		if (currentUserEmail != null) {
			val usersCollection = FirebaseFirestore.getInstance().collection("Users")

			usersCollection
				.whereEqualTo("email", currentUserEmail) // 현재 로그인된 사용자의 이메일과 일치하는 문서를 찾음
				.get()
				.addOnSuccessListener { querySnapshot ->
					if (!querySnapshot.isEmpty) {
						val userDocument = querySnapshot.documents[0]
						val userDocRef = usersCollection.document(userDocument.id)
						val userid = userDocument.id

						// 이미지 업로드
						val imageRef = storageRef.child("${userid}.jpg")
						imageRef.putFile(imageUri)
							.addOnSuccessListener {
								// 업로드 성공 시 이미지 URL을 가져옴
								imageRef.downloadUrl.addOnSuccessListener { uri ->
									val imageUrl = uri.toString()

									// Firestore에 이미지 URL 업데이트
									userDocRef.update("userImageURL", imageUrl)
										.addOnSuccessListener {
											// 이미지 URL 업데이트 성공 시 이미지 로딩
											loadAndSetImage(imageUrl)
										}
										.addOnFailureListener { e ->
											Log.e(TAG, "Firestore에 이미지 URL 업데이트 실패: $e")
										}
								}
							}
							.addOnFailureListener { e ->
								Log.e(TAG, "이미지 업로드 실패: $e")
							}
					}
				}
				.addOnFailureListener { e ->
					Log.e(TAG, "사용자 정보 불러오기 실패: $e")
				}
		}
	}

	companion object {
		private const val TAG = "SettingFragment"
		private const val REQUEST_GALLERY = 123
	}

	// 다이얼로그 표시 함수
	private fun showImageChangeDialog() {
		val options = arrayOf("기본 이미지로 설정하기", "갤러리에서 선택하기")

		AlertDialog.Builder(requireContext())
			.setTitle("프로필 사진 변경")
			.setItems(options) { _, which ->
				when (which) {
					0 -> changeToDefaultProfilePicture()
					1 -> choosePictureFromGallery()
				}
			}
			.show()
	}

	private fun modifyName() {
		// 다이얼로그 빌더 생성
		val builder = AlertDialog.Builder(requireContext())
		builder.setTitle("새로운 이름 입력")

		// 다이얼로그에 EditText 추가
		val input = EditText(requireContext())
		builder.setView(input)

		// 다이얼로그의 "확인" 버튼 설정
		builder.setPositiveButton("확인") { dialog, _ ->
			val newUserName = input.text.toString().trim() // 입력된 이름 가져오기

			if (newUserName.isNotEmpty()) {
				// 현재 로그인한 사용자의 이메일 가져오기
				val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

				if (currentUserEmail != null) {
					// Firestore 참조 가져오기
					val db = FirebaseFirestore.getInstance()
					val usersCollection = db.collection("Users")

					// 이메일을 사용하여 사용자 문서 찾기
					usersCollection
						.whereEqualTo("email", currentUserEmail)
						.get()
						.addOnSuccessListener { querySnapshot ->
							if (!querySnapshot.isEmpty) {
								val userDocument = querySnapshot.documents[0]

								// 사용자 문서의 name 필드 업데이트
								userDocument.reference
									.update("name", newUserName)
									.addOnSuccessListener {
										// 업데이트 성공
										binding.myNameText.text = newUserName
										Log.d("MyTag", "사용자 이름 업데이트 성공")
									}
									.addOnFailureListener { e ->
										// 업데이트 실패
										Log.e("MyTag", "사용자 이름 업데이트 실패", e)
									}
							}
						}
						.addOnFailureListener { e ->
							// 오류 처리
							Log.w("MyTag", "사용자 정보 불러오기 실패", e)
						}
				}

				// 다이얼로그 닫기
				dialog.dismiss()
			} else {
				// 이름이 비어있는 경우 경고 메시지 표시
				Toast.makeText(requireContext(), "이름을 입력하세요.", Toast.LENGTH_SHORT).show()
			}
		}

		// 다이얼로그의 "취소" 버튼 설정
		builder.setNegativeButton("취소") { dialog, _ ->
			// 다이얼로그 닫기
			dialog.dismiss()
		}

		// 다이얼로그 표시
		builder.show()
	}

	private fun membershipWithdrawl(email: String)
	{
//		// Firebase Authentication에서 사용자 로그아웃
//		val auth = FirebaseAuth.getInstance()
//		auth.signOut()
//
//		// Firebase Authentication에서 사용자 삭제
//		val user = auth.currentUser
//		user?.delete()
//			?.addOnCompleteListener { task ->
//				if (task.isSuccessful) {
//					// 사용자 삭제 성공
//					Log.d("MyTag", "Firebase Authentication 사용자 삭제 성공")
//
//					val db = FirebaseFirestore.getInstance()
//					val usersCollection = db.collection("Users")
//					val friendsCollection = db.collection("Friends")
//					val exerciseRef = db.collection("Exercises").document(email)
//
//					// 이메일을 사용하여 문서를 찾고 삭제
//					usersCollection
//						.whereEqualTo("email", email)
//						.get()
//						.addOnSuccessListener { querySnapshot ->
//							for (document in querySnapshot.documents) {
//								document.reference.delete()
//							}
//						}
//						.addOnFailureListener { e ->
//							// 삭제 중에 오류가 발생한 경우 처리
//							Log.e("MyTag", "사용자 문서 삭제 실패: $e")
//						}
//
//					val friendsDocument = friendsCollection.document(email)
//					friendsDocument
//						.delete()
//						.addOnSuccessListener {
//							// 삭제 성공 시 처리
//							Log.d("MyTag", "사용자 문서 삭제 성공")
//						}
//						.addOnFailureListener { e ->
//							// 삭제 중에 오류가 발생한 경우 처리
//							Log.e("MyTag", "사용자 문서 삭제 실패: $e")
//						}
//
//					val myFriendsRef = db.collection("Friends").document(email).collection("MyFriends")
//
//					myFriendsRef
//						.whereEqualTo("email", email)
//						.get()
//						.addOnSuccessListener { querySnapshot ->
//							for (document in querySnapshot) {
//								// 이메일이 일치하는 문서를 삭제합니다.
//								document.reference.delete()
//									.addOnSuccessListener {
//										Log.d("MyTag", "MyFriends 문서 삭제 성공")
//									}
//									.addOnFailureListener { e ->
//										Log.e("MyTag", "MyFriends 문서 삭제 실패: $e")
//									}
//							}
//						}
//						.addOnFailureListener { e ->
//							Log.e("MyTag", "MyFriends 컬렉션 조회 실패: $e")
//						}
//
//					exerciseRef
//						.delete()
//						.addOnSuccessListener {
//							Log.d("MyTag", "Exercise 문서 삭제 성공")
//						}
//						.addOnFailureListener { e ->
//							Log.e("MyTag", "Exercise 컬렉션 조회 실패: $e")
//						}
//					Toast.makeText(requireContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
//				} else {
//					// 사용자 삭제 실패
//					Log.e("MyTag", "Firebase Authentication 사용자 삭제 실패: ${task.exception}")
//				}

	}
}
