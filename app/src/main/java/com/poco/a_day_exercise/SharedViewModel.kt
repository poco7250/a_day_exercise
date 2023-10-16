package com.poco.a_day_exercise

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
	private val selectedDateLiveData = MutableLiveData<String>()

	fun setSelectedDate(selectedDate: String) {
		selectedDateLiveData.value = selectedDate
	}

	fun getSelectedDate(): LiveData<String> {
		return selectedDateLiveData
	}
}

class SharedViewModelFriend : ViewModel() {
	private val selectedDateLiveData = MutableLiveData<String>()
	private val useremailLiveData = MutableLiveData<String>()
	fun setSelectedDate(selectedDate: String) {
		selectedDateLiveData.value = selectedDate
	}

	fun setuseremail(useremail: String) {
		useremailLiveData.value = useremail
	}

	fun getSelectedDate(): LiveData<String> {
		return selectedDateLiveData
	}

	fun getuseremail(): LiveData<String> {
		return useremailLiveData
	}
}