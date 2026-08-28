package com.example.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.DrawingOverlayActivity
import com.example.MainActivity
import com.example.R
import com.example.model.FloatingIconSize
import kotlin.math.abs

class FloatingDrawingService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var prefs: SharedPreferences
    private var screenWidth: Int = 1080
    private var screenHeight: Int = 1920

    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // When phone is locked, active drawing session stops and returns to normal screen
                    DrawingOverlayActivity.closeOverlayIfOpen(applicationContext)
                }
                ACTION_OVERLAY_STATE_CHANGED -> {
                    val isOpen = intent.getBooleanExtra(EXTRA_IS_OPEN, false)
                    floatingView?.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(ACTION_OVERLAY_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenLockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenLockReceiver, filter)
        }

        updateScreenDimensions()
        showFloatingIcon()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Refresh icon configuration if needed
        updateIconSizeAndPosition()

        return START_STICKY
    }

    private fun updateScreenDimensions() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun showFloatingIcon() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        if (floatingView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val sizeName = prefs.getString(KEY_ICON_SIZE, FloatingIconSize.SMALL.name) ?: FloatingIconSize.SMALL.name
        val iconSizeEnum = try {
            FloatingIconSize.valueOf(sizeName)
        } catch (e: Exception) {
            FloatingIconSize.SMALL
        }
        val sizePx = dpToPx(iconSizeEnum.dpSize)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(KEY_POS_X, dpToPx(16))
            y = prefs.getInt(KEY_POS_Y, screenHeight / 3)
        }

        val rootLayout = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
        }

        val iconContainer = FrameLayout(this).apply {
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#090A0E"))
                setStroke(dpToPx(2), Color.parseColor("#00E5FF"))
            }
            background = bgDrawable
            elevation = dpToPx(8).toFloat()
        }

        val iconText = TextView(this).apply {
            text = "🖌️"
            textSize = when (iconSizeEnum) {
                FloatingIconSize.SMALL -> 18f
                FloatingIconSize.MEDIUM -> 24f
                FloatingIconSize.LARGE -> 30f
            }
            gravity = Gravity.CENTER
        }

        val glowDot = View(this).apply {
            val dotDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#00E5FF"))
            }
            background = dotDrawable
            layoutParams = FrameLayout.LayoutParams(dpToPx(6), dpToPx(6)).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dpToPx(4), dpToPx(4), 0)
            }
        }

        iconContainer.addView(iconText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        iconContainer.addView(glowDot)
        rootLayout.addView(iconContainer, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        setupDragAndClickListener(rootLayout)

        floatingView = rootLayout
        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDragAndClickListener(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchStartTime = 0L

        view.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try {
                        windowManager?.updateViewLayout(floatingView, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val totalDx = abs(event.rawX - initialTouchX)
                    val totalDy = abs(event.rawY - initialTouchY)
                    val touchDuration = System.currentTimeMillis() - touchStartTime

                    // If tap with minimal movement: open drawing overlay
                    if (totalDx < dpToPx(10) && totalDy < dpToPx(10) && touchDuration < 300) {
                        openDrawingOverlay()
                    } else {
                        // Keep icon at selected position or snap to edge if enabled
                        val edgeSnap = prefs.getBoolean(KEY_EDGE_SNAP, true)
                        if (edgeSnap) {
                            snapToNearestEdge(params)
                        } else {
                            savePosition(params.x, params.y)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToNearestEdge(params: WindowManager.LayoutParams) {
        val currentX = params.x
        val viewWidth = floatingView?.width ?: dpToPx(44)
        val targetX = if (currentX + viewWidth / 2 < screenWidth / 2) {
            dpToPx(8)
        } else {
            screenWidth - viewWidth - dpToPx(8)
        }

        val animator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                params.x = va.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(floatingView, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animator.start()
        savePosition(targetX, params.y)
    }

    private fun savePosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_POS_X, x)
            .putInt(KEY_POS_Y, y)
            .apply()
    }

    private fun openDrawingOverlay() {
        val intent = Intent(this, DrawingOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        // Temporarily hide the floating bubble while drawing overlay is active
        floatingView?.visibility = View.GONE
    }

    private fun updateIconSizeAndPosition() {
        val params = layoutParams ?: return
        val sizeName = prefs.getString(KEY_ICON_SIZE, FloatingIconSize.SMALL.name) ?: FloatingIconSize.SMALL.name
        val iconSizeEnum = try {
            FloatingIconSize.valueOf(sizeName)
        } catch (e: Exception) {
            FloatingIconSize.SMALL
        }
        val sizePx = dpToPx(iconSizeEnum.dpSize)
        params.width = sizePx
        params.height = sizePx
        try {
            windowManager?.updateViewLayout(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BlackCanvas Floating Drawing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating drawing icon to draw directly over screen."
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openMainIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingDrawingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BlackCanvas Active")
            .setContentText("Floating drawing icon ready. Tap icon to draw over screen.")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(openMainIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenLockReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatingView = null
        }
    }

    companion object {
        const val CHANNEL_ID = "black_canvas_floating_service"
        const val NOTIFICATION_ID = 4040
        const val PREFS_NAME = "black_canvas_floating_prefs"
        const val KEY_POS_X = "floating_icon_pos_x"
        const val KEY_POS_Y = "floating_icon_pos_y"
        const val KEY_ICON_SIZE = "floating_icon_size"
        const val KEY_EDGE_SNAP = "floating_edge_snap"

        const val ACTION_STOP_SERVICE = "com.example.action.STOP_FLOATING_SERVICE"
        const val ACTION_OVERLAY_STATE_CHANGED = "com.example.action.OVERLAY_STATE_CHANGED"
        const val EXTRA_IS_OPEN = "extra_is_open"

        fun startService(context: Context) {
            val intent = Intent(context, FloatingDrawingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingDrawingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun notifyOverlayVisible(context: Context, isOpen: Boolean) {
            val intent = Intent(ACTION_OVERLAY_STATE_CHANGED).apply {
                putExtra(EXTRA_IS_OPEN, isOpen)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}
