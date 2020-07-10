package com.newpage.salamport

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.nio.charset.StandardCharsets

class LoginPassword : AppCompatActivity() {

    private lateinit var username: String
    private lateinit var password: String

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_password)

        username = intent.getStringExtra("username")
        password = intent.getStringExtra("password")

        findViewById<Button>(R.id.submitLogin).setOnClickListener {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("login", findViewById<EditText>(R.id.usernameEdit).text.toString())
                    .add("password", findViewById<EditText>(R.id.passwordEdit).text.toString())
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/normal_auth.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await()
                }
                try {
                    val string = responseString.body?.string()
                    val mmJSON = JSONObject(string)
                    saveLoginAndPassword(findViewById<EditText>(R.id.usernameEdit).text.toString(),
                        findViewById<EditText>(R.id.passwordEdit).text.toString())
                    UserActivity.startFrom(this@LoginPassword, string)
                } catch (jex: JSONException) {
                    MainActivity.startFrom(this@LoginPassword)
                }
            }
        }

        if (!((username.equals("-1")) && (password.equals("-1")))) {
            GlobalScope.launch {
                val requestBody = FormBody.Builder()
                    .add("login", username)
                    .add("password", password)
                    .build()
                val request = Request.Builder()
                    .url("https://salamport.newpage.xyz/api/normal_auth.php")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                val responseString = withContext(Dispatchers.IO) {
                    client.newCall(request).await()
                }
                try {
                    val string = responseString.body?.string()
                    val mmJSON = JSONObject(string)
                    saveLoginAndPassword(findViewById<EditText>(R.id.usernameEdit).text.toString(),
                        findViewById<EditText>(R.id.passwordEdit).text.toString())
                    UserActivity.startFrom(this@LoginPassword, string)
                } catch (jex: JSONException) {
                    MainActivity.startFrom(this@LoginPassword)
                }
            }
        }
    }

    private fun saveLoginAndPassword(username: String, password: String) {
        this.openFileOutput("username", Context.MODE_PRIVATE).use {
            it.write(username.toByteArray(charset = StandardCharsets.UTF_8))
            it.flush()
        }
        this.openFileOutput("password", Context.MODE_PRIVATE).use {
            it.write(password.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }

    companion object {
        fun startFrom(context: Context, username: String = "-1", password: String = "-1") {
            val intent = Intent(context, LoginPassword::class.java)
            intent.putExtra("username", username)
            intent.putExtra("password", password)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
