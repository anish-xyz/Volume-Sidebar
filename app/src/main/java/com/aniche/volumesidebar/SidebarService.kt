package com.aniche.volumesidebar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.abs

class SidebarService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var touchContainer: FrameLayout
    private lateinit var sidebarHandle: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var audioManager: AudioManager
    private lateinit var prefs: SharedPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var isMoveMode = false

    private var portraitY = 500
    private var portraitIsLeft = true

    private var landscapeY = 200
    private var landscapeIsLeft = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("sidebar_prefs", Context.MODE_PRIVATE)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        startForegroundServiceNotification()
        createSidebarHandle()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sidebar_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Volume Sidebar Running",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setContentTitle("Volume Sidebar Active")
            .setContentText("Swipe inward to trigger volume panel.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(1, notification)
    }

    private fun createSidebarHandle() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Read dimension preferences
        val handleWidth = prefs.getInt("handle_width", 30)
        val handleHeight = prefs.getInt("handle_height", 250)
        val touchWidth = prefs.getInt("touch_width", 160)
        val borderThickness = prefs.getInt("border_thickness", 2)

        // Read RGB Color values for Sidebar Fill
        val barA = prefs.getInt("bar_a", 128)
        val barR = prefs.getInt("bar_r", 0)
        val barG = prefs.getInt("bar_g", 0)
        val barB = prefs.getInt("bar_b", 0)
        val fillColor = Color.argb(barA, barR, barG, barB)

        // Read RGB Color values for Border
        val borderA = prefs.getInt("border_a", 255)
        val borderR = prefs.getInt("border_r", 255)
        val borderG = prefs.getInt("border_g", 255)
        val borderB = prefs.getInt("border_b", 255)
        val borderColor = Color.argb(borderA, borderR, borderG, borderB)

        touchContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val handleDrawable = GradientDrawable().apply {
            setColor(fillColor)
            setStroke(borderThickness, borderColor)
            cornerRadius = 4f
        }

        sidebarHandle = View(this).apply {
            background = handleDrawable
        }

        val handleParams = FrameLayout.LayoutParams(handleWidth, handleHeight).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        touchContainer.addView(sidebarHandle, handleParams)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            touchWidth,
            handleHeight + 70,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = portraitY

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        setupGestureListener()
        windowManager.addView(touchContainer, layoutParams)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPositionForOrientation(newConfig.orientation)
    }

    private fun applyPositionForOrientation(orientation: Int) {
        val isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT
        val currentY = if (isPortrait) portraitY else landscapeY
        val isLeft = if (isPortrait) portraitIsLeft else landscapeIsLeft

        layoutParams.y = currentY
        if (isLeft) {
            layoutParams.gravity = Gravity.TOP or Gravity.START
            sidebarHandle.layoutParams = (sidebarHandle.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
        } else {
            layoutParams.gravity = Gravity.TOP or Gravity.END
            sidebarHandle.layoutParams = (sidebarHandle.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
        }
        layoutParams.x = 0
        windowManager.updateViewLayout(touchContainer, layoutParams)
    }

    private fun setupGestureListener() {
        var startX = 0f
        var startY = 0f
        var initialY = 0
        var swipeTriggered = false

        val holdTime = prefs.getInt("hold_time", 2000).toLong()

        val holdRunnable = Runnable {
            isMoveMode = true
            sidebarHandle.alpha = 0.5f
        }

        touchContainer.setOnTouchListener { v, event ->
            val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val isLeftEdge = if (isPortrait) portraitIsLeft else landscapeIsLeft

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)

                    startX = event.rawX
                    startY = event.rawY
                    initialY = layoutParams.y
                    isMoveMode = false
                    swipeTriggered = false

                    handler.postDelayed(holdRunnable, holdTime)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    val deltaY = event.rawY - startY

                    if (isMoveMode) {
                        val newY = (initialY + deltaY).toInt()
                        layoutParams.y = newY

                        val screenWidth = resources.displayMetrics.widthPixels
                        val newlyIsLeft = event.rawX <= screenWidth / 2

                        if (isPortrait) {
                            portraitY = newY
                            portraitIsLeft = newlyIsLeft
                        } else {
                            landscapeY = newY
                            landscapeIsLeft = newlyIsLeft
                        }

                        if (newlyIsLeft) {
                            layoutParams.gravity = Gravity.TOP or Gravity.START
                            sidebarHandle.layoutParams = (sidebarHandle.layoutParams as FrameLayout.LayoutParams).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                            }
                        } else {
                            layoutParams.gravity = Gravity.TOP or Gravity.END
                            sidebarHandle.layoutParams = (sidebarHandle.layoutParams as FrameLayout.LayoutParams).apply {
                                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                            }
                        }

                        layoutParams.x = 0
                        windowManager.updateViewLayout(touchContainer, layoutParams)
                    } else {
                        val minSwipeDistance = 30
                        val isInwardSwipe = if (isLeftEdge) {
                            deltaX > minSwipeDistance
                        } else {
                            deltaX < -minSwipeDistance
                        }

                        if (isInwardSwipe && !swipeTriggered && abs(deltaY) < 120) {
                            swipeTriggered = true
                            handler.removeCallbacks(holdRunnable)
                            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(holdRunnable)
                    sidebarHandle.alpha = 1.0f
                    isMoveMode = false
                    true
                }

                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::touchContainer.isInitialized) {
            windowManager.removeView(touchContainer)
        }
    }
}