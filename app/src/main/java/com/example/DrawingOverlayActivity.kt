package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.model.CanvasBackgroundColor
import com.example.service.FloatingDrawingService
import com.example.ui.canvas.DrawingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DrawingViewModel
import java.lang.ref.WeakReference

class DrawingOverlayActivity : ComponentActivity() {

    private val viewModel: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable Drawing on Lock Screen & Screen-Off Note Mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentActivityRef = WeakReference(this)
        FloatingDrawingService.notifyOverlayVisible(this, true)

        val prefs = getSharedPreferences(FloatingDrawingService.PREFS_NAME, Context.MODE_PRIVATE)
        val isOledBlackMode = prefs.getBoolean(FloatingDrawingService.KEY_OLED_BLACK_MODE, false)

        if (isOledBlackMode) {
            // Pure Black Canvas for Screen-Off OLED Note taking
            viewModel.setCanvasBackground(CanvasBackgroundColor.BLACK)
            viewModel.setBackgroundOpacity(1.0f)
        } else {
            // Default overlay background is Transparent so underlying apps/lock screen remain visible
            viewModel.setCanvasBackground(CanvasBackgroundColor.TRANSPARENT)
            viewModel.setBackgroundOpacity(0.0f)
        }

        setContent {
            MyApplicationTheme {
                DrawingScreen(
                    viewModel = viewModel,
                    onNavigateHome = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FloatingDrawingService.notifyOverlayVisible(this, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentActivityRef?.get() == this) {
            currentActivityRef = null
        }
        FloatingDrawingService.notifyOverlayVisible(this, false)
    }

    companion object {
        private var currentActivityRef: WeakReference<DrawingOverlayActivity>? = null

        fun closeOverlayIfOpen(context: Context) {
            currentActivityRef?.get()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.finish()
                }
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, DrawingOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }
}
