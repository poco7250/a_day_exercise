package com.poco.a_day_exercise

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.AddFriendDialogBinding
import com.poco.a_day_exercise.databinding.FragmentAddFriendBinding
import com.poco.a_day_exercise.databinding.ItemFriendBinding

data class User(
	val email: String? = null,
	val name: String? = null,
	val profileImageUrl: String? = null
)

data class Friend(
	var email : String? = null,
	var name : String? = null,
	var profileImageUrl : String? = null
)

class AddFriendFragment : Fragment() {
	companion object{
		fun newInstance() : AddFriendFragment {
			return AddFriendFragment()
		}
	}
	private var friend : ArrayList<Friend> = arrayListOf()
	val binding by lazy {FragmentAddFriendBinding.inflate(layoutInflater)} // 뷰바인딩 설정
	private lateinit var database: DatabaseReference



	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment

		database = Firebase.database.reference

		binding.addFriendButton.setOnClickListener {
			showAddFriendDialog()
		}

		val recyclerViewAdapter = RecyclerViewAdapter()
		binding.homeRecycler.layoutManager = LinearLayoutManager(requireContext())
		binding.homeRecycler.adapter = recyclerViewAdapter



		return binding.root
	}

	inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.CustomViewHolder>() {

		init {
			val myUid = Firebase.auth.currentUser?.uid.toString()
			FirebaseDatabase.getInstance().reference.child("Users").addValueEventListener(object :
				ValueEventListener {
				override fun onCancelled(error: DatabaseError) {
				}

				@SuppressLint("NotifyDataSetChanged")
				override fun onDataChange(snapshot: DataSnapshot) {
					friend.clear()
					for (data in snapshot.children) {
						val item = data.getValue<Friend>()
						if (item == null || item.email == myUid) {
							continue // 본인은 친구창에서 제외
						}
						friend.add(item!!)
					}
					notifyDataSetChanged()
				}
			})
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
			val binding =
				ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			return CustomViewHolder(binding)
		}

		inner class CustomViewHolder(private val binding: ItemFriendBinding) :
			RecyclerView.ViewHolder(binding.root) {
			val imageView: ImageView = binding.homeItemIv
			val textView: TextView = binding.homeItemTv
			val textViewEmail: TextView = binding.homeItemEmail
		}

		override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
			if (friend.isEmpty()) {
				// 친구가 없는 경우 "친구가 없습니다." 라는 메시지를 보여줍니다.
				holder.textView.text = "친구가 없습니다."
				holder.textViewEmail.text = ""
				holder.imageView.visibility = View.GONE
			} else {
				// 친구가 있는 경우 친구 정보를 보여줍니다.
				val currentFriend = friend[position]
				Glide.with(holder.itemView.context)
					.load(currentFriend.profileImageUrl)
					.circleCrop()
					.into(holder.imageView)
				holder.textView.text = currentFriend.name
				holder.textViewEmail.text = currentFriend.email
				holder.imageView.visibility = View.VISIBLE
			}
		}

		override fun getItemCount(): Int {
			return friend.size // 친구가 몇명 있는지 확인
		}
	}

	private fun showAddFriendDialog() {
		// 뷰바인딩 설정
		val dialogBinding = AddFriendDialogBinding.inflate(layoutInflater)
		val alertDialog = AlertDialog.Builder(requireContext())
			.setView(dialogBinding.root)
			.create()

		// 리사이클러 뷰와 데이터 시작
		val users = mutableListOf<User>()
		val adapter = RecyclerViewAdapter()

		dialogBinding.userList.layoutManager = LinearLayoutManager(requireContext())
		dialogBinding.userList.adapter = adapter

		// EditText에 TextWatcher 설정
		dialogBinding.username.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

			override fun afterTextChanged(s: Editable?) {
				val searchKeyword = s.toString().trim()
				searchUsers(searchKeyword, users, adapter)
			}
		})

		alertDialog.show()
		dialogBinding.cancelButton.setOnClickListener {
			alertDialog.dismiss() // 다이얼로그 종료
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun searchUsers(keyword: String, users: MutableList<User>, adapter: RecyclerViewAdapter) {
		// Firestore 컬렉션 "users"에서 사용자를 검색하는 쿼리를 생성합니다.
		val query = Firebase.firestore.collection("users")
			.whereEqualTo("name", keyword) // "name" 필드를 검색 키워드와 비교합니다.

		// 쿼리를 실행하여 검색 결과를 가져옵니다.
		query.get()
			.addOnSuccessListener { querySnapshot ->
				// 성공적으로 쿼리를 수행한 경우, 검색 결과를 처리합니다.
				users.clear() // 이전 검색 결과를 비웁니다.

				// querySnapshot에서 사용자 정보를 가져와서 users 리스트에 추가합니다.
				for (document in querySnapshot) {
					val user = document.toObject<User>() // User 클래스에 맞게 사용자 정보를 가져옵니다.
					users.add(user)
				}

				// RecyclerView adapter에 새로운 데이터를 설정하여 화면에 보여줍니다.
				adapter.notifyDataSetChanged()
			}
			.addOnFailureListener {
				// 쿼리를 수행하는 중에 오류가 발생한 경우, 오류를 처리합니다.
				Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
			}
	}

}