package com.portalpaciente.hrc.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val FILE_NAME = "portal_debug.log"

    fun log(context: Context, tag: String, message: String) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(dir, FILE_NAME)
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            FileWriter(file, true).use { writer ->
                writer.append("$ts [$tag] $message\n")
            }
        } catch (e: Exception) {
            // silencioso
        }
    }

    fun clear(context: Context) {
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            File(dir, FILE_NAME).delete()
        } catch (_: Exception) {}
    }
}
