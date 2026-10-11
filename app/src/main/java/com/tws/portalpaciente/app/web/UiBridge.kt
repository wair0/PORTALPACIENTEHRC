package com.tws.portalpaciente.app.web

import android.webkit.JavascriptInterface
import com.tws.portalpaciente.app.BuildConfig
import com.tws.portalpaciente.app.MainActivity
import com.tws.portalpaciente.app.data.Endpoints

class UiBridge(private val activity: MainActivity) {

    @JavascriptInterface fun isDebug(): Boolean = BuildConfig.DEBUG

    @JavascriptInterface fun loginMode(): String = if (Endpoints.nativeLoginConfigured) "native" else "portal"

    @JavascriptInterface fun appVersion(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface fun isLoggedIn(): Boolean = activity.session.loggedIn

    @JavascriptInterface fun login(dni: String, password: String) = activity.startLogin(dni, password)

    @JavascriptInterface fun fetch(kind: String) = activity.fetchData(kind)

    @JavascriptInterface fun openExam(id: String) = activity.openExam(id)

    @JavascriptInterface fun logout() = activity.logout()
}
