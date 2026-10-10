package com.portalpaciente.hrc.web

import android.webkit.JavascriptInterface
import com.portalpaciente.hrc.data.ProfileInfo
import com.portalpaciente.hrc.ui.PortalViewModel
import org.json.JSONObject

/**
 * Puente JS → Kotlin (`window.HRCBridge`). Solo recibe datos de presentación (nombre, foto,
 * cantidad de notificaciones) y valida tamaño y forma de todo lo que entra.
 * La WebView solo navega dentro del host del portal, así que ningún sitio externo puede llamarlo.
 */
class PortalJsBridge(private val viewModel: PortalViewModel) {

    @JavascriptInterface
    fun onProfile(json: String?) {
        if (json == null || json.length > MAX_PAYLOAD) return
        runCatching {
            val obj = JSONObject(json)
            val name = obj.optString("name").trim().take(MAX_NAME)
            val photo = obj.optString("photo")
                .takeIf { it.startsWith("data:image/") && it.length <= MAX_PHOTO } ?: ""
            if (name.isNotEmpty()) viewModel.onProfile(ProfileInfo(name, photo))
        }
    }

    @JavascriptInterface
    fun onUnread(count: Int) {
        viewModel.onUnread(count.coerceIn(0, 99))
    }

    private companion object {
        const val MAX_PAYLOAD = 800_000
        const val MAX_PHOTO = 700_000
        const val MAX_NAME = 120
    }
}
