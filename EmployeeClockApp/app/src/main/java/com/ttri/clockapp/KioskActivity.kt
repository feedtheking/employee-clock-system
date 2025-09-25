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
import java.util.*
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

fun triggerLogSync(context: android.content.Context) {
    val workRequest = OneTimeWorkRequestBuilder<LogSyncWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)
}

class KioskActivity : AppCompatActivity() {

    private lateinit var pinInput: EditText
    private lateinit var actionButton: Button
    private lateinit var syncButton: Button

    private lateinit var numButtons: List<Button>
    private lateinit var btnClear: Button
    private lateinit var btnDelete: Button

    private val db = Firebase.firestore
    private val analytics = Firebase.analytics
    private val timer = Timer()
    private lateinit var pendingLogDao: PendingLogDao

    private var pendingEmployeeID: String? = null
    private var pendingFirstName: String? = null
    private var pendingLastName: String? = null
    private var pendingAction: String? = null
    private var photoFile: File? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCameraCapture() else fail("Camera permission denied.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kiosk)

        pinInput = findViewById(R.id.pinInput)
        actionButton = findViewById(R.id.btnClock)
        syncButton = findViewById(R.id.btnSync)
        pendingLogDao = AppDatabase.getDatabase(this).pendingLogDao()

        numButtons = listOf(
            findViewById(R.id.btn0), findViewById(R.id.btn1), findViewById(R.id.btn2),
            findViewById(R.id.btn3), findViewById(R.id.btn4), findViewById(R.id.btn5),
            findViewById(R.id.btn6), findViewById(R.id.btn7), findViewById(R.id.btn8),
            findViewById(R.id.btn9)
        )
        btnClear = findViewById(R.id.btnClear)
        btnDelete = findViewById(R.id.btnDelete)

        numButtons.forEach { btn ->
            btn.setOnClickListener {
                val current = pinInput.text.toString()
                if (current.length < 6) pinInput.setText(current + btn.text.toString())
            }
        }
        btnDelete.setOnClickListener {
            val current = pinInput.text.toString()
            if (current.isNotEmpty()) pinInput.setText(current.dropLast(1))
        }
        btnClear.setOnClickListener { pinInput.setText("") }

        actionButton.setOnClickListener {
            val pin = pinInput.text.toString().trim()
            if (pin.length != 6) {
                showErrorDialog("Please enter a 6-digit PIN.")
                return@setOnClickListener
            }
            actionButton.isEnabled = false
            validateAndPrepareLog(pin)
        }

        syncButton.setOnClickListener {
            syncEmployees(manual = true)
            triggerLogSync(this)
        }

        syncEmployees(manual = false)

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
                if (manual) showInfoDialog("Sync Complete", "Employee list: ${qs.size()} employees")
            }
            .addOnFailureListener { e ->
                if (manual) showErrorDialog("Sync failed: ${e.message}")
            }
    }

    private fun validateAndPrepareLog(pin: String) {
        db.collection("employees")
            .whereEqualTo("pin", pin)
            .limit(1)
            .get()
            .addOnSuccessListener { qs ->
                if (qs.isEmpty) {
                    fail("Invalid PIN."); return@addOnSuccessListener
                }
                val doc = qs.documents[0]
                if (!(doc.getBoolean("employedStatus") ?: true)) {
                    fail("Employee inactive."); return@addOnSuccessListener
                }

                val employeeID = (doc.getString("employeeID") ?: "").ifBlank { doc.id }
                val firstName = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""

                val lookbackDate = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")).apply {
                    add(Calendar.DAY_OF_YEAR, -2)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                db.collection("logs")
                    .whereEqualTo("employeeID", employeeID)
                    .whereGreaterThanOrEqualTo("timestamp", lookbackDate)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { qs2 ->
                        val lastAction = qs2.documents.firstOrNull()?.getString("action")
                        val nextAction = if (lastAction == null || lastAction == "clock_out")
                            "clock_in" else "clock_out"

                        pendingEmployeeID = employeeID
                        pendingFirstName = firstName
                        pendingLastName = lastName
                        pendingAction = nextAction

                        if (ContextCompat.checkSelfPermission(
                                this, android.Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) startCameraCapture()
                        else requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                    }
                    .addOnFailureListener { e -> fail("Log check failed: ${e.message}") }
            }
            .addOnFailureListener { e -> fail("Employee lookup failed: ${e.message}") }
    }

    private fun startCameraCapture() {
        val timeStamp = System.currentTimeMillis()
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, "PHOTO_${timeStamp}.jpg")
        photoFile = file
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        takePicture.launch(uri)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoFile?.let { file ->
                    ImageUtils.resizeImageIfNeeded(file)
                    val log = PendingLogEntity(
                        employeeID = pendingEmployeeID ?: "",
                        action = pendingAction ?: "clock_in",
                        timestamp = System.currentTimeMillis(),
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
                            pendingFirstName ?: "", pendingLastName ?: "", pendingAction ?: "clock_in"
                        )
                        triggerLogSync(this)
                    }
                }
            } else fail("Photo capture canceled.")
        }

    private fun showActionDialog(firstName: String, lastName: String, action: String) {
        val name = listOf(firstName, lastName).filter { it.isNotBlank() }
            .joinToString(" ").ifBlank { "Employee" }
        val verb = if (action == "clock_in") "Clock-in" else "Clock-out"

        val dialog = AlertDialog.Builder(this)
            .setTitle("$verb Successful")
            .setMessage("$name — $verb recorded.")
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val msgView = dialog.findViewById<TextView>(android.R.id.message)
            msgView?.apply {
                textSize = 16f
                setTextColor(if (action == "clock_in") 0xFF006400.toInt() else 0xFF800000.toInt())
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply { topMargin = 20 }
            }
        }
        dialog.show()
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }.show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this).setTitle("Action Failed").setMessage(message)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }.show()
    }

    private fun fail(msg: String) {
        actionButton.isEnabled = true
        showErrorDialog(msg)
    }
}
