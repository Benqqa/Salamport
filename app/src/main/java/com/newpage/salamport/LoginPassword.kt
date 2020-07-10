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

    private val client = OkHttpClient.Builder().build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_password)

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

                    val grishaToken = mmJSON.getString("token")
                    val grishaSession = mmJSON.getString("session")
                    saveTokenAndSession(grishaToken, grishaSession)
                    UserActivity.startFrom(this@LoginPassword, grishaToken, grishaSession)
                } catch (jex: JSONException) {
                    MainActivity.startFrom(this@LoginPassword)
                }
            }
        }

    }

    private fun saveTokenAndSession(token: String, session: String) {
        this.openFileOutput("session", Context.MODE_PRIVATE).use {
            it.write(session.toByteArray(charset = StandardCharsets.UTF_8))
            it.flush()
        }
        this.openFileOutput("token", Context.MODE_PRIVATE).use {
            it.write(token.toByteArray(StandardCharsets.UTF_8))
            it.flush()
        }
    }

    companion object {
        fun startFrom(context: Context) {
            val intent = Intent(context, LoginPassword::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

    }
}
