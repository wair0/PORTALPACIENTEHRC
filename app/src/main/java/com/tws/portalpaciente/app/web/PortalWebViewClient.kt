package com.tws.portalpaciente.app.web

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tws.portalpaciente.app.ui.PortalViewModel
import com.tws.portalpaciente.app.util.FileLogger

class PortalWebViewClient(
    private val context: Context,
    private val viewModel: PortalViewModel,
    private val uiMapScript: String,
    private val themeCss: String,
    private val enhancerJs: String
) : WebViewClient() {

    private fun ensureState(view: WebView, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = Uri.parse(url)
        if (!PortalRoutes.isPortal(uri)) return false
        val path = uri.path.orEmpty()
        if (!path.contains("/login")) return false
        val state = uri.getQueryParameter("state")
        if (state.isNullOrBlank()) {
            val forced = PortalRoutes.newLoginUrl()
            view.loadUrl(forced)
            return true
        }
        return false
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (PortalRoutes.isPortal(uri)) {
            if (ensureState(view, uri.toString())) return true
            return false
        }
        when (uri.scheme) {
            "tel", "mailto" -> openExternally(Intent(Intent.ACTION_VIEW, uri))
            "https" -> openExternally(Intent(Intent.ACTION_VIEW, uri))
            else -> Unit
        }
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        ensureState(view, url)
        viewModel.onPageStarted()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        try {
            view.addJavascriptInterface(PortalJsBridge(viewModel, context), "HRCBridge")
        } catch (_: Exception) {}

        // Logs y mapa UI desactivados
        viewModel.onPageFinished(url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        ensureState(view, url)
        viewModel.onPageFinished(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame) viewModel.onError(error.description?.toString().orEmpty())
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        val host = error.url?.let { Uri.parse(it).host } ?: view.url?.let { Uri.parse(it).host }
        if (host == null || host == PortalRoutes.HOST || host.endsWith(".sanluis.gob.ar")) {
            handler.proceed()
        } else {
            handler.cancel()
            viewModel.onError("No se pudo verificar la conexión segura (${error.primaryError}).")
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        viewModel.onError("La vista del portal se reinició. Tocá Reintentar.")
        return true
    }

    private fun openExternally(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
