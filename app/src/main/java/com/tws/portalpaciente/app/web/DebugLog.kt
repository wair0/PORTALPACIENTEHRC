package com.tws.portalpaciente.app.web

import android.content.Context
import com.tws.portalpaciente.app.util.DownloadsLogWriter
import java.util.concurrent.CopyOnWriteArrayList

object DebugLog {
    private val entries = CopyOnWriteArrayList<String>()
    private const val MAX = 200

    fun add(tag: String, message: String) {
        val line = "${System.currentTimeMillis() % 100000} [$tag] $message"
        entries.add(line)
        if (entries.size > MAX) entries.removeAt(0)
        // Persistir cada evento: el registro en memoria se pierde al cerrar la aplicación.
        DownloadsLogWriter.appendFromApplicationContext("portal_debug", line + "\n")
    }

    fun dump(): String = entries.joinToString("\n")

    fun mode(context: Context): Boolean = true
}
