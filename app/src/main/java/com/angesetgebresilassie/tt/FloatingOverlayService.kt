package com.angesetgebresilassie.tt

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.*

class FloatingOverlayService: Service() {
    private lateinit var wm: WindowManager
    private lateinit var handle: View
    override fun onCreate() {
        super.onCreate()
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        handle=TextView(this).apply {
            text="↙"
            textSize=22f
            gravity=Gravity.CENTER
            setTextColor(Color.WHITE)
            background=GradientDrawable().apply {
                cornerRadius=60f
                setColor(Color.rgb(35,35,38))
            }
            setOnClickListener { showLauncher() }
        }
        val p=WindowManager.LayoutParams(110,110,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)
        p.gravity=Gravity.TOP or Gravity.END
        p.x=18; p.y=18
        wm.addView(handle,p)
    }
    private fun showLauncher() {
        val apps=packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
            .sortedBy { it.loadLabel(packageManager).toString() }
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(24,24,24,24)
            background=GradientDrawable().apply { cornerRadius=36f; setColor(Color.WHITE) }
        }
        val title=TextView(this).apply { text="Floating Apps"; textSize=24f; setTextColor(Color.BLACK); setPadding(0,0,0,16) }
        box.addView(title)
        apps.take(40).forEach { ri ->
            val b=Button(this).apply {
                text=ri.loadLabel(packageManager)
                setOnClickListener {
                    val i=packageManager.getLaunchIntentForPackage(ri.activityInfo.packageName)
                    if(i!=null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        try {
                            val opts=ActivityOptions.makeBasic()
                            opts.launchBounds=android.graphics.Rect(80,180,980,1500)
                            startActivity(i,opts.toBundle())
                        } catch(_:Throwable) { startActivity(i) }
                    }
                    wm.removeView(box)
                }
            }
            box.addView(b,LinearLayout.LayoutParams(-1,60))
        }
        val lp=WindowManager.LayoutParams(900,1200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT)
        lp.gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y=120
        wm.addView(box,lp)
    }
    override fun onDestroy(){ if(::handle.isInitialized) wm.removeView(handle); super.onDestroy() }
    override fun onBind(intent:Intent?):IBinder?=null
}
