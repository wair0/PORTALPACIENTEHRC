package com.tws.portalpaciente.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.tws.portalpaciente.app.data.Endpoints
import com.tws.portalpaciente.app.data.SessionManager

/** Login del portal en WebView interno (no Chrome). Genera y mantiene state=UUID. */
class AuthActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var errorPanel: LinearLayout
    private lateinit var errorMessage: TextView
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
        progress = bar

        errorMessage = TextView(this).apply {
            text = "No se pudo cargar el inicio de sesión."
            textSize = 17f
            setTextColor(Color.rgb(35, 35, 35))
            gravity = Gravity.CENTER
        }
        val retry = Button(this).apply {
            text = "REINTENTAR"
            setOnClickListener {
                errorPanel.visibility = View.GONE
                web.visibility = View.VISIBLE
                web.loadUrl(session.loginUrl())
            }
        }
        errorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
            visibility = View.GONE
            addView(errorMessage, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(retry, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER; topMargin = 16 })
        }

        root = FrameLayout(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        root.addView(bar, FrameLayout.LayoutParams(-1, 8, Gravity.TOP))
        root.addView(errorPanel, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
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

    private fun showLoadError(message: String) {
        if (finished || isFinishing) return
        Log.e(TAG, message)
        progress?.visibility = View.GONE
        errorMessage.text = message
        web.visibility = View.GONE
        errorPanel.visibility = View.VISIBLE
    }

    private fun safeLocation(url: String): String {
        val uri = url.toUri()
        return "${uri.scheme}://${uri.host}${uri.path.orEmpty()}"
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
            val uri = request.url
            val url = uri.toString()
            if (uri.scheme != "https") {
                if (request.isForMainFrame && uri.scheme == "http") {
                    showLoadError("El portal intentó abrir una conexión HTTP no segura. Por seguridad, la app la bloqueó. Volvé a intentarlo o contactá al soporte del portal.")
                }
                Log.w(TAG, "Navegación bloqueada por esquema: ${safeLocation(url)}")
                return true
            }
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
            progress?.visibility = View.VISIBLE
            errorPanel.visibility = View.GONE
            web.visibility = View.VISIBLE
            Log.d(TAG, "Inicio de carga: ${safeLocation(url)}")
            if (loggedInUrl(url)) success()
        }

        override fun onPageFinished(view: WebView, url: String) {
            progress?.visibility = View.GONE
            session.flushCookies()
            Log.d(TAG, "Fin de carga: ${safeLocation(url)}")
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (!request.isForMainFrame) return
            showLoadError("No se pudo abrir el portal (${error.errorCode}). Revisá tu conexión e intentá nuevamente.")
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (!request.isForMainFrame) return
            if (errorResponse.statusCode >= 400) {
                showLoadError("El portal respondió con un error HTTP ${errorResponse.statusCode}. Intentá nuevamente más tarde.")
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.cancel()
            showLoadError("No se pudo verificar el certificado de seguridad del portal. La conexión fue cancelada para proteger tus datos.")
            Log.e(TAG, "Certificado SSL rechazado: primaryError=${error.primaryError}, url=${safeLocation(error.url)}")
        }
    }

    private companion object {
        const val TAG = "HRCAuth"
    }
}
