package com.tws.portalpaciente.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Escribe logs de diagnóstico directamente en Descargas y los divide en un máximo de cinco TXT.
 */
object DownloadsLogWriter {
    private const val TAG = "DownloadsLogWriter"
    private const val PARTS = 5
    private const val PART_LIMIT_BYTES = 350 * 1024
    private const val CHUNK_CHARS = 48 * 1024
    private const val DIRECTORY = "HRC-logs"
    @Volatile private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun appendFromApplicationContext(baseName: String, text: String) {
        val context = appContext
        if (context == null) {
            Log.e(TAG, "Logger no inicializado; se descarta escritura de $baseName")
            return
        }
        append(context, baseName, text)
    }

    @Synchronized
    fun append(context: Context, baseName: String, text: String) {
        if (text.isEmpty()) return
        val app = context.applicationContext
        try {
            var offset = 0
            while (offset < text.length) {
                val end = (offset + CHUNK_CHARS).coerceAtMost(text.length)
                if (!appendChunk(app, baseName, text.substring(offset, end))) {
                    Log.e(TAG, "No se pudo guardar $baseName en Descargas")
                    return
                }
                offset = end
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando $baseName en Descargas", e)
        }
    }

    private fun appendChunk(context: Context, baseName: String, chunk: String): Boolean {
        val bytes = chunk.toByteArray(Charsets.UTF_8).size
        for (part in 1..PARTS) {
            val name = "${baseName}_${part}.txt"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var found = findMediaStoreFile(context, name)
                if (found == null) {
                    val created = createMediaStoreFile(context, name) ?: return false
                    found = created to 0L
                }
                val (uri, size) = found
                if (size >= PART_LIMIT_BYTES && part < PARTS) continue
                if (size > 0L && size + bytes > PART_LIMIT_BYTES && part < PARTS) continue
                val output = context.contentResolver.openOutputStream(uri, "wa") ?: return false
                output.bufferedWriter(Charsets.UTF_8).use { it.write(chunk) }
                return true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    DIRECTORY
                )
                if (!dir.exists() && !dir.mkdirs()) return false
                val file = File(dir, name)
                val size = if (file.exists()) file.length() else 0L
                if (size >= PART_LIMIT_BYTES && part < PARTS) continue
                if (size > 0L && size + bytes > PART_LIMIT_BYTES && part < PARTS) continue
                file.appendText(chunk, Charsets.UTF_8)
                return true
            }
        }
        // Si las primeras cuatro partes se llenan, la quinta conserva el resto del log.
        val name = "${baseName}_${PARTS}.txt"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val found = findMediaStoreFile(context, name)
            val uri = found?.first ?: createMediaStoreFile(context, name) ?: return false
            context.contentResolver.openOutputStream(uri, "wa")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(chunk) }
                ?: return false
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY)
            if (!dir.exists() && !dir.mkdirs()) return false
            File(dir, name).appendText(chunk, Charsets.UTF_8)
        }
        return true
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
                val index = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                val size = if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else 0L
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
