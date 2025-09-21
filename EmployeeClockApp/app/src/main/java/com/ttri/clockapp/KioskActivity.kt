package com.ttri.clockapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ttri.clockapp.databinding.ActivityKioskBinding
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class KioskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKioskBinding
    private val validPin = "1234"
    private val maxPinLength = 6 // adjust if you want longer/shorter PINs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityKioskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Attach number buttons
        val numButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2,
            binding.btn3, binding.btn4, binding.btn5,
            binding.btn6, binding.btn7, binding.btn8,
            binding.btn9
        )

        for (button in numButtons) {
            button.setOnClickListener {
                val current = binding.editTextPin.text.toString()
                if (current.length < maxPinLength) {
                    binding.editTextPin.setText(current + button.text)
                }
            }
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            binding.editTextPin.setText("")
        }

        // Enter button = Validate PIN
        binding.btnEnter.setOnClickListener {
            val pin = binding.editTextPin.text.toString()

            if (pin == validPin) {
                Toast.makeText(this, "Clock-in successful!", Toast.LENGTH_SHORT).show()

                // Firebase event log
                Firebase.analytics.logEvent("clock_action", Bundle().apply {
                    putString("employee_pin", pin)
                    putString("result", "success")
                })

                binding.editTextPin.setText("")

            } else {
                Toast.makeText(this, "Invalid PIN!", Toast.LENGTH_SHORT).show()

                // Firebase event log
                Firebase.analytics.logEvent("clock_action", Bundle().apply {
                    putString("employee_pin", pin)
                    putString("result", "fail")
                })

                binding.editTextPin.setText("")
            }
        }
    }
}
