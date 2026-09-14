package com.rnsgate.app.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Thread-safe in-app diagnostic ring buffer for user-facing Copy/Share.
 * Does NOT send anything to the network.
 */
object DiagLog {

    private const val MAX_LINES = 800
    private const val TAG = "DiagLog"

    private val lock = Any()
    private val lines = ArrayDeque<String>(MAX_LINES)
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    fun i(tag: String, msg: String) = append("I", tag, msg)
    fun w(tag: String, msg: String) = append("W", tag, msg)
    fun e(tag: String, msg: String) = append("E", tag, msg)

    fun append(level: String, tag: String, msg: String) {
        val line = "${timeFmt.format(Date())} $level/$tag: ${msg.trimEnd()}"
        synchronized(lock) {
            while (lines.size >= MAX_LINES) {
                lines.removeFirst()
            }
            lines.addLast(line)
            _text.value = lines.joinToString("\n")
        }
        when (level) {
            "E" -> Log.e(TAG, "$tag: $msg")
            "W" -> Log.w(TAG, "$tag: $msg")
            else -> Log.i(TAG, "$tag: $msg")
        }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
            _text.value = ""
        }
    }

    fun snapshot(): String = synchronized(lock) { lines.joinToString("\n") }

    fun lineCount(): Int = synchronized(lock) { lines.size }
}
