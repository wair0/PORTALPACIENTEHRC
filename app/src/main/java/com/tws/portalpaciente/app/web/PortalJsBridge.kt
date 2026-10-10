package com.tws.portalpaciente.app.web

import android.content.Context
import android.webkit.JavascriptInterface
import com.tws.portalpaciente.app.data.ProfileInfo
import com.tws.portalpaciente.app.ui.PortalViewModel
import com.tws.portalpaciente.app.util.PortalUiMapLogger

class PortalJsBridge(
    private val viewModel: PortalViewModel,
    private val context: Context
) {
    @JavascriptInterface
    fun onProfile(fullName: String?, photo: String?) {
        viewModel.onProfile(ProfileInfo(fullName ?: "", photo))
    }

    @JavascriptInterface
    fun onUnread(count: Int) {
        viewModel.onUnread(count)
    }

    @JavascriptInterface
    fun requestLogout() {
        viewModel.requestLogout()
    }

    /** Recibe el mapa técnico de UI; el JS omite valores de formularios y texto clínico. */
    @JavascriptInterface
    fun saveUiMap(json: String?) {
        if (!json.isNullOrBlank()) PortalUiMapLogger.append(context, json)
    }
}
