package com.example.sliverroad.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sliverroad.api.ApiClient
import com.example.sliverroad.data.Driver
import kotlinx.coroutines.launch
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class DriverViewModel : ViewModel() {
    private val _driver = MutableLiveData<Driver>()
    val driver: LiveData<Driver> = _driver

    fun loadDriverInfo(accessToken: String) {
        Log.d("API", "AccessToken: Bearer $accessToken")
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getDriverInfo("Bearer $accessToken")
                if (response.isSuccessful) {
                    _driver.value = response.body()
                } else {
                    Log.e("API", "Response Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("API", "Exception: ${e.message}")
            }
        }
    }
}



