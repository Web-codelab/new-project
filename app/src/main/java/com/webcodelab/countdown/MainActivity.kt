package com.webcodelab.countdown

import android.app.*
import android.content.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val msg = i.getStringExtra("msg") ?: "Aapka time poora ho gaya! 🎯"
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("cd", "Countdown", NotificationManager.IMPORTANCE_HIGH)
        )
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(c, "cd")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ WCL • Time Complete")
            .setContentText(msg)
            .setStyle(Notification.BigTextStyle().bigText(msg))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(1, n)
        c.getSharedPreferences("cd", Context.MODE_PRIVATE).edit()
            .remove("end")
            .putBoolean("done", true)
            .putString("doneMsg", msg)
            .apply()
    }
}
