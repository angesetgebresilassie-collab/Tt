package com.angesetgebresilassie.tt

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); buildUi() }
    override fun onResume() { super.onResume(); if (::status.isInitialized) updateStatus() }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,48,32,32); setBackgroundColor(Color.rgb(246,247,250)) }
        root.addView(TextView(this).apply { text="Tt"; textSize=42f; setTextColor(Color.rgb(25,25,28)); gravity=Gravity.CENTER })
        root.addView(TextView(this).apply { text="Floating workspace"; textSize=22f; setTextColor(Color.DKGRAY); gravity=Gravity.CENTER; setPadding(0,0,0,12) })
        status=TextView(this).apply { textSize=15f; gravity=Gravity.CENTER; setPadding(16,16,16,24) }; root.addView(status)
        root.addView(Button(this).apply {
            text="Enable floating controls"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity))
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                else {
                    val i=Intent(this@MainActivity,FloatingOverlayService::class.java)
                    if (android.os.Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i)
                }
            }
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(Button(this).apply {
            text="Stop floating controls"
            setOnClickListener { stopService(Intent(this@MainActivity,FloatingOverlayService::class.java)); updateStatus() }
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply {
            text="Gesture: swipe diagonally ↙ from the top-right handle.\n\nTt requests freeform bounds where supported and safely falls back to normal launching where the device does not provide freeform windows."
            textSize=14f; setTextColor(Color.GRAY); setPadding(8,28,8,8)
        })
        setContentView(root); updateStatus()
    }
    private fun updateStatus() {
        status.text=if(Settings.canDrawOverlays(this)) "Overlay permission: enabled\nTt is ready to start." else "Overlay permission: required\nGrant Display over other apps to use floating controls."
    }
}
