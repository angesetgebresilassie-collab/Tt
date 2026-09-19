package com.angesetgebresilassie.tt

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
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
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View?=null
    private var panel: View?=null
    private var panelParams: WindowManager.LayoutParams?=null
    private var compactLauncher=false
    private var panelWidth=0
    private var panelHeight=0
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
            text="↙"; textSize=18f; gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            typeface=Typeface.DEFAULT_BOLD
            background=rounded(Color.rgb(28,29,32),16)
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
        val lp=WindowManager.LayoutParams(dp(48),dp(34),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.END; x=dp(6); y=dp(20)
        }
        runCatching { wm.addView(v,lp); handle=v }.onFailure { stopSelf() }
    }

    private fun showLauncher() {
        closePanel()
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(14),dp(12),dp(14),dp(14))
            background=rounded(Color.rgb(249,250,252),36)
            elevation=dp(18).toFloat()
        }
        val titleBar=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(2),0,dp(2),dp(8)) }
        titleBar.addView(trafficLight("×", Color.rgb(255,95,86), "Close launcher") { closePanel() })
        titleBar.addView(trafficLight("–", Color.rgb(255,189,46), "Show icons only") {
            compactLauncher=true
            panelWidth=compactPanelWidth(); panelHeight=compactPanelHeight()
            showLauncher()
        }, trafficLightLayout())
        titleBar.addView(trafficLight("+", Color.rgb(39,201,63), "Expand launcher") {
            compactLauncher=false
            panelWidth=expandedPanelWidth(); panelHeight=expandedPanelHeight()
            showLauncher()
        }, trafficLightLayout())
        titleBar.addView(TextView(this).apply {
            text=if(compactLauncher) "Apps · icons" else "Floating Apps"
            textSize=compactLauncher.let { if(it) 15f else 20f }
            typeface=Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25,25,28))
            setPadding(dp(10),0,0,0)
        }, LinearLayout.LayoutParams(0,dp(28),1f))
        box.addView(titleBar)
        installDrag(titleBar)

        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
            .filter{it.activityInfo.packageName!=packageName}.sortedBy{it.loadLabel(packageManager).toString().lowercase()}
        if(compactLauncher) {
            val grid=GridLayout(this).apply { columnCount=4; useDefaultMargins=true }
            apps.take(24).forEach { info ->
                val icon=ImageView(this).apply {
                    setImageDrawable(info.loadIcon(packageManager))
                    contentDescription="Launch ${info.loadLabel(packageManager)}"
                    background=rounded(Color.WHITE,18)
                    setPadding(dp(8),dp(8),dp(8),dp(8))
                    setOnClickListener { launchApp(info.activityInfo.packageName); closePanel() }
                }
                grid.addView(icon, ViewGroup.LayoutParams(dp(46),dp(46)))
            }
            box.addView(grid)
        } else {
            box.addView(TextView(this).apply{text="Drag to move · use the lower-right grip to resize";textSize=12f;setTextColor(Color.GRAY);setPadding(dp(6),0,0,dp(10))})
            val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
            apps.take(80).forEach{info->
                val row=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(6),dp(10),dp(6));background=rounded(Color.WHITE,20);elevation=dp(1).toFloat()}
                row.addView(ImageView(this).apply{setImageDrawable(info.loadIcon(packageManager))},LinearLayout.LayoutParams(dp(42),dp(42)))
                row.addView(TextView(this).apply{text=info.loadLabel(packageManager);textSize=16f;setTextColor(Color.DKGRAY);setPadding(dp(12),0,0,0)},LinearLayout.LayoutParams(0,dp(54),1f))
                row.setOnClickListener{launchApp(info.activityInfo.packageName);closePanel()}
                list.addView(row,LinearLayout.LayoutParams(-1,dp(60)).apply{bottomMargin=dp(5)})
            }
            val scroll=ScrollView(this);scroll.addView(list);box.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        }
        val resize=TextView(this).apply { text="◢"; textSize=18f; gravity=Gravity.END; setTextColor(Color.GRAY); contentDescription="Resize launcher" }
        box.addView(resize,LinearLayout.LayoutParams(-1,dp(24)))
        val lp=WindowManager.LayoutParams(if(panelWidth>0) panelWidth else expandedPanelWidth(),if(panelHeight>0) panelHeight else expandedPanelHeight(),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=dp(8);y=dp(108)}
        panelWidth=lp.width; panelHeight=lp.height
        panelParams=lp
        installResize(resize)
        runCatching{wm.addView(box,lp);panel=box}.onFailure { panelParams=null }
    }

    private fun trafficLight(symbol:String, color:Int, description:String, action:()->Unit)=TextView(this).apply {
        text=symbol; textSize=13f; gravity=Gravity.CENTER; setTextColor(Color.rgb(60,60,60)); typeface=Typeface.DEFAULT_BOLD
        contentDescription=description; background=rounded(color,20); setOnClickListener { action() }
        layoutParams=LinearLayout.LayoutParams(dp(20),dp(20))
    }

    private fun trafficLightLayout()=LinearLayout.LayoutParams(dp(20),dp(20)).apply { leftMargin=dp(6) }

    private fun expandedPanelWidth()=min(dp(380),resources.displayMetrics.widthPixels-dp(24))
    private fun expandedPanelHeight()=min(dp(600),(resources.displayMetrics.heightPixels*.76f).toInt())
    private fun compactPanelWidth()=min(dp(250),resources.displayMetrics.widthPixels-dp(24))
    private fun compactPanelHeight()=min(dp(350),(resources.displayMetrics.heightPixels*.58f).toInt())

    private fun installDrag(view:View) { var x=0f; var y=0f
        view.setOnTouchListener { _, event ->
            val lp=panelParams ?: return@setOnTouchListener false
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { x=event.rawX; y=event.rawY; true }
                MotionEvent.ACTION_MOVE -> { lp.x=max(0,lp.x+(x-event.rawX).toInt()); lp.y=max(0,lp.y+(event.rawY-y).toInt()); x=event.rawX; y=event.rawY; panel?.let { runCatching { wm.updateViewLayout(it,lp) } }; true }
                else -> true
            }
        }
    }

    private fun installResize(view:View) { var x=0f; var y=0f; var width=0; var height=0
        view.setOnTouchListener { _, event ->
            val lp=panelParams ?: return@setOnTouchListener false
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { x=event.rawX; y=event.rawY; width=lp.width; height=lp.height; true }
                MotionEvent.ACTION_MOVE -> { lp.width=max(dp(180),min(resources.displayMetrics.widthPixels,width+(x-event.rawX).toInt())); lp.height=max(dp(120),min(resources.displayMetrics.heightPixels,height+(event.rawY-y).toInt())); panelWidth=lp.width; panelHeight=lp.height; panel?.let { runCatching { wm.updateViewLayout(it,lp) } }; true }
                else -> true
            }
        }
    }

    private fun hasFreeformSupport():Boolean {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val pm=packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(contentResolver,"enable_freeform_support",0)==1 ||
            (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1 &&
                Settings.Global.getInt(contentResolver,"force_resizable_activities",0)==1)
    }

    private fun launchApp(pkg:String){
        if(!hasFreeformSupport()){
            val launch=packageManager.getLaunchIntentForPackage(pkg) ?: return
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            runCatching { startActivity(launch) }
            return
        }

        // Always enter through the bootstrap. Starting the target directly first
        // means Android can silently ignore its freeform ActivityOptions, leaving
        // the fallback unused and launching the app full-screen.
        val bootstrap=Intent(this,FreeformBootstrapActivity::class.java)
            .putExtra(FreeformBootstrapActivity.EXTRA_PACKAGE,pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NO_ANIMATION)

        val w=resources.displayMetrics.widthPixels
        val h=resources.displayMetrics.heightPixels
        val bootstrapOptions=ActivityOptions.makeBasic().apply {
            // Request FREEFORM for the bootstrap itself. Bounds alone do not select
            // a windowing mode on devices that also support full-screen windows.
            launchBounds=Rect(w,h,w+1,h+1)
            runCatching {
                HiddenApiBypass.invoke(
                    ActivityOptions::class.java,
                    this,
                    "setLaunchWindowingMode",
                    5
                )
            }
        }

        runCatching { startActivity(bootstrap,bootstrapOptions.toBundle()) }
            .onFailure {
                // The requested mode is unavailable despite the device reporting
                // support; preserve the existing normal-launch fallback.
                packageManager.getLaunchIntentForPackage(pkg)?.let { launch ->
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    runCatching { startActivity(launch) }
                }
            }
    }

    private fun closePanel(){panel?.let{runCatching{wm.removeView(it)}};panel=null;panelParams=null}
    private fun rounded(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()}
    private fun dp(v:Int)=(v*d).toInt()
    override fun onDestroy(){closePanel();handle?.let{runCatching{wm.removeView(it)}};handle=null;super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
