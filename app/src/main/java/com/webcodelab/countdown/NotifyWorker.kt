package com.webcodelab.countdown

import android.app.*
import android.content.Context
import androidx.work.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class NotifyWorker(c: Context, p: WorkerParameters) : Worker(c, p) {
    override fun doWork(): Result {
        return try {
            val url = URL("https://webcodelab.in/db.php?action=latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            val text = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            conn.disconnect()

            val json = JSONObject(text)
            val id = json.optInt("id", 0)
            val title = json.optString("title", "")
            val message = json.optString("message", "")

            val prefs = applicationContext.getSharedPreferences("cd", Context.MODE_PRIVATE)
            val lastId = prefs.getInt("lastNotifyId", 0)

            if (id > 0 && id != lastId && title.isNotBlank()) {
                showNotification(title, message)
                prefs.edit().putInt("lastNotifyId", id).apply()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("wcl_broadcast", "WebCodelab Updates", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            android.content.Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(applicationContext, "wcl_broadcast")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(2, n)
    }

    companion object {
        fun schedule(c: Context) {
            val req = PeriodicWorkRequestBuilder<NotifyWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork(
                "wcl_notify_check", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
