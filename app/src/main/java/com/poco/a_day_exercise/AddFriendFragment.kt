package com.poco.a_day_exercise

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.poco.a_day_exercise.databinding.AddFriendDialogBinding
import com.poco.a_day_exercise.databinding.AddFriendRequestsDialogBinding
import com.poco.a_day_exercise.databinding.FragmentAddFriendBinding
import com.poco.a_day_exercise.databinding.ItemFriendBinding
import com.poco.a_day_exercise.databinding.ItemFriendRequestsBinding
import com.poco.a_day_exercise.databinding.ItemUserBinding

open class Person(
	open var email: String? = null,
	open var name: String? = null,
	open var profileImageUrl: String? = null
)

data class Friend(
	override var email: String? = null,
	override var name: String? = null,
	override var profileImageUrl: String? = null
) : Person(email, name, profileImageUrl)

data class User(
	override var email: String? = null,
	override var name: String? = null,
	override var profileImageUrl: String? = null
) : Person(email, name, profileImageUrl)

data class FriendRequest(
	val requesterEmail: String? = null, // 친구 요청을 보낸 사용자의 이메일
	val receiverEmail: String? = null, // 친구 요청을 받은 사용자의 이메일
	val status: FriendRequestStatus = FriendRequestStatus.pending // 친구 요청 상태
)

enum class FriendRequestStatus {
	pending, // 대기 중
	accepted, // 수락됨
	rejected // 거절됨
}

class AddFriendFragment : Fragment() {

	companion object {
		fun newInstance(): AddFriendFragment {
			return AddFriendFragment()
		}
	}

	private var friends: ArrayList<Friend> = arrayListOf()
	private var users: ArrayList<User> = arrayListOf()
	private var persons: ArrayList<Person> = arrayListOf()
	private var requests: ArrayList<FriendRequest> = arrayListOf()
	private val binding by lazy { FragmentAddFriendBinding.inflate(layoutInflater) } // 뷰바인딩 설정

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// Inflate the layout for this fragment
		val view = binding.root

		// 현재 로그인한 사용자의 이메일을 가져옵니다.
		val currentUserEmail = Firebase.auth.currentUser?.email

		// 친구 목록 보여주기
		if (currentUserEmail != null) {
			FirebaseFirestore.getInstance().collection("Friends")
				.document(currentUserEmail)
				.collection("MyFriends")
				.get()
				.addOnSuccessListener { querySnapshot ->
					val friendEmails = mutableListOf<String>()

					// "MyFriends" 컬렉션에서 친구들의 이메일을 가져옵니다.
					for (document in querySnapshot) {
						val friendEmail = document.getString("email")
						friendEmail?.let { friendEmails.add(it) }
					}

					// 가져온 친구 이메일 리스트를 사용하여 "Users" 컬렉션에서 친구들의 정보를 가져옵니다.
					val usersCollection = FirebaseFirestore.getInstance().collection("Users")
					val friends = mutableListOf<Friend>()

					// 친구들의 이메일과 일치하는 사용자 문서를 조회합니다.
					for (friendEmail in friendEmails) {
						usersCollection.whereEqualTo("email", friendEmail)
							.get()
							.addOnSuccessListener { querySnapshot ->
								// querySnapshot에서 사용자 정보를 가져와서 friends 리스트에 추가합니다.
								for (document in querySnapshot) {
									val friend = document.toObject<Friend>()
									friends.add(friend)
								}

								// RecyclerView Adapter 설정
								val adapter = RecyclerViewFriendAdapter(friends)
								binding.homeUserList.layoutManager = LinearLayoutManager(requireContext())
								binding.homeUserList.adapter = adapter
								adapter.notifyDataSetChanged()
							}
							.addOnFailureListener { e ->
								// "Users" 컬렉션 조회 실패에 대한 처리
								Log.e(TAG, "Error fetching user document", e)
							}
					}

					// 친구추가 버튼 리스너
					binding.addFriendButton.setOnClickListener {
						showAddFriendDialog()
					}

					binding.checkRequest.setOnClickListener {
						showAddFriendRequestsDialog()
					}
				}
				.addOnFailureListener { e ->
					// "MyFriends" 컬렉션 조회 실패에 대한 처리
					Log.e(TAG, "Error fetching my friends", e)
				}
		}

