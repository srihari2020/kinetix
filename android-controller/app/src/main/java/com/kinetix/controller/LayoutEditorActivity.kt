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
        
        activeProfile?.let {
            if (it.layoutJson.isNotEmpty()) {
                controllerView.customLayoutJson = it.layoutJson
            }
        }

        // Apply joystick initial positions
        leftJoystick.post {
            applyJoystickPosition(leftJoystick, "left")
            applyJoystickPosition(rightJoystick, "right")
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
                profiles[index] = profiles[index].copy(layoutJson = buildMasterJson())
                ControllerProfile.saveAll(this, profiles)
                Toast.makeText(this, "Layout saved to ${profiles[index].name}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun buildMasterJson(): String {
        val rootStr = controllerView.exportLayoutJson()
        val root = try { org.json.JSONObject(rootStr) } catch(e:Exception){ org.json.JSONObject() }
        val sticksArray = org.json.JSONArray()
        
        val lParent = leftJoystick.parent as View
        val rParent = rightJoystick.parent as View
        
        val ljObj = org.json.JSONObject()
        ljObj.put("id", "left")
        ljObj.put("x", (leftJoystick.x + leftJoystick.width/2f) / lParent.width)
        ljObj.put("y", (leftJoystick.y + leftJoystick.height/2f) / lParent.height)
        ljObj.put("size", 160)
        sticksArray.put(ljObj)
        
        val rjObj = org.json.JSONObject()
        rjObj.put("id", "right")
        rjObj.put("x", (rightJoystick.x + rightJoystick.width/2f) / rParent.width)
        rjObj.put("y", (rightJoystick.y + rightJoystick.height/2f) / rParent.height)
        rjObj.put("size", 160)
        sticksArray.put(rjObj)
        
        root.put("sticks", sticksArray)
        return root.toString()
    }

    private fun applyJoystickPosition(view: View, id: String) {
        val jsonStr = activeProfile?.layoutJson ?: return
        if (jsonStr.isEmpty()) return
        
        try {
            val root = org.json.JSONObject(jsonStr)
            val sticks = root.optJSONArray("sticks") ?: return
            for (i in 0 until sticks.length()) {
                val obj = sticks.getJSONObject(i)
                if (obj.optString("id") == id) {
                    val cx = obj.optDouble("x").toFloat()
                    val cy = obj.optDouble("y").toFloat()
                    val parent = view.parent as View
                    view.x = cx * parent.width - view.width / 2f
                    view.y = cy * parent.height - view.height / 2f
                    break
                }
            }
        } catch (e: Exception) {}
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

    private fun goImmersive() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        supportActionBar?.hide()
    }
}
