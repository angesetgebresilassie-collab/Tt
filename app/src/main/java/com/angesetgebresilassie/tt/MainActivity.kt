package com.angesetgebresilassie.tt

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
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
        toggle=TextView(this).apply { textSize=17f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(20,18,20,18); isClickable=true; elevation=10f; setOnClickListener { toggleService() } }
        root.addView(toggle, LinearLayout.LayoutParams(-1,-2).apply { topMargin=12 })
        root.addView(TextView(this).apply { text="Freeform setup"; textSize=15f; setTextColor(Color.rgb(55,55,60)); setPadding(4,24,4,4) })
        root.addView(TextView(this).apply {
            text="Tt checks Android freeform support. If your phone exposes freeform but it is disabled, open Developer options and enable the available freeform/resizable activity settings, then restart Tt."
            textSize=14f; setTextColor(Color.GRAY); setPadding(8,8,8,8)
            setOnClickListener { runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) } }
        })
        root.addView(TextView(this).apply { text="How it works"; textSize=15f; setTextColor(Color.rgb(55,55,60)); setPadding(4,24,4,4) })
        root.addView(TextView(this).apply { text="Gesture: tap the floating pill or swipe diagonally ↙ from the top-right corner.\n\nThe floating launcher has macOS-style controls: red closes it, yellow switches to an icon-only launcher, and green restores the full app list. Drag its title bar to move it and use the lower-right grip to resize it.\n\nFreeform mode: Tt first creates a small invisible bootstrap activity, then launches the selected app with requested window bounds. If the device refuses freeform, Tt falls back to normal Android app launching."; textSize=14f; setTextColor(Color.GRAY); setPadding(8,28,8,8) })
        setContentView(root); updateStatus()
    }

    private fun toggleService() {
        if (!Settings.canDrawOverlays(this)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return }
        val intent=Intent(this,FloatingOverlayService::class.java)
        if (toggle.tag==true) { stopService(intent); toggle.tag=false } else { if (Build.VERSION.SDK_INT>=26) startForegroundService(intent) else startService(intent); toggle.tag=true }
        updateStatus()
    }

    private fun updateStatus() {
        val enabled=Settings.canDrawOverlays(this)
        val freeform=hasFreeformSupport()
        val state=if(toggle.tag==true) "Floating launcher: ON" else "Floating launcher: OFF"
        status.text=if(enabled) "Overlay permission: enabled\n$state\nFreeform support: "+if(freeform) "detected" else "not detected" else "Overlay permission: required\nGrant Display over other apps to use floating controls."
        toggle.text=if(toggle.tag==true) "●  Floating Launcher  •  ON" else "○  Floating Launcher  •  OFF"
        toggle.background=GradientDrawable().apply { setColor(if(toggle.tag==true) Color.rgb(35,125,78) else Color.rgb(35,36,40)); cornerRadius=28f }
    }

    private fun hasFreeformSupport(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(contentResolver,"enable_freeform_support",0)==1 ||
            (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 && Settings.Global.getInt(contentResolver,"force_resizable_activities",0)==1)
    }
}
