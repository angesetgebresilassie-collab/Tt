package com.angesetgebresilassie.tt

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import org.lsposed.hiddenapibypass.HiddenApiBypass
import android.view.Window
import android.view.WindowManager

/**
 * Small Taskbar-inspired freeform bootstrap activity.
 *
 * The system may place this activity in a freeform task when the device's
 * freeform window support is enabled. The real application is then launched
 * from this activity so it can inherit the freeform workspace.
 */
class FreeformBootstrapActivity : Activity() {
    companion object { const val EXTRA_PACKAGE = "tt.extra.FREEFORM_PACKAGE" }
    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.setDimAmount(0f)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        Handler(Looper.getMainLooper()).postDelayed({ launchTarget() }, 120)
    }

    override fun onResume() {
        super.onResume()
        if (!launched) Handler(Looper.getMainLooper()).postDelayed({ launchTarget() }, 80)
    }

    private fun launchTarget() {
        if (launched || isFinishing) return
        val pkg=intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val launch=packageManager.getLaunchIntentForPackage(pkg) ?: run { finish(); return }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)

        val w=resources.displayMetrics.widthPixels
        val h=resources.displayMetrics.heightPixels
        val bounds=android.graphics.Rect((w*.08f).toInt(),(h*.12f).toInt(),(w*.92f).toInt(),(h*.86f).toInt())
        launched=true

        try {
            val options=ActivityOptions.makeBasic().apply {
                launchBounds=bounds
                runCatching {
                    HiddenApiBypass.invoke(
                        ActivityOptions::class.java,
                        this,
                        "setLaunchWindowingMode",
                        5
                    )
                }
            }
            startActivity(launch,options.toBundle())
        } catch(_:Throwable) {
            runCatching { startActivity(launch) }
        }
        Handler(Looper.getMainLooper()).postDelayed({ finish() },180)
    }
}
