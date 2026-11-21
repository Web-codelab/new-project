package com.webcodelab.countdown

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private var timer: CountDownTimer? = null
    private lateinit var tv: TextView
    private lateinit var etMin: EditText
    private lateinit var etMsg: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        tv = TextView(this).apply { textSize = 48f; gravity = Gravity.CENTER; text = "00:00:00" }
        etMin = EditText(this).apply {
            hint = "Kitne minute?"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        etMsg = EditText(this).apply { hint = "Notification message" }
        val start = Button(this).apply { text = "Start"; setOnClickListener { startCountdown() } }
        val cancel = Button(this).apply { text = "Cancel"; setOnClickListener { cancelCountdown() } }
        listOf(tv, etMin, etMsg, start, cancel).forEach { root.addView(it) }
        setContentView(root)

        val end = getSharedPreferences("cd", MODE_PRIVATE).getLong("end", 0)
        if (end > System.currentTimeMillis()) run(end)
    }

    private fun alarmIntent(msg: String): PendingIntent {
        val i = Intent(this, AlarmReceiver::class.java).putExtra("msg", msg)
        return PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startCountdown() {
        val min = etMin.text.toString().toLongOrNull() ?: return
        val msg = etMsg.text.toString().ifBlank { "Time ho gaya!" }
        val end = System.currentTimeMillis() + min * 60_000
        getSharedPreferences("cd", MODE_PRIVATE).edit().putLong("end", end).apply()
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        val pi = alarmIntent(msg)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(end, pi), pi)
        run(end)
    }

    private fun run(end: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(end - System.currentTimeMillis(), 1000) {
            override fun onTick(ms: Long) {
                val s = ms / 1000
                tv.text = String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
            }
            override fun onFinish() { tv.text = "00:00:00" }
        }.start()
    }

    private fun cancelCountdown() {
        timer?.cancel()
        tv.text = "00:00:00"
        (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(alarmIntent(""))
        getSharedPreferences("cd", MODE_PRIVATE).edit().remove("end").apply()
    }
}
