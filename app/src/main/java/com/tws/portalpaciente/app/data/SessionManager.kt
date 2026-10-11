package com.tws.portalpaciente.app.data

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import java.util.UUID

class SessionManager(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("hrc_session", Context.MODE_PRIVATE)

    @Synchronized
    fun ensureState(): String {
        val cur = prefs.getString(KEY_STATE, null)
        if (!cur.isNullOrEmpty()) return cur
        val s = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_STATE, s).apply()
        return s
    }

    fun clearState() = prefs.edit().remove(KEY_STATE).apply()

    var loggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED, false) && cookieHeader().isNotEmpty()
        set(v) { prefs.edit().putBoolean(KEY_LOGGED, v).apply() }

    fun loginUrl(): String =
        Uri.parse(Endpoints.LOGIN_PAGE).buildUpon().appendQueryParameter("state", ensureState()).build().toString()

    fun cookieHeader(): String = CookieManager.getInstance().getCookie(Endpoints.BASE) ?: ""

    fun storeSetCookie(value: String) {
        CookieManager.getInstance().setCookie(Endpoints.BASE, value)
    }

    fun flushCookies() = CookieManager.getInstance().flush()

    fun logout() {
        prefs.edit().clear().apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private companion object {
        const val KEY_STATE = "state"
        const val KEY_LOGGED = "logged_in"
    }
}
