package com.tws.portalpaciente.app.web

import android.webkit.JavascriptInterface
import com.tws.portalpaciente.app.data.ProfileInfo
import com.tws.portalpaciente.app.ui.PortalViewModel

class PortalJsBridge(private val viewModel: PortalViewModel) {

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
}
