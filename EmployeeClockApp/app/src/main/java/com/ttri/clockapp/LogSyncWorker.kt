package com.ttri.clockapp

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val db = Firebase.firestore
    private val storage = FirebaseStorage.getInstance("gs://employee-kiosk.firebasestorage.app")
    private val dao = AppDatabase.getDatabase(appContext).pendingLogDao()

    override suspend fun doWork(): Result {
        try {
            val pendingLogs = dao.getPendingLogs()
            println("🚀 [LogSyncWorker] Found ${pendingLogs.size} pending logs")

            for (log in pendingLogs) {
                var photoURL: String? = null

                // 🔹 Upload photo if exists
                if (!log.localPhotoPath.isNullOrEmpty()) {
                    val file = File(log.localPhotoPath)

                    if (file.exists()) {
                        try {
                            val monthPath = SimpleDateFormat("yyyy-MM", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("Asia/Manila")
                            }.format(Date(log.timestamp))

                            val storagePath =
                                "logs/${log.employeeID}/$monthPath/${log.timestamp}.jpg"

                            println("📸 Uploading photo → $storagePath")
                            val storageRef = storage.reference.child(storagePath)
                            storageRef.putFile(Uri.fromFile(file)).await()

                            photoURL = storageRef.downloadUrl.await().toString()
                            println("✅ Uploaded: $photoURL")

                            if (file.delete()) {
                                println("🗑️ Deleted local file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            println("❌ Upload failed for ${file.absolutePath}")
                            e.printStackTrace()
                            continue
                        }
                    } else {
                        println("⚠️ Missing file, purging log → ${log.localPhotoPath}")
                        dao.updateLog(log.copy(synced = true))
                        continue
                    }
                }

                // 🔹 Firestore data
                val data = hashMapOf(
                    "employeeID" to log.employeeID,
                    "action" to log.action,
                    "timestamp" to Date(log.timestamp) // ✅ Firestore Timestamp
                )

                if (photoURL != null) {
                    data[if (log.action == "clock_in") "photoIn" else "photoOut"] = photoURL
                }

                try {
                    db.collection("logs").add(data).await()
                    println("☁️ Firestore saved → ${log.employeeID} (${log.action})")

                    dao.updateLog(log.copy(synced = true))
                } catch (e: Exception) {
                    println("❌ Firestore write failed for ${log.employeeID}")
                    e.printStackTrace()
                }
            }

            println("🎉 Sync finished")
            return Result.success()

        } catch (e: Exception) {
            println("💥 Fatal error in sync")
            e.printStackTrace()
            return Result.retry()
        }
    }
}
