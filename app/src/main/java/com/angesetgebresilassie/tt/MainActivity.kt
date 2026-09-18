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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            setPadding(48,48,48,48)
            setBackgroundColor(Color.rgb(245,245,247))
        }
        root.addView(TextView(this).apply {
            text="Tt Floating Launcher"
            textSize=28f
            setTextColor(Color.BLACK)
            gravity=Gravity.CENTER
        }, LinearLayout.LayoutParams(-1,-2))
        root.addView(TextView(this).apply {
            text="Floating windows • Mac-style controls • shrink to icon • top-right diagonal gesture"
            textSize=16f
            gravity=Gravity.CENTER
            setPadding(0,24,0,32)
        }, LinearLayout.LayoutParams(-1,-2))
        val permission=Button(this).apply {
            text="Enable Floating Window Controls"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else startService(Intent(this@MainActivity, FloatingOverlayService::class.java))
            }
        }
        root.addView(permission, LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }
}
