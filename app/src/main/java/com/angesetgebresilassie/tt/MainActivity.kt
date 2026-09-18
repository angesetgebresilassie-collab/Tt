package com.angesetgebresilassie.tt

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggle: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); if (::status.isInitialized) updateStatus() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32); setBackgroundColor(Color.rgb(246,247,250)) }
        root.addView(TextView(this).apply { text="Tt"; textSize=42f; setTextColor(Color.rgb(25,25,28)); gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="Floating workspace"; textSize=22f; setTextColor(Color.DKGRAY); gravity=Gravity.CENTER; setPadding(0,0,0,12) })
        status=TextView(this).apply { textSize=15f; gravity=Gravity.CENTER; setPadding(16,16,16,24) }; root.addView(status)
        toggle=TextView(this).apply {
            textSize=17f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(20,18,20,18)
            isClickable=true; elevation=10f
            setOnClickListener { toggleService() }
        }
        root.addView(toggle, LinearLayout.LayoutParams(-1,-2).apply { topMargin=12 })
        root.addView(TextView(this).apply { text="How it works"; textSize=15f; setTextColor(Color.rgb(55,55,60)); setPadding(4,24,4,4) })
        root.addView(TextView(this).apply {
            text="Gesture: tap the floating pill or swipe diagonally ↙ from the top-right corner.\n\nFreeform note: Android does not let ordinary apps force arbitrary apps into freeform on every phone. Tt requests freeform bounds when the device supports them; otherwise it launches the app normally."
            textSize=14f; setTextColor(Color.GRAY); setPadding(8,28,8,8)
        })
        setContentView(root); updateStatus()
    }

    private fun toggleService() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val intent=Intent(this,FloatingOverlayService::class.java)
        if (toggle.tag==true) { stopService(intent); toggle.tag=false }
        else {
            if (android.os.Build.VERSION.SDK_INT>=26) startForegroundService(intent) else startService(intent)
            toggle.tag=true
        }
        updateStatus()
    }

    private fun updateStatus() {
        val enabled=Settings.canDrawOverlays(this)
        val state=if(toggle.tag==true) "Floating launcher: ON" else "Floating launcher: OFF"
        status.text=if(enabled) "Overlay permission: enabled\n"+state else "Overlay permission: required\nGrant Display over other apps to use floating controls."
        if (::toggle.isInitialized) {
            toggle.text=if(toggle.tag==true) "●  Floating Launcher  •  ON" else "○  Floating Launcher  •  OFF"
            toggle.background=GradientDrawable().apply { setColor(if(toggle.tag==true) Color.rgb(35,125,78) else Color.rgb(35,36,40)); cornerRadius=28f }
        }
    }
}
