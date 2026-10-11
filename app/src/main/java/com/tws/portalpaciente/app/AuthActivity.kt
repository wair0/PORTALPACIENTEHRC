package com.tws.portalpaciente.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.tws.portalpaciente.app.data.Endpoints
import com.tws.portalpaciente.app.data.SessionManager

/** Login del portal en WebView interno (no Chrome). Genera y mantiene state=UUID. */
class AuthActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var web: WebView
    private var finished = false
    private var progress: ProgressBar? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        web.webViewClient = AuthClient()

        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
        }
        setContentView(FrameLayout(this).apply {
            addView(web, ViewGroup.LayoutParams(-1, -1))
            addView(bar, FrameLayout.LayoutParams(-1, 8))
        })
        progress = bar
        web.loadUrl(session.loginUrl())
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack()
        else {
            setResult(RESULT_CANCELED)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }

    private fun success() {
        if (finished) return
        finished = true
        session.loggedIn = true
        session.clearState()
        session.flushCookies()
        setResult(RESULT_OK)
        finish()
    }

    private inner class AuthClient : WebViewClient() {

        private fun needsState(url: String): String? {
            val u = url.toUri()
            if (u.host != Endpoints.HOST) return null
            if (u.path?.trimEnd('/') != "/login") return null
            if (!u.getQueryParameter("state").isNullOrEmpty()) return null
            return u.buildUpon().appendQueryParameter("state", session.ensureState()).build().toString()
        }

        private fun loggedInUrl(url: String): Boolean {
            val u = url.toUri()
            return u.host == Endpoints.HOST && (u.path ?: "").startsWith(Endpoints.LOGGED_IN_PREFIX)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (request.url.scheme != "https") return true
            if (loggedInUrl(url)) {
                success()
                return true
            }
            needsState(url)?.let {
                view.loadUrl(it)
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            progress?.visibility = android.view.View.VISIBLE
            if (loggedInUrl(url)) success()
        }

        override fun onPageFinished(view: WebView, url: String) {
            progress?.visibility = android.view.View.GONE
            session.flushCookies()
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            if (loggedInUrl(url)) success()
        }
    }
}
