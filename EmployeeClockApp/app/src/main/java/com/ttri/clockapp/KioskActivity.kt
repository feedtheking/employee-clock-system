package com.ttri.clockapp

import com.ttri.clockapp.ImageUtils
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import android.widget.TextView
import android.view.ViewGroup
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// 🔹 Trigger background log sync job
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

    // 🔹 Temp values for current action
    private var pendingEmployeeID: String? = null
    private var pendingFirstName: String? = null
    private var pendingLastName: String? = null
    private var pendingAction: String? = null
    private var photoFile: File? = null  // ✅ always absolute file

    // 🔹 Camera permission launcher
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraCapture()
            } else {
                fail("Camera permission denied.")
            }
        }

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

        btnClear.setOnClickListener { pinInput.setText("") }

        // clock button
        actionButton.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            if (pin.length != 6) {
                showErrorDialog("Please enter a 6-digit PIN.")
                return@setOnClickListener
            }
            actionButton.isEnabled = false
            validateAndPrepareLog(pin)
        }

        // manual sync button
        syncButton.setOnClickListener {
            syncEmployees(manual = true)
            triggerLogSync(this) // also trigger log sync
        }

        // auto sync once on load
        syncEmployees(manual = false)

        // schedule recurring sync every 30 min
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    syncEmployees(manual = false)
                    triggerLogSync(this@KioskActivity)
                }
            }
        }, 30 * 60 * 1000L, 30 * 60 * 1000L)
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
                    showErrorDialog("Sync failed. Check internet connection.\n${e.message}")
                }
            }
    }

    private fun validateAndPrepareLog(pin: String) {
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

                // 🔹 look back 2 days for last action
                val tz = TimeZone.getTimeZone("Asia/Manila")
                val cal = Calendar.getInstance(tz).apply {
                    add(Calendar.DAY_OF_YEAR, -2)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val lookbackDate: Date = cal.time

                db.collection("logs")
                    .whereEqualTo("employeeID", employeeID)
                    .whereGreaterThanOrEqualTo("timestamp", lookbackDate)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { qs2 ->
                        val lastAction = if (!qs2.isEmpty) qs2.documents[0].getString("action") else null
                        val nextAction = if (lastAction == null || lastAction == "clock_out") {
                            "clock_in"
                        } else {
                            "clock_out"
                        }

                        // save for after photo
                        pendingEmployeeID = employeeID
                        pendingFirstName = firstName
                        pendingLastName = lastName
                        pendingAction = nextAction

                        // check permission before camera
                        if (ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            startCameraCapture()
                        } else {
                            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                    .addOnFailureListener { e -> fail("Read last log failed: ${e.message}") }
            }
            .addOnFailureListener { e -> fail("Employee lookup failed: ${e.message}") }
    }

    // 🔹 launch camera intent
    private fun startCameraCapture() {
        val timeStamp = System.currentTimeMillis() // ✅ unified with LogSyncWorker
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, "PHOTO_${timeStamp}.jpg") // ✅ consistent filename
        photoFile = file
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        takePicture.launch(uri)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoFile?.let { file ->
                    ImageUtils.resizeImageIfNeeded(file) // resize to 640x480

                    val log = PendingLogEntity(
                        employeeID = pendingEmployeeID ?: "",
                        action = pendingAction ?: "clock_in",
                        timestamp = System.currentTimeMillis(), // ✅ Long (ms since epoch)
                        localPhotoPath = file.absolutePath,
                        synced = false
                    )
                    lifecycleScope.launch { pendingLogDao.insert(log) }

                    runOnUiThread {
                        analytics.logEvent("clock_action") {
                            param("employeeID", pendingEmployeeID ?: "")
                            param("action", pendingAction ?: "clock_in")
                        }
                        pinInput.setText("")
                        actionButton.isEnabled = true
                        showActionDialog(
                            pendingFirstName ?: "",
                            pendingLastName ?: "",
                            pendingAction ?: "clock_in"
                        )
                        triggerLogSync(this) // auto sync after photo
                    }
                }
            } else {
                fail("Photo capture canceled.")
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
