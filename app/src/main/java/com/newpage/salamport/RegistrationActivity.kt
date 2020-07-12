package com.newpage.salamport

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val client = OkHttpClient.Builder().build()

        findViewById<Button>(R.id.submitRegister).setOnClickListener {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("login", findViewById<EditText>(R.id.emailRegister).text.toString())
                    .add("password", findViewById<EditText>(R.id.passwordRegister).text.toString())
                    .add("email", findViewById<EditText>(R.id.emailRegister).text.toString())
                    .add("name", findViewById<EditText>(R.id.nameRegister).text.toString())
                    .add("surname", findViewById<EditText>(R.id.surnameRegister).text.toString())
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/normal_reg.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await()
                }
                LoginPassword.startFrom(this@RegistrationActivity)
            }
        }
    }

    companion object {
        fun startFrom(context: Context) {
            val intent = Intent(context, RegistrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
