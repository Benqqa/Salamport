package com.newpage.salamport

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class FriendsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)
    }

    companion object {
        fun startFrom(context: Context, token: String, session: String) {
            val intent = Intent(context, FriendsActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
