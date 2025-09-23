package com.ttri.clockapp

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class LogSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val dao = AppDatabase.getDatabase(appContext).pendingLogDao()

    override suspend fun doWork(): Result {
        try {
            val pendingLogs = dao.getPendingLogs()

            for (log in pendingLogs) {
                var photoURL: String? = null

                // 1️⃣ Upload photo if exists
                if (!log.localPhotoPath.isNullOrEmpty()) {
                    try {
                        val fileUri = Uri.parse(log.localPhotoPath)

                        // Format path: logs/{employeeID}/{YYYY-MM}/{timestamp}.jpg
                        val date = Date(log.timestamp)
                        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                        val monthPath = sdf.format(date)

                        val storageRef = storage.reference
                            .child("logs/${log.employeeID}/$monthPath/${log.timestamp}.jpg")

                        storageRef.putFile(fileUri).await()
                        photoURL = storageRef.downloadUrl.await().toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        continue // skip this log if photo upload failed
                    }
                }

                // 2️⃣ Upload log to Firestore
                val data = hashMapOf(
                    "employeeID" to log.employeeID,
                    "action" to log.action,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "photoURL" to photoURL
                )

                try {
                    db.collection("logs").add(data).await()

                    // 3️⃣ Mark as synced
                    dao.updateLog(log.copy(synced = true))
                } catch (e: Exception) {
                    e.printStackTrace()
                    // skip if Firestore failed, retry later
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
