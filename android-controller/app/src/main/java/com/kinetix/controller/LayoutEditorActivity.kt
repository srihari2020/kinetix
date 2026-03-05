package com.kinetix.controller

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LayoutEditorActivity : AppCompatActivity() {

    private lateinit var controllerView: ControllerView
    private lateinit var leftJoystick: JoystickView
    private lateinit var rightJoystick: JoystickView

    private var activeProfile: ControllerProfile? = null
    private var layoutOverrides = mutableMapOf<String, FloatArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout_editor)
        goImmersive()

        controllerView = findViewById(R.id.controller_view)
        leftJoystick = findViewById(R.id.left_joystick)
        rightJoystick = findViewById(R.id.right_joystick)

        val resetBtn: Button = findViewById(R.id.reset_btn)
        val saveBtn: Button = findViewById(R.id.save_btn)

        // Load active profile layout
        activeProfile = ControllerProfile.getActive(this)
        activeProfile?.buttonPositions?.let {
            layoutOverrides.putAll(it)
            controllerView.layoutOverrides = layoutOverrides
        }

        // Apply joystick initial positions
        controllerView.post {
            applyJoystickPosition(leftJoystick, "left_joystick")
            applyJoystickPosition(rightJoystick, "right_joystick")
        }

        // Enable edit mode on controllerView
        controllerView.isEditMode = true
        controllerView.onLayoutChanged = { map ->
            layoutOverrides.putAll(map)
        }

        // Enable dragging for joysticks
        setupDraggable(leftJoystick, "left_joystick")
        setupDraggable(rightJoystick, "right_joystick")

        resetBtn.setOnClickListener {
            layoutOverrides.clear()
            controllerView.layoutOverrides = layoutOverrides
            
            // Reset joysticks to margin-based locations
            leftJoystick.translationX = 0f
            leftJoystick.translationY = 0f
            rightJoystick.translationX = 0f
            rightJoystick.translationY = 0f
        }

        saveBtn.setOnClickListener {
            val profiles = ControllerProfile.loadAll(this)
            val index = profiles.indexOfFirst { it.name == activeProfile?.name }
            if (index >= 0) {
                profiles[index] = profiles[index].copy(buttonPositions = layoutOverrides)
                ControllerProfile.saveAll(this, profiles)
                Toast.makeText(this, "Layout saved to ${profiles[index].name}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun applyJoystickPosition(view: View, id: String) {
        layoutOverrides[id]?.let { pos ->
            val parent = view.parent as View
            val targetX = pos[0] * parent.width - view.width / 2f
            val targetY = pos[1] * parent.height - view.height / 2f
            view.translationX = targetX - view.left
            view.translationY = targetY - view.top
        }
    }

    private fun setupDraggable(view: View, id: String) {
        var dX = 0f
        var dY = 0f
        
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    v.animate()
                        .x(newX)
                        .y(newY)
                        .setDuration(0)
                        .start()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val parent = v.parent as View
                    val cx = v.x + v.width / 2f
                    val cy = v.y + v.height / 2f
                    layoutOverrides[id] = floatArrayOf(cx / parent.width, cy / parent.height)
                    true
                }
                else -> false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun goImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.hide(WindowInsets.Type.systemBars())
                ctrl.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
}
