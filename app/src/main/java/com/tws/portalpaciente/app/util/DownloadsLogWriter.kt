package com.tws.portalpaciente.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Escribe logs de diagnóstico en Descargas y los rota en un máximo de cinco TXT.
 * Cada parte se mantiene por debajo de PART_LIMIT_BYTES aproximadamente.
 */
object DownloadsLogWriter {
    private const val TAG = "DownloadsLogWriter"
    private const val PARTS = 5
    private const val PART_LIMIT_BYTES = 350 * 1024
    private const val CHUNK_CHARS = 48 * 1024
    private const val DIRECTORY = "HRC-logs"

    @Synchronized
    fun append(context: Context, baseName: String, text: String) {
        if (text.isEmpty()) return
        val app = context.applicationContext
        try {
            var offset = 0
            while (offset < text.length) {
                val end = (offset + CHUNK_CHARS).coerceAtMost(text.length)
                val chunk = text.substring(offset, end)
                if (!appendChunk(app, baseName, chunk)) {
                    Log.e(TAG, "No se pudo guardar $baseName en Descargas/HRC-logs; parte o almacenamiento no disponible")
                    return
                }
                offset = end
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando $baseName en Descargas/HRC-logs", e)
        }
    }

    private fun appendChunk(context: Context, baseName: String, chunk: String): Boolean {
        for (part in 1..PARTS) {
            val name = "${baseName}_${part}.txt"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val found = findMediaStoreFile(context, name)
                val uri = found?.first ?: createMediaStoreFile(context, name) ?: return false
                val currentSize = found?.second ?: 0L
                if (currentSize >= PART_LIMIT_BYTES && part < PARTS) continue
                if (currentSize + chunk.toByteArray(Charsets.UTF_8).size > PART_LIMIT_BYTES &&
                    currentSize > 0L && part < PARTS) continue
                val output = context.contentResolver.openOutputStream(uri, "wa") ?: return false
                output.bufferedWriter(Charsets.UTF_8).use { it.write(chunk); it.flush() }
                return true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    DIRECTORY
                )
                if (!dir.exists() && !dir.mkdirs()) return false
                val file = File(dir, name)
                if (file.exists() && file.length() >= PART_LIMIT_BYTES && part < PARTS) continue
                val bytes = chunk.toByteArray(Charsets.UTF_8).size
                if (file.exists() && file.length() + bytes > PART_LIMIT_BYTES &&
                    file.length() > 0L && part < PARTS) continue
                file.appendText(chunk, Charsets.UTF_8)
                return true
            }
        }
        // El quinto archivo actúa como último segmento y puede crecer si se agota la rotación.
        val name = "${baseName}_${PARTS}.txt"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val found = findMediaStoreFile(context, name)
            val uri = found?.first ?: createMediaStoreFile(context, name) ?: return false
            val output = context.contentResolver.openOutputStream(uri, "wa") ?: return false
            output.bufferedWriter(Charsets.UTF_8).use { it.write(chunk); it.flush() }
            true
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY)
            if (!dir.exists() && !dir.mkdirs()) return false
            File(dir, name).appendText(chunk, Charsets.UTF_8)
            true
        }
    }

    private fun findMediaStoreFile(context: Context, name: String): Pair<Uri, Long>? {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.SIZE),
            selection,
            arrayOf(name, Environment.DIRECTORY_DOWNLOADS + "/" + DIRECTORY + "/"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                val sizeIndex = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                return uri to size
            }
        }
        return null
    }

    private fun createMediaStoreFile(context: Context, name: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + DIRECTORY)
        }
        return context.contentResolver.insert(
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
        )
    }
}
