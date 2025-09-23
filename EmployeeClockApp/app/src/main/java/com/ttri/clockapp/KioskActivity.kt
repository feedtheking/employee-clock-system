package com.ttri.clockapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.TextView
import android.view.ViewGroup
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

// 🔹 WorkManager trigger function
fun triggerLogSync(context: android.content.Context) {
    val workRequest = OneTimeWorkRequestBuilder<LogSyncWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}

class KioskActivity : AppCompatActivity() {

    private lateinit var pinInput: EditText
    private lateinit var actionButton: Button
    private lateinit var syncButton: Button

    // numpad buttons
    private lateinit var numButtons: List<Button>
    private lateinit var btnClear: Button
    private lateinit var btnDelete: Button

    private val db = Firebase.firestore
    private val analytics = Firebase.analytics
    private val timer = Timer()

    // 🔹 Room database
    private lateinit var pendingLogDao: PendingLogDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiosk)

        pinInput = findViewById(R.id.pinInput)
        actionButton = findViewById(R.id.btnClock)
        syncButton = findViewById(R.id.btnSync)

        // 🔹 init Room DAO
        pendingLogDao = AppDatabase.getDatabase(this).pendingLogDao()

        // collect numpad buttons
        numButtons = listOf(
            findViewById(R.id.btn0),
            findViewById(R.id.btn1),
            findViewById(R.id.btn2),
            findViewById(R.id.btn3),
            findViewById(R.id.btn4),
            findViewById(R.id.btn5),
            findViewById(R.id.btn6),
            findViewById(R.id.btn7),
            findViewById(R.id.btn8),
            findViewById(R.id.btn9)
        )
        btnClear = findViewById(R.id.btnClear)
        btnDelete = findViewById(R.id.btnDelete)

        // numpad click handlers
        numButtons.forEach { btn ->
            btn.setOnClickListener {
                val current = pinInput.text.toString()
                if (current.length < 6) {
                    pinInput.setText(current + btn.text.toString())
                }
            }
        }

        btnDelete.setOnClickListener {
            val current = pinInput.text.toString()
            if (current.isNotEmpty()) {
                pinInput.setText(current.dropLast(1))
            }
        }

        btnClear.setOnClickListener {
            pinInput.setText("")
        }

        // clock button
        actionButton.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            if (pin.length != 6) {
                showErrorDialog("Please enter a 6-digit PIN.")
                return@setOnClickListener
            }
            actionButton.isEnabled = false
            validateAndLog(pin)
        }

        // sync button (manual → show dialog)
        syncButton.setOnClickListener {
            syncEmployees(manual = true)
            triggerLogSync(this) // 🔹 force log sync manually too
        }

        // auto sync on login
        syncEmployees(manual = false)

        // schedule regular sync every 30 minutes
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    syncEmployees(manual = false)
                    triggerLogSync(this@KioskActivity) // 🔹 background log sync
                }
            }
        }, 30 * 60 * 1000L, 30 * 60 * 1000L) // 30 min
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    private fun syncEmployees(manual: Boolean) {
        db.collection("employees")
            .get()
            .addOnSuccessListener { qs ->
                if (manual) {
                    val count = qs.size()
                    showInfoDialog("Sync Complete", "Employee list refreshed: $count employees")
                }
            }
            .addOnFailureListener { e ->
                if (manual) {
                    showErrorDialog("Sync failed: ${e.message}")
                }
            }
    }

    private fun validateAndLog(pin: String) {
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
                val employed = doc.getBoolean("employedStatus") ?: true
                if (!employed) {
                    fail("This employee is marked inactive.")
                    return@addOnSuccessListener
                }

                val employeeID = (doc.getString("employeeID") ?: "").ifBlank { doc.id }
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""

                decideActionAndWrite(employeeID, firstName, lastName)
            }
            .addOnFailureListener { e -> fail("Employee lookup failed: ${e.message}") }
    }

    private fun decideActionAndWrite(employeeID: String, firstName: String, lastName: String) {
        // Asia/Manila start-of-day
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

    // 🔹 Now writes to Room instead of Firestore directly
    private fun writeLog(employeeID: String, firstName: String, lastName: String, action: String) {
        lifecycleScope.launch {
            val log = PendingLogEntity(
                employeeID = employeeID,
                action = action,
                timestamp = System.currentTimeMillis(),
                localPhotoPath = null,  // will fill when we add camera
                synced = false
            )
            pendingLogDao.insert(log)

            runOnUiThread {
                analytics.logEvent("clock_action") {
                    param("employeeID", employeeID)
                    param("action", action)
                }
                pinInput.setText("")
                actionButton.isEnabled = true
                showActionDialog(firstName, lastName, action)

                // 🔹 trigger sync right after log (optional)
                triggerLogSync(this@KioskActivity)
            }
        }
    }

    private fun showActionDialog(firstName: String, lastName: String, action: String) {
        val name = listOf(firstName, lastName).filter { it.isNotBlank() }
            .joinToString(" ").ifBlank { "Employee" }
        val verb = if (action == "clock_in") "Clock-in" else "Clock-out"
        val message = "$name — $verb recorded."

        val dialog = AlertDialog.Builder(this)
            .setTitle("$verb Successful")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val msgView = dialog.findViewById<TextView>(android.R.id.message)
            msgView?.apply {
                textSize = 16f
                setTextColor(
                    if (action == "clock_in") 0xFF006400.toInt() // Dark green
                    else 0xFF800000.toInt() // Maroon
                )
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    topMargin = 20
                }
            }
        }
        dialog.show()
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Action Failed")
            .setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun fail(msg: String) {
        actionButton.isEnabled = true
        showErrorDialog(msg)
    }
}
