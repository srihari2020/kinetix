package com.kinetix.controller.v2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kinetix.controller.v2.system.DraggableElement
import com.kinetix.controller.v2.ui.components.ABXYGroup
import com.kinetix.controller.v2.ui.components.BumperView
import com.kinetix.controller.v2.ui.components.CenterButtonsView
import com.kinetix.controller.v2.ui.components.DPadGroup
import com.kinetix.controller.v2.ui.components.JoystickView
import com.kinetix.controller.v2.ui.components.TriggerView
import org.json.JSONObject

/**
 * Editor Screen — Full freestyle customization.
 * Drag, independent X/Y resize, add/remove components.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var editorRoot: FrameLayout
    private lateinit var handleOverlay: HandleOverlayView

    // Component registry: key → (view, draggable, visible)
    data class ComponentEntry(val view: View, var draggable: DraggableElement? = null, var visible: Boolean = true)
    private val components = linkedMapOf<String, ComponentEntry>()

    // Available components that can be added
    private val addableTypes = listOf(
        "D-Pad" to "dpad",
        "Left Joystick" to "left",
        "Right Joystick" to "right",
        "ABXY Buttons" to "abxy",
        "LT Trigger" to "lt",
        "RT Trigger" to "rt",
        "LB Bumper" to "lb",
        "RB Bumper" to "rb",
        "Start/Select" to "center"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        goImmersive()

        editorRoot = findViewById(R.id.editor_root)

        // ── Create handle overlay (drawn on top of everything) ──
        handleOverlay = HandleOverlayView(this)
        // Will be added to root after components

        // ── Register all default components ──
        registerComponent("dpad", findViewById(R.id.editor_dpad))
        registerComponent("abxy", findViewById(R.id.editor_action_buttons))
        registerComponent("left", findViewById(R.id.editor_left_joystick))
        registerComponent("right", findViewById(R.id.editor_right_joystick))
        registerComponent("lt", findViewById(R.id.editor_lt))
        registerComponent("rt", findViewById(R.id.editor_rt))
        registerComponent("lb", findViewById(R.id.editor_lb))
        registerComponent("rb", findViewById(R.id.editor_rb))
        registerComponent("center", findViewById(R.id.editor_center))

        // Setup bumper labels
        (components["lb"]!!.view as? BumperView)?.label = "LB"
        (components["rb"]!!.view as? BumperView)?.label = "RB"

        // ── Add handle overlay on top ──
        editorRoot.addView(handleOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // ── Load existing layout ──
        loadLayout()

        // ── Buttons ──
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            saveLayout()
            Toast.makeText(this, "Layout saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            resetLayout()
            Toast.makeText(this, "Layout reset to default", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_add).setOnClickListener {
            showAddComponentDialog()
        }
    }

    private fun registerComponent(id: String, view: View) {
        view.tag = "editor_mode" // Mark as editor — components check this to skip game input

        val entry = ComponentEntry(view)
        val draggable = DraggableElement(view) {
            handleOverlay.invalidate() // Redraw handles when layout changes
        }
        draggable.attach()
        entry.draggable = draggable
        components[id] = entry

        // Long press to remove
        view.setOnLongClickListener {
            showRemoveDialog(id)
            true
        }
    }

    // ── Add Component Dialog ──
    private fun showAddComponentDialog() {
        // Filter out already visible components
        val hiddenTypes = addableTypes.filter { (_, id) ->
            val entry = components[id]
            entry == null || !entry.visible
        }

        if (hiddenTypes.isEmpty()) {
            Toast.makeText(this, "All components are already visible!", Toast.LENGTH_SHORT).show()
            return
        }

        val names = hiddenTypes.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Add Component")
            .setItems(names) { _, which ->
                val (_, id) = hiddenTypes[which]
                val entry = components[id]
                if (entry != null) {
                    entry.visible = true
                    entry.view.visibility = View.VISIBLE
                    entry.view.alpha = 0f
                    entry.view.animate().alpha(1f).setDuration(300).start()
                    handleOverlay.invalidate()
                    Toast.makeText(this, "${names[which]} added!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Remove Component Dialog ──
    private fun showRemoveDialog(id: String) {
        val name = addableTypes.find { it.second == id }?.first ?: id
        AlertDialog.Builder(this)
            .setTitle("Remove $name?")
            .setMessage("This will hide the component from the controller. You can add it back anytime.")
            .setPositiveButton("Remove") { _, _ ->
                val entry = components[id]
                if (entry != null) {
                    entry.visible = false
                    entry.view.animate().alpha(0f).setDuration(200).withEndAction {
                        entry.view.visibility = View.GONE
                    }.start()
                    handleOverlay.invalidate()
                    Toast.makeText(this, "$name removed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Save layout as normalized JSON (with visibility) ──
    private fun saveLayout() {
        val w = editorRoot.width.toFloat()
        val h = editorRoot.height.toFloat()
        if (w == 0f || h == 0f) return

        val json = JSONObject()
        components.forEach { (id, entry) ->
            val v = entry.view
            val obj = JSONObject()
            obj.put("tx", v.translationX / w)
            obj.put("ty", v.translationY / h)
            obj.put("scaleX", v.scaleX)
            obj.put("scaleY", v.scaleY)
            obj.put("visible", entry.visible)
            json.put(id, obj)
        }
        getSharedPreferences("kinetix_layout", Context.MODE_PRIVATE)
            .edit().putString("layout_offsets_v3", json.toString()).apply()
    }

    // ── Load existing layout ──
    private fun loadLayout() {
        editorRoot.post {
            val prefs = getSharedPreferences("kinetix_layout", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("layout_offsets_v3", null) ?: return@post
            try {
                val json = JSONObject(jsonStr)
                val w = editorRoot.width.toFloat()
                val h = editorRoot.height.toFloat()
                if (w == 0f || h == 0f) return@post

                components.forEach { (id, entry) ->
                    val obj = json.optJSONObject(id)
                    if (obj != null) {
                        entry.view.translationX = obj.getDouble("tx").toFloat() * w
                        entry.view.translationY = obj.getDouble("ty").toFloat() * h
                        entry.view.scaleX = obj.getDouble("scaleX").toFloat()
                        entry.view.scaleY = obj.getDouble("scaleY").toFloat()
                        entry.visible = obj.optBoolean("visible", true)
                        entry.view.visibility = if (entry.visible) View.VISIBLE else View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ── Reset all to default ──
    private fun resetLayout() {
        components.forEach { (_, entry) ->
            entry.view.translationX = 0f
            entry.view.translationY = 0f
            entry.view.scaleX = 1.0f
            entry.view.scaleY = 1.0f
            entry.visible = true
            entry.view.visibility = View.VISIBLE
            entry.view.alpha = 1f
        }
        handleOverlay.invalidate()
        getSharedPreferences("kinetix_layout", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun goImmersive() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).let { c ->
            c.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            c.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        supportActionBar?.hide()
    }

    /**
     * Overlay view that draws resize handles and selection borders on top of all components.
     */
    inner class HandleOverlayView(context: Context) : View(context) {
        init {
            // Must not intercept touches — let them pass through to components below
            setWillNotDraw(false)
        }

        override fun onDraw(canvas: Canvas) {
            components.forEach { (_, entry) ->
                if (entry.visible) {
                    entry.draggable?.drawHandles(canvas)
                }
            }
        }

        // Pass all touches through to views below
        override fun onTouchEvent(event: android.view.MotionEvent?): Boolean = false
    }
}
