package com.aniche.volumesidebar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("sidebar_prefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // Apply Button
        val enableBtn = Button(this).apply {
            text = "Enable / Apply Settings"
            setOnClickListener { checkPermissionAndStart() }
        }
        layout.addView(enableBtn)

        // Helper function for adding sliders
        fun addSlider(title: String, min: Int, max: Int, current: Int, onProgress: (Int) -> Unit) {
            val label = TextView(this).apply {
                text = "$title: $current"
                textSize = 15f
                setPadding(0, 15, 0, 0)
            }
            val seekBar = SeekBar(this).apply {
                this.max = max - min
                this.progress = current - min
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                        val value = progress + min
                        label.text = "$title: $value"
                        onProgress(value)
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {}
                })
            }
            layout.addView(label)
            layout.addView(seekBar)
        }

        // --- Dimensions & Gestures ---
        val currentTouchWidth = prefs.getInt("touch_width", 160)
        addSlider("Touch Zone Width (px)", 100, 300, currentTouchWidth) { value ->
            prefs.edit().putInt("touch_width", value).apply()
        }

        val currentHandleWidth = prefs.getInt("handle_width", 30)
        addSlider("Sidebar Width (px)", 10, 80, currentHandleWidth) { value ->
            prefs.edit().putInt("handle_width", value).apply()
        }

        val currentHandleHeight = prefs.getInt("handle_height", 250)
        addSlider("Sidebar Height (px)", 100, 500, currentHandleHeight) { value ->
            prefs.edit().putInt("handle_height", value).apply()
        }

        val currentHoldTime = prefs.getInt("hold_time", 2000)
        addSlider("Hold to Move (ms)", 500, 4000, currentHoldTime) { value ->
            prefs.edit().putInt("hold_time", value).apply()
        }

        // --- Border Options ---
        val currentBorderThickness = prefs.getInt("border_thickness", 2)
        addSlider("Border Thickness (px)", 0, 15, currentBorderThickness) { value ->
            prefs.edit().putInt("border_thickness", value).apply()
        }

        // --- Sidebar RGB Color Sliders ---
        val barSection = TextView(this).apply {
            text = "\n-- Sidebar Fill Color --"
            textSize = 16f
        }
        layout.addView(barSection)

        addSlider("Sidebar Transparency (Alpha)", 0, 255, prefs.getInt("bar_a", 128)) { v -> prefs.edit().putInt("bar_a", v).apply() }
        addSlider("Sidebar Red", 0, 255, prefs.getInt("bar_r", 0)) { v -> prefs.edit().putInt("bar_r", v).apply() }
        addSlider("Sidebar Green", 0, 255, prefs.getInt("bar_g", 0)) { v -> prefs.edit().putInt("bar_g", v).apply() }
        addSlider("Sidebar Blue", 0, 255, prefs.getInt("bar_b", 0)) { v -> prefs.edit().putInt("bar_b", v).apply() }

        // --- Border RGB Color Sliders ---
        val borderSection = TextView(this).apply {
            text = "\n-- Border Color --"
            textSize = 16f
        }
        layout.addView(borderSection)

        addSlider("Border Transparency (Alpha)", 0, 255, prefs.getInt("border_a", 255)) { v -> prefs.edit().putInt("border_a", v).apply() }
        addSlider("Border Red", 0, 255, prefs.getInt("border_r", 255)) { v -> prefs.edit().putInt("border_r", v).apply() }
        addSlider("Border Green", 0, 255, prefs.getInt("border_g", 255)) { v -> prefs.edit().putInt("border_g", v).apply() }
        addSlider("Border Blue", 0, 255, prefs.getInt("border_b", 255)) { v -> prefs.edit().putInt("border_b", v).apply() }

        val scrollView = ScrollView(this).apply { addView(layout) }
        setContentView(scrollView)
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            startSidebarService()
        }
    }

    private fun startSidebarService() {
        val intent = Intent(this, SidebarService::class.java)
        stopService(intent) // Restart service to apply settings instantly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Sidebar Updated & Enabled", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startSidebarService()
            } else {
                Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}