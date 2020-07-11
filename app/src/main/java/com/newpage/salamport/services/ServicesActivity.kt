package com.newpage.salamport.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newpage.salamport.R

class ServicesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

    }

    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, ServicesActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}
