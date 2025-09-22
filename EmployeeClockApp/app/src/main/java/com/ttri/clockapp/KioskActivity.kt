package com.ttri.clockapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class KioskActivity : AppCompatActivity() {

    // NOTE: If your IDs differ, update these two lines to match your layout XML:
    // e.g. R.id.pinEditText, R.id.clockButton
    private lateinit var pinInput: EditText
    private lateinit var actionButton: Button

    private val db = Firebase.firestore
    private val analytics = Firebase.analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiosk)

        pinInput = findViewById(R.id.pinInput)      // <-- change if your EditText id is different
        actionButton = findViewById(R.id.btnClock)  // <-- change if your Button id is different

        actionButton.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            if (pin.length != 6) {
                toast("Enter 6-digit PIN")
                return@setOnClickListener
            }
            actionButton.isEnabled = false
            validateAndLog(pin)
        }
    }

    private fun validateAndLog(pin: String) {
        // Query by PIN; we’ll check employedStatus on the client to avoid composite index requirements.
        db.collection("employees")
            .whereEqualTo("pin", pin)
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                if (qs.isEmpty) {
                    fail("Invalid PIN.")
                    return@addOnSuccessListener
                }

                val doc = qs.documents[0]
                val employed = doc.getBoolean("employedStatus") ?: false
                if (!employed) {
                    fail("Inactive employee.")
                    return@addOnSuccessListener
                }

                val employeeID = doc.getString("employeeID") ?: doc.id
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""

                decideActionAndWrite(employeeID, firstName, lastName)
            }
            .addOnFailureListener { e -> fail("Employee lookup failed: ${e.message}") }
    }

    private fun decideActionAndWrite(employeeID: String, firstName: String, lastName: String) {
        // Asia/Manila start-of-day for "today"
        val tz = TimeZone.getTimeZone("Asia/Manila")
        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay: Date = cal.time

        db.collection("logs")
            .whereEqualTo("employeeID", employeeID)
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                val lastAction = if (!qs.isEmpty) qs.documents[0].getString("action") else null
                val nextAction = if (lastAction == "clock_in") "clock_out" else "clock_in"
                writeLog(employeeID, firstName, lastName, nextAction)
            }
            .addOnFailureListener { e -> fail("Read last log failed: ${e.message}") }
    }

    private fun writeLog(employeeID: String, firstName: String, lastName: String, action: String) {
        val data = hashMapOf(
            "employeeID" to employeeID,
            "action" to action,
            "timestamp" to FieldValue.serverTimestamp(),
            "photoURL" to null // placeholder for STEP 2 (selfie)
        )

        db.collection("logs")
            .add(data)
            .addOnSuccessListener {
                analytics.logEvent("clock_action") {
                    param("employeeID", employeeID)
                    param("action", action)
                }
                toast("${fullName(firstName, lastName)} ${if (action == "clock_in") "clocked in" else "clocked out"}")
                pinInput.text.clear()
                actionButton.isEnabled = true
            }
            .addOnFailureListener { e -> fail("Write log failed: ${e.message}") }
    }

    private fun fullName(first: String, last: String): String =
        (listOf(first, last).filter { it.isNotBlank() }.joinToString(" ")).ifBlank { "Employee" }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun fail(msg: String) {
        actionButton.isEnabled = true
        toast(msg)
    }
}