		return view
	}

	inner class RecyclerViewFriendAdapter(private var items: List<Person>) :
		RecyclerView.Adapter<RecyclerViewFriendAdapter.CustomViewHolder>(),
		Filterable {

		// filter를 관리할 mutableList 정의
		private val filteredItems: MutableList<Person> = mutableListOf()
		private val itemFilter = ItemFilter()

		init {

			filteredItems.addAll(items)
		}

		// RecyclerViewAdapter 클래스 내부에서 ItemFilter 정의
		inner class ItemFilter : Filter() {
			override fun performFiltering(charSequence: CharSequence): FilterResults {
				val filterString = charSequence.toString().trim()

				val results = FilterResults()

				// 검색어가 없을 경우, 전체 리스트를 반환
				if (filterString.isEmpty()) {
					results.values = items // 전체 데이터 사용
					results.count = items.size
				} else {
					val filteredList: ArrayList<Person> = ArrayList()

					// 검색어와 사용자의 이름 또는 이메일이 일치하는 경우 필터링하여 filteredList에 추가
					for (item in persons) {
						val name = if (item is Friend) {
							item.name
						} else {
							(item as User).name
						}

						if (name?.contains(filterString, ignoreCase = true) == true) {
							filteredList.add(item)
						}
					}

					results.values = filteredList
					results.count = filteredList.size
				}

				return results
			}

			override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
				filteredItems.clear()
				filteredItems.addAll(filterResults.values as List<Person>)
				notifyDataSetChanged()
			}
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
			val information: Button = binding.information
		}

		fun updateData(newItems: List<Friend>) {
			items = newItems
			notifyDataSetChanged()
		}

		override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
			if (filteredItems.isEmpty()) {
				// 친구가 없는 경우 "친구가 없습니다." 라는 메시지를 보여줍니다.
				holder.textView.text = "친구가 없습니다."
				holder.textViewEmail.text = ""
				holder.imageView.visibility = View.GONE
			} else {
				// 친구가 있는 경우 친구 정보를 보여줍니다.
				val currentItem = filteredItems[position]
				Glide.with(holder.itemView.context)
					.load(currentItem.profileImageUrl)
					.circleCrop()
					.into(holder.imageView)
				holder.textView.text = currentItem.name
				holder.textViewEmail.text = currentItem.email
				holder.imageView.visibility = View.VISIBLE
				holder.information.setOnClickListener {
					// 아직 미구현
				}
			}
		}

		override fun getItemCount(): Int {
			return filteredItems.size // 검색된 친구 몇명인지 확인
		}

		// getFilter() 함수 오버라이드
		override fun getFilter(): Filter {
			return ItemFilter()
		}
	}

	inner class RecyclerViewRequestsAdapter(private var items: List<FriendRequest>) :
		RecyclerView.Adapter<RecyclerViewRequestsAdapter.CustomViewHolder>(),
		Filterable {

		// filter를 관리할 mutableList 정의
		private val filteredItems: MutableList<FriendRequest> = mutableListOf()
		private val itemFilter = ItemFilter()

		init {
			filteredItems.addAll(items)
		}

		// RecyclerViewAdapter 클래스 내부에서 ItemFilter 정의
		inner class ItemFilter : Filter() {
			override fun performFiltering(charSequence: CharSequence): FilterResults {
				val filterString = charSequence.toString().trim()

				val results = FilterResults()

				// 검색어가 없을 경우, 전체 리스트를 반환
				if (filterString.isEmpty()) {
					results.values = items // 전체 데이터 사용
					results.count = items.size
				} else {
					val filteredList: ArrayList<FriendRequest> = ArrayList()

					// 검색어와 사용자의 이름 또는 이메일이 일치하는 경우 필터링하여 filteredList에 추가
					for (item in items) {
						val receiver = item.receiverEmail
						if (receiver?.contains(filterString, ignoreCase = true) == true) {
							filteredList.add(item)
						}
					}

					results.values = filteredList
					results.count = filteredList.size
				}

				return results
			}

			override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
				filteredItems.clear()
				filteredItems.addAll(filterResults.values as List<FriendRequest>)
				notifyDataSetChanged()
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
			val binding = ItemFriendRequestsBinding.inflate(
				LayoutInflater.from(parent.context),
				parent,
				false
			)
			return CustomViewHolder(binding)
		}

		inner class CustomViewHolder(private val binding: ItemFriendRequestsBinding) :
			RecyclerView.ViewHolder(binding.root) {
			val textViewEmail: TextView = binding.homeItemEmail
			val acceptButton: Button = binding.accept
			val rejectButton: Button = binding.reject
		}

		fun updateData(newItems: List<FriendRequest>) {
			items = newItems
			notifyDataSetChanged()
		}

		override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
			if (filteredItems.isEmpty()) {
				// 친구가 없는 경우 "친구가 없습니다." 라는 메시지를 보여줍니다.
				holder.textViewEmail.text = "친구가 없습니다."
			} else {
				// 친구가 있는 경우 친구 정보를 보여줍니다.
				val currentItem = filteredItems[position]
				holder.textViewEmail.text = currentItem.requesterEmail
				holder.acceptButton.setOnClickListener {
					acceptFriendRequest(currentItem.requesterEmail!!)
					items = items.filterNot { it.requesterEmail == currentItem.requesterEmail }
					notifyDataSetChanged()
				}
				holder.rejectButton.setOnClickListener {
					rejectFriendRequest(currentItem.requesterEmail!!)
					// 친구 요청을 거절한 뒤에 데이터를 새로운 데이터로 갱신합니다.
					items = items.filterNot { it.requesterEmail == currentItem.requesterEmail }
					notifyDataSetChanged()
				}
			}
		}

		override fun getItemCount(): Int {
			return filteredItems.size // 검색된 친구 몇명인지 확인
		}

		// getFilter() 함수 오버라이드
		override fun getFilter(): Filter {
			return ItemFilter()
		}
	}

	inner class RecyclerViewUserAdapter(private var items: List<Person>) :
		RecyclerView.Adapter<RecyclerViewUserAdapter.CustomViewHolder>(),
		Filterable {

		// filter를 관리할 mutableList 정의
		private val filteredItems: MutableList<Person> = mutableListOf()
		private val itemFilter = ItemFilter()

		init {
			filteredItems.addAll(items)
		}

		// RecyclerViewAdapter 클래스 내부에서 ItemFilter 정의
		inner class ItemFilter : Filter() {
			override fun performFiltering(charSequence: CharSequence): FilterResults {
				val filterString = charSequence.toString().trim()

				val results = FilterResults()

				// 검색어가 없을 경우, 전체 리스트를 반환
				if (filterString.isEmpty()) {
					results.values = items // 전체 데이터 사용
					results.count = items.size
				} else {
					val filteredList: ArrayList<Person> = ArrayList()

					// 검색어와 사용자의 이름 또는 이메일이 일치하는 경우 필터링하여 filteredList에 추가
					for (item in persons) {
						val name = if (item is Friend) {
							item.name
						} else {
							(item as User).name
						}

						if (name?.contains(filterString, ignoreCase = true) == true) {
							filteredList.add(item)
						}
					}

					results.values = filteredList
					results.count = filteredList.size
				}

				return results
			}

			override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
				filteredItems.clear()
				filteredItems.addAll(filterResults.values as List<Person>)
				notifyDataSetChanged()
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
			val binding =
				ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
			return CustomViewHolder(binding)
		}

		inner class CustomViewHolder(private val binding: ItemUserBinding) :
			RecyclerView.ViewHolder(binding.root) {
			val imageView: ImageView = binding.homeItemIv
			val textView: TextView = binding.homeItemTv
			val textViewEmail: TextView = binding.homeItemEmail
			val addFriend: Button = binding.addFriend
		}

		fun updateData(newItems: List<User>) {
			items = newItems
			notifyDataSetChanged()
		}

		override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
			if (filteredItems.isEmpty()) {
				// 친구가 없는 경우 "친구가 없습니다." 라는 메시지를 보여줍니다.
				holder.textView.text = "친구가 없습니다."
				holder.textViewEmail.text = ""
				holder.imageView.visibility = View.GONE
			} else {
				// 친구가 있는 경우 친구 정보를 보여줍니다.
				val currentItem = filteredItems[position]
				Glide.with(holder.itemView.context)
					.load(currentItem.profileImageUrl)
					.circleCrop()
					.into(holder.imageView)
				holder.textView.text = currentItem.name
				holder.textViewEmail.text = currentItem.email
				holder.imageView.visibility = View.VISIBLE
				holder.addFriend.setOnClickListener {
					sendFriendRequest(currentItem.email!!)
				}
			}
		}

		override fun getItemCount(): Int {
			return filteredItems.size // 검색된 친구 몇명인지 확인
		}

		// getFilter() 함수 오버라이드
		override fun getFilter(): Filter {
			return ItemFilter()
		}
	}

	private fun showAddFriendDialog() {
		// 뷰바인딩 설정
		val dialogBinding = AddFriendDialogBinding.inflate(layoutInflater)
		val alertDialog = AlertDialog.Builder(requireContext())
			.setView(dialogBinding.root)
			.setCancelable(true) // 다이얼로그를 백키 눌렀을 때 종료 가능하도록 설정
			.create()
		// 현재 로그인한 사용자의 이메일을 가져옵니다.
		val currentUserEmail = Firebase.auth.currentUser?.email

		// Firestore 컬렉션 "Users"에서 전체 사용자 정보를 가져옵니다.
		FirebaseFirestore.getInstance().collection("Users")
			.whereNotEqualTo("email", currentUserEmail)
			.get()
			.addOnSuccessListener { querySnapshot ->
				val users = mutableListOf<User>()

				// querySnapshot에서 사용자 정보를 가져와서 users 리스트에 추가합니다.
				for (document in querySnapshot) {
					val user = document.toObject<User>()
					users.add(user)
				}

				// RecyclerView Adapter 설정
				val adapter = RecyclerViewUserAdapter(users)
				dialogBinding.userList.layoutManager = LinearLayoutManager(requireContext())
				dialogBinding.userList.adapter = adapter

				// EditText에 TextWatcher 설정
				dialogBinding.searchView.setOnQueryTextListener(object :
					SearchView.OnQueryTextListener {
					override fun onQueryTextSubmit(query: String?): Boolean {
						return false
					}

					override fun onQueryTextChange(newText: String?): Boolean {
						// 사용자의 입력에 따라 검색을 수행합니다.
						adapter.filter.filter(newText)
						return false
					}
				})

				// 다이얼로그 표시
				alertDialog.show()
				dialogBinding.cancelButton.setOnClickListener {
					alertDialog.dismiss() // 다이얼로그 종료
				}
			}
			.addOnFailureListener { e ->
				// 파이어스토어에서 사용자 정보를 가져오는데 실패한 경우 처리
				Toast.makeText(requireContext(), "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
				Log.e(TAG, "Error fetching users", e)
			}
	}

	// 친구 요청 보내기
	private fun sendFriendRequest(friendEmail: String) {
		val currentUserEmail = Firebase.auth.currentUser?.email

		// Firestore의 "FriendRequests" 컬렉션에 친구 요청 정보를 추가합니다.
		val friendRequestData = hashMapOf(
			"requesterEmail" to currentUserEmail,
			"receiverEmail" to friendEmail,
			"status" to "pending" // 친구 요청 상태를 "pending"으로 설정 (수락 전 상태)
		)

		// Firestore의 "FriendRequests" 컬렉션에 문서를 추가합니다.
		FirebaseFirestore.getInstance().collection("FriendRequests")
			.add(friendRequestData)
			.addOnSuccessListener { documentReference ->
				// 친구 요청이 성공적으로 보내졌을 경우 처리
				Toast.makeText(requireContext(), "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show()
			}
			.addOnFailureListener { e ->
				// 친구 요청 보내기 실패한 경F우 처리
				Toast.makeText(requireContext(), "친구 요청을 보내지 못했습니다.", Toast.LENGTH_SHORT).show()
				Log.e(TAG, "Error sending friend request", e)
			}
	}

	private fun showAddFriendRequestsDialog() {
		// 뷰바인딩 설정
		val dialogBinding = AddFriendRequestsDialogBinding.inflate(layoutInflater)
		val alertDialog = AlertDialog.Builder(requireContext())
			.setView(dialogBinding.root)
			.setCancelable(true) // 다이얼로그를 백키 눌렀을 때 종료 가능하도록 설정
			.create()

		// 현재 로그인한 사용자의 이메일을 가져옵니다.
		val currentUserEmail = Firebase.auth.currentUser?.email

		// Firestore 컬렉션 "FriendRequests"에서 전체 사용자 정보를 가져옵니다.
		FirebaseFirestore.getInstance().collection("FriendRequests")
			.whereEqualTo("receiverEmail", currentUserEmail)
			.get()
			.addOnSuccessListener { querySnapshot ->
				val friendRequest = mutableListOf<FriendRequest>()

				// querySnapshot에서 사용자 정보를 가져와서 friendRequest 리스트에 추가합니다.
				for (document in querySnapshot) {
					val request = document.toObject<FriendRequest>()
					friendRequest.add(request)
				}

				// RecyclerView Adapter 설정
				val adapter = RecyclerViewRequestsAdapter(friendRequest)
				dialogBinding.userList.layoutManager = LinearLayoutManager(requireContext())
				dialogBinding.userList.adapter = adapter

				// 다이얼로그 표시
				alertDialog.show()
				dialogBinding.cancelButton.setOnClickListener {
					alertDialog.dismiss() // 다이얼로그 종료
				}
			}
			.addOnFailureListener { e ->
				// 파이어스토어에서 사용자 정보를 가져오는데 실패한 경우 처리
				Toast.makeText(requireContext(), "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
				Log.e(TAG, "Error fetching users", e)
			}
	}

	// 친구 요청 수락 기능을 처리하는 함수
	private fun acceptFriendRequest(friendRequestId: String) {
		val firestore = FirebaseFirestore.getInstance()
		val friendRequestsCollection = firestore.collection("FriendRequests")
		val currentUserEmail = Firebase.auth.currentUser?.email

		// friendRequestId를 사용하여 해당 친구 요청을 찾는 쿼리
		friendRequestsCollection
			.whereEqualTo("requesterEmail", friendRequestId)
			.whereEqualTo("receiverEmail", currentUserEmail)
			.get()
			.addOnSuccessListener { querySnapshot ->
				if (!querySnapshot.isEmpty) {
					// 찾은 친구 요청 문서가 여기에 있습니다.
					val friendRequestDocument = querySnapshot.documents[0]
					// 문서를 업데이트하고, 친구 요청을 수락하는 로직을 이어서 진행합니다.
					friendRequestDocument.reference
						.update("status", FriendRequestStatus.accepted.name)
						.addOnSuccessListener {
							// 성공적으로 업데이트되었을 경우에 대한 처리
							Log.d(TAG, "Friend request accepted.")
							// ... 친구 추가 등 추가 작업 진행 ...
							// 친구 요청을 수락한 상대방의 이메일 정보를 가져옵니다.
							val friendRequest = friendRequestDocument.toObject<FriendRequest>()
							val friendEmail = friendRequest?.requesterEmail

							// 내 친구 목록에 상대방을 추가합니다.
							addFriendToMyList(currentUserEmail, friendEmail)

							// 상대방의 친구 목록에 나를 추가합니다.
							addMeToFriendList(currentUserEmail, friendEmail)

							// 문서를 삭제하는 로직
							friendRequestDocument.reference
								.delete()
								.addOnSuccessListener {
									Log.d(TAG, "Friend request document deleted.")
								}
								.addOnFailureListener { e ->
									Log.e(TAG, "Error deleting friend request document", e)
								}
						}
						.addOnFailureListener { e ->
							// 업데이트 실패에 대한 처리
							Log.e(TAG, "Error accepting friend request", e)
						}
				} else {
					// 해당하는 친구 요청 문서를 찾지 못한 경우에 대한 처리
					Log.d(TAG, "Friend request not found.")
				}
			}
			.addOnFailureListener { e ->
				// 쿼리 실패에 대한 처리
				Log.e(TAG, "Error querying friend request", e)
			}
	}


	// 내 친구 목록에 상대방을 추가하는 함수
	private fun addFriendToMyList(currentUserEmail: String?, friendEmail: String?) {
		if (currentUserEmail != null && friendEmail != null) {
			val firestore = FirebaseFirestore.getInstance()
			val friendsCollection = firestore.collection("Friends")

			// 내 친구 목록에 상대방을 추가합니다.
			val currentUserFriend = hashMapOf(
				"email" to friendEmail,
			)

			friendsCollection
				.document(currentUserEmail)
				.collection("MyFriends")
				.document(friendEmail)
				.set(currentUserFriend)
				.addOnSuccessListener {
					Log.d(TAG, "Friend added to my list.")
				}
				.addOnFailureListener { e ->
					Log.e(TAG, "Error adding friend to my list", e)
				}
		}
	}

	// 상대방의 친구 목록에 나를 추가하는 함수
	private fun addMeToFriendList(currentUserEmail: String?, friendEmail: String?) {
		if (currentUserEmail != null && friendEmail != null) {
			val firestore = FirebaseFirestore.getInstance()
			val friendUserFriend = hashMapOf(
				"email" to currentUserEmail,
				// 내 추가 정보를 필요에 따라 여기에 추가할 수 있습니다.
			)

			firestore.collection("Friends")
				.document(friendEmail)
				.collection("MyFriends")
				.document(currentUserEmail)
				.set(friendUserFriend)
				.addOnSuccessListener {
					Log.d(TAG, "I added to friend's list.")
				}
				.addOnFailureListener { e ->
					Log.e(TAG, "Error adding me to friend's list", e)
				}
		}
	}


	// 친구 요청 거절 기능을 처리하는 함수
	fun rejectFriendRequest(friendRequestId: String) {
		val firestore = FirebaseFirestore.getInstance()
		val friendRequestsCollection = firestore.collection("FriendRequests")
		val currentUserEmail = Firebase.auth.currentUser?.email

		// friendRequestId를 사용하여 해당 친구 요청을 찾는 쿼리
		friendRequestsCollection
			.whereEqualTo("requesterEmail", friendRequestId)
			.whereEqualTo("receiverEmail", currentUserEmail)
			.get()
			.addOnSuccessListener { querySnapshot ->
				if (!querySnapshot.isEmpty) {
					// 찾은 친구 요청 문서가 여기에 있습니다.
					val friendRequestDocument = querySnapshot.documents[0]
					// 문서를 업데이트하고, 친구 요청을 수락하는 로직을 이어서 진행합니다.
					friendRequestDocument.reference
						.update("status", FriendRequestStatus.rejected.name)
						.addOnSuccessListener {
							// 성공적으로 업데이트되었을 경우에 대한 처리
							Log.d(TAG, "Friend request rejected.")

							// 문서를 삭제하는 로직
							friendRequestDocument.reference
								.delete()
								.addOnSuccessListener {
									Log.d(TAG, "Friend request document deleted.")
								}
								.addOnFailureListener { e ->
									Log.e(TAG, "Error deleting friend request document", e)
								}
						}
						.addOnFailureListener { e ->
							// 업데이트 실패에 대한 처리
							Log.e(TAG, "Error accepting friend request", e)
						}
				} else {
					// 해당하는 친구 요청 문서를 찾지 못한 경우에 대한 처리
					Log.d(TAG, "Friend request not found.")
				}
			}
			.addOnFailureListener { e ->
				// 쿼리 실패에 대한 처리
				Log.e(TAG, "Error querying friend request", e)
			}
	}
}
