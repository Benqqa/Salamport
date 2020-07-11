package com.newpage.salamport

import android.R
import android.app.Application
import android.util.Log
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKTokenExpiredHandler



class TokenHandler : Application (){
    override fun onCreate() {
        super.onCreate()
        VK.addTokenExpiredHandler(tokenTracker)
    }

    private val tokenTracker = object: VKTokenExpiredHandler {
        override fun onTokenExpired() {
            Log.e("error", "token expired")
            MainActivity.startFrom(this@TokenHandler)
        }
    }
}