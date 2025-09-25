package com.ttri.clockapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.ktx.Firebase
import com.google.firebase.appcheck.ktx.appCheck

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // 🔹 For testing only → bypass App Check
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        println("✅ Firebase initialized (Debug AppCheck enabled, test mode)")
    }
}
