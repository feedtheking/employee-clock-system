package com.ttri.clockapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // 🔹 For testing only → allow debug token App Check
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // 🔹 Ensure Firebase Storage always uses your correct bucket
        FirebaseStorage.getInstance("gs://employee-kiosk.firebasestorage.app")
        println("✅ Firebase initialized with Storage bucket gs://employee-kiosk.firebasestorage.app")
    }
}
