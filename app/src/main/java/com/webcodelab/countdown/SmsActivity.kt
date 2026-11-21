package com.webcodelab.countdown

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class SmsActivity : Activity() {
    data class Sms(val from: String, val body: String, val date: Long)

    private val list = ArrayList<Sms>()
    private lateinit var count: TextView
    private lateinit var info: TextView
    private lateinit var adapter: BaseAdapter

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()

    private fun pill(color: String, r: Int, stroke: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color))
        cornerRadius = dp(r).toFloat()
        if (stroke != null) setStroke(dp(1), Color.parseColor(stroke))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#1A1040")
        window.navigationBarColor = Color.parseColor("#0F2A5E")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(12))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1A1040"), Color.parseColor("#2B1B63"), Color.parseColor("#0F2A5E"))
            )
        }

        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(dp(8), dp(4), dp(16), dp(4))
            setOnClickListener { finish() }
        }
        val title = TextView(this).apply {
            text = "📩 SMS Inbox"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(title)
        }
        root.addView(top)

        count = TextView(this).apply {
            text = "Total SMS: ..."
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0B1A2A"))
            background = pill("#00E5A0", 50)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        root.addView(count, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12); bottomMargin = dp(12) })

        info = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#B3FFFFFF"))
        }
        root.addView(info)

        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        adapter = object : BaseAdapter() {
            override fun getCount() = list.size
            override fun getItem(p: Int) = list[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, v: View?, parent: ViewGroup?): View {
                val s = list[p]
                val row = LinearLayout(this@SmsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = pill("#1AFFFFFF", 16, "#33FFFFFF")
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                }
                row.addView(TextView(this@SmsActivity).apply {
                    text = s.from
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#00E5A0"))
                })
                row.addView(TextView(this@SmsActivity).apply {
                    text = s.body
                    textSize = 14f
                    setTextColor(Color.WHITE)
                    setPadding(0, dp(6), 0, dp(6))
                })
                row.addView(TextView(this@SmsActivity).apply {
                    text = fmt.format(Date(s.date))
                    textSize = 11f
                    setTextColor(Color.parseColor("#80FFFFFF"))
                })
                return row
            }
        }

        val lv = ListView(this).apply {
            divider = ColorDrawable(Color.TRANSPARENT)
            dividerHeight = dp(8)
            adapter = this@SmsActivity.adapter
        }
        root.addView(lv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        if (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            load()
        } else {
            info.text = "SMS dikhane ke liye permission allow karo."
            requestPermissions(arrayOf(Manifest.permission.READ_SMS), 2)
        }
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) {
            info.text = ""
            load()
        } else {
            count.text = "Total SMS: 0"
            info.text = "Permission nahi mili. Settings → Apps → WCL → Permissions mein SMS allow karo."
        }
    }

    private fun load() {
        Thread {
            val tmp = ArrayList<Sms>()
            try {
                contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("address", "body", "date"),
                    null, null, "date DESC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        tmp.add(Sms(c.getString(0) ?: "Unknown", c.getString(1) ?: "", c.getLong(2)))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { info.text = "SMS nahi padh paye: " + e.message }
            }
            runOnUiThread {
                list.clear()
                list.addAll(tmp)
                adapter.notifyDataSetChanged()
                count.text = "Total SMS: " + tmp.size
            }
        }.start()
    }
}
