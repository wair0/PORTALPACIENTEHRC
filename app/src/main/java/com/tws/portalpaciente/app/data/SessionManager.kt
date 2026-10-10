package com.tws.portalpaciente.app.data

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

class SessionManager {

    fun configureCookies(webView: WebView) {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, true)
    }

    fun clear(webView: WebView) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)
        webView.clearHistory()
    }
}
