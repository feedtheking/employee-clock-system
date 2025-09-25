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

    // ✅ Force Firebase to use the correct storage bucket
    private val storage = FirebaseStorage.getInstance("gs://employee-kiosk.firebasestorage.app")

    // ✅ Only PendingLogDao is kept
    private val dao = AppDatabase.getDatabase(appContext).pendingLogDao()

    override suspend fun doWork(): Result {
        try {
            val pendingLogs = dao.getPendingLogs()
            println("🚀 [LogSyncWorker] Starting sync… Found ${pendingLogs.size} logs")

            for (log in pendingLogs) {
                var photoURL: String? = null

                // 1️⃣ Upload photo if exists
                if (!log.localPhotoPath.isNullOrEmpty()) {
                    val file = File(log.localPhotoPath)

                    if (file.exists()) {
                        try {
                            println("📸 [LogSyncWorker] Preparing photo upload…")
                            println("   Local file path: ${file.absolutePath}")
                            println("   File exists? ${file.exists()} | Size = ${file.length()} bytes")

                            // 🔹 Path format: logs/{employeeID}/{YYYY-MM}/{timestamp}.jpg
                            val date = Date(log.timestamp)
                            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                            val monthPath = sdf.format(date)

                            val storagePath =
                                "logs/${log.employeeID}/$monthPath/${log.timestamp}.jpg"
                            println("   Target Firebase path: $storagePath")

                            val storageRef = storage.reference.child(storagePath)
                            val fileUri = Uri.fromFile(file)
                            println("   Uploading from Uri: $fileUri")

                            // ✅ Always fresh upload (avoid resumable session 404)
                            storageRef.putFile(fileUri).await()

                            photoURL = storageRef.downloadUrl.await().toString()
                            println("✅ Upload successful → $photoURL")

                            // ✅ Delete local file after successful upload
                            if (file.delete()) {
                                println("🗑️ Deleted local file: ${file.absolutePath}")
                            } else {
                                println("⚠️ Could not delete local file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            println("❌ [LogSyncWorker] Photo upload failed for ${file.absolutePath}")
                            e.printStackTrace()
                            continue // skip this log, retry later
                        }
                    } else {
                        // 🚮 Purge broken/missing file logs
                        println("⚠️ [LogSyncWorker] Missing file → purging log: ${log.localPhotoPath}")
                        dao.updateLog(log.copy(synced = true)) // mark as synced so ignored
                        continue
                    }
                }

                // 2️⃣ Prepare Firestore log data
                val data = hashMapOf(
                    "employeeID" to log.employeeID,
                    "action" to log.action,
                    // ✅ Store as Firestore Timestamp for proper query/sorting
                    "timestamp" to Date(log.timestamp)
                )

                if (photoURL != null) {
                    if (log.action == "clock_in") {
                        data["photoIn"] = photoURL
                    } else if (log.action == "clock_out") {
                        data["photoOut"] = photoURL
                    }
                }

                // 3️⃣ Upload log to Firestore
                try {
                    db.collection("logs").add(data).await()
                    println("☁️ Firestore log saved for ${log.employeeID} (${log.action})")

                    // Mark as synced in local DB
                    dao.updateLog(log.copy(synced = true))
                    println("✅ Marked log as synced → ${log.employeeID}, ts=${log.timestamp}")
                } catch (e: Exception) {
                    println("❌ [LogSyncWorker] Firestore write failed for log ${log.employeeID}")
                    e.printStackTrace()
                    // Firestore failed → leave unsynced for retry
                }
            }

            println("🎉 [LogSyncWorker] Sync cycle finished")
            return Result.success()

        } catch (e: Exception) {
            println("💥 [LogSyncWorker] Fatal error in sync loop")
            e.printStackTrace()
            return Result.retry()
        }
    }
}
