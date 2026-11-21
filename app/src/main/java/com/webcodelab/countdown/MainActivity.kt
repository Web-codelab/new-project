package com.webcodelab.countdown

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private var timer: CountDownTimer? = null
    private lateinit var tv: TextView
    private lateinit var status: TextView
    private lateinit var etMsg: EditText
    private lateinit var ring: Ring

    private val vals = intArrayOf(0, 0, 0, 0)          // day, hour, min, sec
    private val maxs = intArrayOf(1, 23, 59, 59)
    private val valTvs = arrayOfNulls<TextView>(4)

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun pill(color: String, r: Int, stroke: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(r).toFloat()
        if (stroke != null) setStroke(dp(1), Color.parseColor(stroke))
    }

    inner class Ring(c: Context) : View(c) {
        var progress = 0f
            set(v) { field = v; invalidate() }
        private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(12).toFloat()
            color = Color.parseColor("#22FFFFFF")
        }
        private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(12).toFloat()
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#00E5A0")
        }
        override fun onDraw(cv: Canvas) {
            val pad = dp(12).toFloat()
            val r = RectF(pad, pad, width - pad, height - pad)
            cv.drawArc(r, 0f, 360f, false, track)
            cv.drawArc(r, -90f, 360f * progress, false, arc)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        window.statusBarColor = Color.parseColor("#1A1040")
        window.navigationBarColor = Color.parseColor("#0F2A5E")

        val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

        val scroll = ScrollView(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1A1040"), Color.parseColor("#2B1B63"), Color.parseColor("#0F2A5E"))
            )
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(36), dp(20), dp(28))
        }
        scroll.addView(root, ViewGroup.LayoutParams(MATCH, WRAP))

        // Title
        root.addView(TextView(this).apply {
            text = "⏳ COUNTDOWN"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.25f
            gravity = Gravity.CENTER
        })

        // Ring + time
        ring = Ring(this)
        tv = TextView(this).apply {
            text = "00:00:00"
            textSize = 34f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Ready"
            textSize = 14f
            setTextColor(Color.parseColor("#B3FFFFFF"))
            gravity = Gravity.CENTER
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(tv)
            addView(status)
        }
        val frame = FrameLayout(this)
        frame.addView(ring, FrameLayout.LayoutParams(dp(260), dp(260)))
        frame.addView(center, FrameLayout.LayoutParams(dp(260), dp(260)))
        root.addView(frame, LinearLayout.LayoutParams(dp(260), dp(260)).apply {
            topMargin = dp(24); bottomMargin = dp(20)
        })

        // Quick chips
        val chipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        val presets = listOf(
            "1s" to 1, "10s" to 10, "30s" to 30, "1m" to 60, "5m" to 300, "10m" to 600,
            "30m" to 1800, "1h" to 3600, "6h" to 21600, "12h" to 43200, "1 day" to 86400
        )
        presets.forEach { (name, sec) ->
            chipRow.addView(TextView(this).apply {
                text = name
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                background = pill("#26FFFFFF", 50, "#33FFFFFF")
                setOnClickListener { setPicker(sec) }
            }, LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(8) })
        }
        val hs = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(chipRow)
        }
        root.addView(hs, LinearLayout.LayoutParams(MATCH, WRAP))

        // Picker card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = pill("#1AFFFFFF", 22, "#33FFFFFF")
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        val labels = listOf("Days", "Hours", "Min", "Sec")
        for (i in 0..3) card.addView(stepper(i, labels[i]), LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(card, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        // Message
        etMsg = EditText(this).apply {
            hint = "💬 Notification message (optional)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#80FFFFFF"))
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(true)
            background = pill("#1AFFFFFF", 16, "#33FFFFFF")
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        root.addView(etMsg, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        // Buttons
        val cancel = Button(this).apply {
            text = "✕  CANCEL"
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
            background = pill("#00000000", 28, "#66FFFFFF")
            setOnClickListener { cancelCountdown() }
        }
        val start = Button(this).apply {
            text = "▶  START"
            isAllCaps = false
            setTextColor(Color.parseColor("#0B1A2A"))
            typeface = Typeface.DEFAULT_BOLD
            stateListAnimator = null
            background = pill("#00E5A0", 28)
            setOnClickListener { startCountdown() }
        }
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(cancel, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(10) })
        btnRow.addView(start, LinearLayout.LayoutParams(0, dp(54), 2f))
        root.addView(btnRow, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(20) })

        setContentView(scroll)

        val p = getSharedPreferences("cd", MODE_PRIVATE)
        val end = p.getLong("end", 0)
        if (end > System.currentTimeMillis()) runTimer(end, p.getLong("total", 1))
    }

    private fun arrow(t: String, f: () -> Unit) = TextView(this).apply {
        text = t
        textSize = 18f
        setTextColor(Color.parseColor("#00E5A0"))
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        setOnClickListener { f() }
    }

    private fun stepper(i: Int, label: String): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val v = TextView(this).apply {
            text = "00"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        valTvs[i] = v
        val lbl = TextView(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.parseColor("#99FFFFFF"))
            gravity = Gravity.CENTER
        }
        col.addView(arrow("▲") { change(i, 1) })
        col.addView(v)
        col.addView(arrow("▼") { change(i, -1) })
        col.addView(lbl)
        return col
    }

    private fun change(i: Int, d: Int) {
        val n = maxs[i] + 1
        vals[i] = (vals[i] + d + n) % n
        valTvs[i]?.text = String.format("%02d", vals[i])
    }

    private fun setPicker(totalSec: Int) {
        vals[0] = totalSec / 86400
        vals[1] = (totalSec % 86400) / 3600
        vals[2] = (totalSec % 3600) / 60
        vals[3] = totalSec % 60
        for (i in 0..3) valTvs[i]?.text = String.format("%02d", vals[i])
    }

    private fun fmt(s: Long): String {
        val d = s / 86400
        val h = (s % 86400) / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (d > 0) String.format("%dd %02d:%02d:%02d", d, h, m, sec)
        else String.format("%02d:%02d:%02d", h, m, sec)
    }

    private fun alarmIntent(msg: String): PendingIntent {
        val i = Intent(this, AlarmReceiver::class.java).putExtra("msg", msg)
        return PendingIntent.getBroadcast(
            this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startCountdown() {
        var total = ((vals[0] * 24L + vals[1]) * 60 + vals[2]) * 60 + vals[3]
        if (total <= 0) {
            toast("Time select karo (kam se kam 1 second)")
            return
        }
        if (total > 86400) {
            total = 86400
            setPicker(86400)
            toast("Max 1 day")
        }
        val msg = etMsg.text.toString().ifBlank { "Time ho gaya!" }
        val end = System.currentTimeMillis() + total * 1000
        try {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            val pi = alarmIntent(msg)
            val show = PendingIntent.getActivity(
                this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            try {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(end, show), pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, end, pi)
            }
            getSharedPreferences("cd", MODE_PRIVATE).edit()
                .putLong("end", end).putLong("total", total).apply()
            runTimer(end, total)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun runTimer(end: Long, total: Long) {
        timer?.cancel()
        status.text = "⏱ Chal raha hai"
        timer = object : CountDownTimer(end - System.currentTimeMillis(), 100) {
            override fun onTick(ms: Long) {
                tv.text = fmt((ms + 999) / 1000)
                ring.progress = ms.toFloat() / (total * 1000f)
            }
            override fun onFinish() {
                tv.text = "00:00:00"
                ring.progress = 0f
                status.text = "✅ Time ho gaya!"
            }
        }.start()
    }

    private fun cancelCountdown() {
        timer?.cancel()
        tv.text = "00:00:00"
        ring.progress = 0f
        status.text = "Ready"
        (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(alarmIntent(""))
        getSharedPreferences("cd", MODE_PRIVATE).edit().remove("end").apply()
    }
}
