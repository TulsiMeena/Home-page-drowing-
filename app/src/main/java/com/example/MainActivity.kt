package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.canvas.DrawingScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DrawingViewModel

enum class AppScreen {
    HOME,
    DRAWING
}

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_OPEN_DRAWING = "com.example.ACTION_OPEN_DRAWING"
    }

    private val drawingViewModel: DrawingViewModel by viewModels()
    private var currentScreen by mutableStateOf(AppScreen.DRAWING)

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                handleScreenLockOrExit()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If explicitly opened with drawing action or normal launch, start directly in smooth black canvas
        currentScreen = AppScreen.DRAWING

        // Register receiver for phone screen lock/off events (Requirement #25)
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        setContent {
            MyApplicationTheme {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState == AppScreen.DRAWING) {
                            (fadeIn(androidx.compose.animation.core.tween(250)) +
                                scaleIn(initialScale = 0.96f, animationSpec = androidx.compose.animation.core.tween(250)))
                                .togetherWith(
                                    fadeOut(androidx.compose.animation.core.tween(200)) +
                                    scaleOut(targetScale = 1.02f, animationSpec = androidx.compose.animation.core.tween(200))
                                )
                        } else {
                            (fadeIn(androidx.compose.animation.core.tween(220)) +
                                scaleIn(initialScale = 1.02f, animationSpec = androidx.compose.animation.core.tween(220)))
                                .togetherWith(
                                    fadeOut(androidx.compose.animation.core.tween(180)) +
                                    scaleOut(targetScale = 0.96f, animationSpec = androidx.compose.animation.core.tween(180))
                                )
                        }
                    },
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                onOpenDrawing = {
                                    currentScreen = AppScreen.DRAWING
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        AppScreen.DRAWING -> {
                            DrawingScreen(
                                viewModel = drawingViewModel,
                                onNavigateHome = {
                                    handleScreenLockOrExit()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Requirement #25 & #26:
     * When user locks device or leaves drawing screen, discard unsaved drawing session
     * and reset navigation to Home Page.
     */
    private fun handleScreenLockOrExit() {
        if (currentScreen == AppScreen.DRAWING) {
            drawingViewModel.discardSession()
            currentScreen = AppScreen.HOME
        }
    }

    override fun onStop() {
        super.onStop()
        // If app goes to background / phone is locked, ensure drawing session is closed (Requirement #25)
        handleScreenLockOrExit()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {
        }
    }
}
