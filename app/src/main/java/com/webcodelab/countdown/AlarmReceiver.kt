package com.webcodelab.countdown

import android.app.*
import android.content.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("cd", "Countdown", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val msg = i.getStringExtra("msg") ?: "Time ho gaya!"
        val n = Notification.Builder(c, "cd")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Countdown khatam")
            .setContentText(msg)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(1, n)
        c.getSharedPreferences("cd", Context.MODE_PRIVATE).edit().remove("end").apply()
    }
}
