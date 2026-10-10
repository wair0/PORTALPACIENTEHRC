package com.tws.portalpaciente.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject

/**
 * Guarda únicamente el mapa técnico de la interfaz: estructura, clases y estilos calculados.
 * No recibe valores de formularios ni el texto clínico/personal de las tarjetas.
 */
object PortalUiMapLogger {
    private const val FILE_NAME = "portal_ui_map.log"
    private const val MAX_ENTRY_CHARS = 500_000
    @Volatile private var cachedUri: Uri? = null

    fun append(context: Context, payload: String) {
        if (payload.isBlank()) return
        val safePayload = payload.take(MAX_ENTRY_CHARS)
        val formatted = runCatching { JSONObject(safePayload).toString(2) }
            .getOrElse { safePayload } + "\n\n--- FIN DEL MAPA ---\n\n"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appendMediaStore(context.applicationContext, formatted)
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                java.io.File(dir, FILE_NAME).appendText(formatted)
            }
        }
    }

    private fun appendMediaStore(context: Context, text: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        var uri = cachedUri
        if (uri == null) {
            val projection = arrayOf(MediaStore.Downloads._ID)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
            resolver.query(
                collection, projection, selection,
                arrayOf(FILE_NAME, Environment.DIRECTORY_DOWNLOADS + "/"), null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                }
            }
        }
        if (uri == null) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            uri = resolver.insert(collection, values)
        }
        cachedUri = uri
        uri?.let { resolver.openOutputStream(it, "wa")?.bufferedWriter(Charsets.UTF_8)?.use { out -> out.append(text) } }
    }
}
