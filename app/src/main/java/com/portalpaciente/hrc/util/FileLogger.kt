package com.portalpaciente.hrc.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val FILE_NAME = "portal_debug.log"

    fun log(context: Context, tag: String, message: String) {
        val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())} [$tag] $message\n"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeWithMediaStore(context, line)
            } else {
                writeLegacy(line)
            }
        } catch (e: Exception) {
            // fallback silencioso
        }
    }

    private fun writeWithMediaStore(context: Context, line: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        // Buscar si ya existe
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val args = arrayOf(FILE_NAME)
        var uri = resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                android.content.ContentUris.withAppendedId(collection, id)
            } else null
        }

        if (uri == null) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            uri = resolver.insert(collection, values)
        }

        uri?.let {
            resolver.openOutputStream(it, "wa")?.use { out ->
                out.write(line.toByteArray())
            }
        }
    }

    private fun writeLegacy(line: String) {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, FILE_NAME)
        FileWriter(file, true).use { it.append(line) }
    }

    fun clear(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.delete(collection, "${MediaStore.Downloads.DISPLAY_NAME} = ?", arrayOf(FILE_NAME))
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, FILE_NAME).delete()
            }
        } catch (_: Exception) {}
    }
}
