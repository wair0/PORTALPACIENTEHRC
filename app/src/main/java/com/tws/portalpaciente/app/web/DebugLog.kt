package com.tws.portalpaciente.app.web

import android.content.Context
import java.util.concurrent.CopyOnWriteArrayList

object DebugLog {
    private val entries = CopyOnWriteArrayList<String>()
    private const val MAX = 200

    fun add(tag: String, message: String) {
        val line = "${System.currentTimeMillis() % 100000} [$tag] $message"
        entries.add(line)
        if (entries.size > MAX) entries.removeAt(0)
    }

    fun dump(): String = entries.joinToString("\n")

    fun mode(context: Context): Boolean = true
}
