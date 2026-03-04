package com.kinetix.controller

/**
 * Immutable snapshot of the full controller state sent to the server.
 */
data class ControllerState(
    val lx: Float = 0f,
    val ly: Float = 0f,
    val rx: Float = 0f,
    val ry: Float = 0f,
    val a: Boolean = false,
    val b: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,
    val lb: Boolean = false,
    val rb: Boolean = false,
    val lt: Float = 0f,
    val rt: Float = 0f,
    val start: Boolean = false,
    val select: Boolean = false,
    val ls: Boolean = false,
    val rs: Boolean = false,
    val dpad: String = "none"
)
