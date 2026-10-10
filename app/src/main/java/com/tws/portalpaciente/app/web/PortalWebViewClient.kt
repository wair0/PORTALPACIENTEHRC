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
    private val uiMapScript: String
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (PortalRoutes.isPortal(uri)) return false
        when (uri.scheme) {
            "tel", "mailto" -> openExternally(Intent(Intent.ACTION_VIEW, uri))
            "https" -> openExternally(Intent(Intent.ACTION_VIEW, uri))
            else -> Unit
        }
        return true
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        DebugLog.add("PAGE", "start ${url?.substringBefore('?')}")
        viewModel.onPageStarted()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        DebugLog.add("PAGE", "finish ${url?.substringBefore('?')}")
        // El mapa es de solo lectura y se inicia después de que el portal termine de cargar.
        if (uiMapScript.isBlank()) {
            DebugLog.add("UI_MAP", "No se cargó assets/js/ui-map.js: el script está vacío")
        } else if (url?.startsWith(PortalRoutes.BASE) == true) {
            DebugLog.add("UI_MAP", "Inyectando mapa UI en la página del portal; script=${uiMapScript.length} caracteres")
            view.evaluateJavascript(uiMapScript) { result ->
                DebugLog.add("UI_MAP", "evaluateJavascript finalizó; resultado=${result ?: "null"}")
            }
        } else {
            DebugLog.add("UI_MAP", "Se omite mapa UI para URL fuera del portal")
        }
        viewModel.onPageFinished(url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        DebugLog.add("NAV", "history ${url?.substringBefore('?')}")
        viewModel.onPageFinished(url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        DebugLog.add("ERR", "${if (request.isForMainFrame) "MAIN " else ""}${request.url} -> ${error.errorCode} ${error.description}")
        if (request.isForMainFrame) viewModel.onError(error.description?.toString().orEmpty())
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        DebugLog.add("SSL", "${error.primaryError} ${error.url}")
        val host = error.url?.let { Uri.parse(it).host } ?: view.url?.let { Uri.parse(it).host }
        FileLogger.log(context, "SSL", "Error ${error.primaryError} en host=$host url=${error.url}")
        if (host == null || host == PortalRoutes.HOST || host.endsWith(".sanluis.gob.ar")) {
            handler.proceed()
        } else {
            handler.cancel()
            viewModel.onError("No se pudo verificar la conexión segura (${error.primaryError}).")
        }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        DebugLog.add("RENDER", "render process gone, crash=${detail.didCrash()}")
        viewModel.onError("La vista del portal se reinició. Tocá Reintentar.")
        return true
    }

    private fun openExternally(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
