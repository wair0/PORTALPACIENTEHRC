package com.tws.portalpaciente.app.util

import android.content.Context
import org.json.JSONObject
import com.tws.portalpaciente.app.web.DebugLog

/**
 * Guarda el mapa técnico de la interfaz sin valores de formularios ni texto clínico/personal.
 */
object PortalUiMapLogger {
    private const val MAX_ENTRY_CHARS = 500_000

    fun append(context: Context, payload: String) {
        if (payload.isBlank()) {
            DebugLog.add("UI_MAP", "Android recibió un mapa vacío; no se guarda")
            return
        }
        DebugLog.add("UI_MAP", "Android recibió mapa: ${payload.length} caracteres")
        val safePayload = payload.take(MAX_ENTRY_CHARS)
        val formatted = runCatching { JSONObject(safePayload).toString(2) }
            .getOrElse {
                DebugLog.add("UI_MAP", "El mapa no se pudo formatear como JSON; se guardará el texto original")
                safePayload
            } + "\n\n--- FIN DEL MAPA ---\n\n"
        val saved = DownloadsLogWriter.append(context, "portal_ui_map", formatted)
        DebugLog.add("UI_MAP", if (saved) {
            "Mapa guardado en Descargas: ${formatted.length} caracteres"
        } else {
            "ERROR: Android recibió el mapa, pero no pudo guardarlo en Descargas"
        })
    }
}
