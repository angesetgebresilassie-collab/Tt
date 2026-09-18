package com.angesetgebresilassie.tt

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View?=null
    private var panel: View?=null
    private var downX=0f; private var downY=0f; private var downTime=0L
    private val d get()=resources.displayMetrics.density

    override fun onCreate() {
        super.onCreate()
        if(!Settings.canDrawOverlays(this)){ stopSelf(); return }
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        showHandle()
    }

    private fun startForegroundNotification() {
        val id="tt_overlay"; val nm=getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel(id,"Tt Floating Controls",NotificationManager.IMPORTANCE_LOW))
        val n=NotificationCompat.Builder(this,id).setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Tt floating controls").setContentText("Tap or swipe ↙ from the top-right pill").setOngoing(true).build()
        startForeground(1001,n)
    }

    private fun showHandle() {
        val v=TextView(this).apply {
            text="↙"; textSize=25f; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            typeface=Typeface.DEFAULT_BOLD
            background=rounded(Color.rgb(28,29,32),24)
            elevation=dp(8).toFloat()
            contentDescription="Open floating apps"
            setOnTouchListener { _,e ->
                when(e.actionMasked){
                    MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;downTime=System.currentTimeMillis();alpha=.72f;true}
                    MotionEvent.ACTION_UP->{
                        val dx=e.rawX-downX; val dy=e.rawY-downY
                        alpha=1f
                        val quickTap=abs(dx)<dp(24)&&abs(dy)<dp(24)&&(System.currentTimeMillis()-downTime)<500
                        val diagonal=dx < -dp(32) && dy > dp(32) && abs(dx) > abs(dy)*0.35f
                        if(diagonal || quickTap) showLauncher()
                        true
                    }
                    else->true
                }
            }
        }
        val lp=WindowManager.LayoutParams(dp(72),dp(48),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.END; x=dp(10); y=dp(28)
        }
        runCatching { wm.addView(v,lp); handle=v }.onFailure { stopSelf() }
    }

    private fun showLauncher() {
        panel?.let{runCatching{wm.removeView(it)}}
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(16));background=rounded(Color.rgb(248,248,250),26)}
        box.addView(TextView(this).apply{text="Floating Apps";textSize=22f;setTextColor(Color.rgb(25,25,28));setPadding(0,0,0,dp(6))})
        box.addView(TextView(this).apply{text="Tap the pill again or swipe ↙ to reopen";textSize=12f;setTextColor(Color.GRAY);setPadding(0,0,0,dp(8))})
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
            .filter{it.activityInfo.packageName!=packageName}.sortedBy{it.loadLabel(packageManager).toString().lowercase()}
        apps.take(80).forEach{info->
            val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(5),dp(8),dp(5));background=rounded(Color.WHITE,16)}
            row.addView(ImageView(this).apply{setImageDrawable(info.loadIcon(packageManager))},LinearLayout.LayoutParams(dp(42),dp(42)))
            row.addView(TextView(this).apply{text=info.loadLabel(packageManager);textSize=16f;setTextColor(Color.DKGRAY);setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(54),1f))
            row.setOnClickListener{launchApp(info.activityInfo.packageName);closePanel()}
            list.addView(row,LinearLayout.LayoutParams(-1,dp(60)).apply{bottomMargin=dp(5)})
        }
        val scroll=ScrollView(this);scroll.addView(list);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val lp=WindowManager.LayoutParams(dp(340),dp(540),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=dp(8);y=dp(108)}
        runCatching{wm.addView(box,lp);panel=box}
    }

    private fun hasFreeformSupport():Boolean {
        val pm=packageManager
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return pm.hasSystemFeature("android.software.freeform_window_management") ||
            Settings.Global.getInt(contentResolver,"enable_freeform_support",0)==1 ||
            Settings.Global.getInt(contentResolver,"force_resizable_activities",0)==1
    }

    private fun launchApp(pkg:String){
        val i=packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        try{
            if(!hasFreeformSupport()) { startActivity(i); return }
            val w=resources.displayMetrics.widthPixels; val h=resources.displayMetrics.heightPixels
            val r=Rect((w*.08f).toInt(),(h*.12f).toInt(),(w*.92f).toInt(),(h*.86f).toInt())
            val o=ActivityOptions.makeBasic()
            o.launchBounds=r
            startActivity(i,o.toBundle())
        }catch(_:Throwable){runCatching{startActivity(i)}}
    }

    private fun closePanel(){panel?.let{runCatching{wm.removeView(it)}};panel=null}
    private fun rounded(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun dp(v:Int)=(v*d).toInt()
    override fun onDestroy(){closePanel();handle?.let{runCatching{wm.removeView(it)}};handle=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
