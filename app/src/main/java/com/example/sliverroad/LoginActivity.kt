package com.example.sliverroad
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sliverroad.data.LoginRequest
import com.example.sliverroad.data.LoginResponse
import com.example.sliverroad.api.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val editId = findViewById<EditText>(R.id.editId)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val id = editId.text.toString()
            val password = editPassword.text.toString()
            val loginRequest = LoginRequest(id, password)

            ApiClient.apiService.login(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if (response.isSuccessful) {
                        val tokens = response.body()
                        val accessToken = tokens?.access_token ?: ""

                        Log.d("API", "로그인 성공: 토큰 = $accessToken")
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("access_token", accessToken)
                        startActivity(intent)
                        finish()

                    } else {
                        Log.e("API", "로그인 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("API", "로그인 오류", t)
                }
            })
            /*/ ✅ 테스트용: 서버 없이 MainActivity로 바로 이동
            Log.d("DEBUG", "서버 없이 화면 이동 테스트")
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()*/

        }}}