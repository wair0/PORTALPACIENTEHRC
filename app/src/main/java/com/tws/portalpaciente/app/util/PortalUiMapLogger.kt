package com.tws.portalpaciente.app.util

import android.content.Context
import org.json.JSONObject

/**
 * Guarda el mapa técnico de la interfaz sin valores de formularios ni texto clínico/personal.
 */
object PortalUiMapLogger {
    private const val MAX_ENTRY_CHARS = 500_000

    fun append(context: Context, payload: String) {
        if (payload.isBlank()) return
        val safePayload = payload.take(MAX_ENTRY_CHARS)
        val formatted = runCatching { JSONObject(safePayload).toString(2) }
            .getOrElse { safePayload } + "\n\n--- FIN DEL MAPA ---\n\n"
        DownloadsLogWriter.append(context, "portal_ui_map", formatted)
    }
}
