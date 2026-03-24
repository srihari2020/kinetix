package com.kinetix.controller.v2

/**
 * Immutable snapshot of the full controller state sent to the server.
 */
data class ControllerState(
    var lx: Float = 0f,
    var ly: Float = 0f,
    var rx: Float = 0f,
    var ry: Float = 0f,
    var a: Boolean = false,
    var b: Boolean = false,
    var x: Boolean = false,
    var y: Boolean = false,
    var lb: Boolean = false,
    var rb: Boolean = false,
    var lt: Float = 0f,
    var rt: Float = 0f,
    var start: Boolean = false,
    var select: Boolean = false,
    var ls: Boolean = false,
    var rs: Boolean = false,
    var dpad: String = "none"
)
