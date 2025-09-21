package com.ttri.clockapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ttri.clockapp.databinding.ActivityMainBinding

// Firebase imports
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Analytics
        analytics = Firebase.analytics

        // Log a test event
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)

        // Show text on screen
        binding.textViewWelcome.text = "Welcome to ClockApp"
    }
}
