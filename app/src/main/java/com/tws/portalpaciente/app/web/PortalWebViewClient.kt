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
    private val script: String,
    private val injectManually: Boolean
) : WebViewClient() {

    private var forceRenderScript: String? = null

    private fun getForceRenderScript(): String {
        if (forceRenderScript == null) {
            forceRenderScript = try {
                context.assets.open("js/force-render.js").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                ""
            }
        }
        return forceRenderScript ?: ""
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        if (PortalRoutes.isPortal(uri)) {
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
        viewModel.onPageStarted()
        if (injectManually && script.isNotEmpty()) view.evaluateJavascript(script, null)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (injectManually && script.isNotEmpty()) view.evaluateJavascript(script, null)

        val force = getForceRenderScript()
        if (force.isNotEmpty()) {
            view.evaluateJavascript(force, null)
        }

        viewModel.onPageFinished(url)

        url?.let {
            val uri = Uri.parse(it)
            if (uri.path?.trimEnd('/') == "/login" && uri.getQueryParameter("state").isNullOrBlank()) {
                view.postDelayed({
                    view.loadUrl(PortalRoutes.LOGIN)
                }, 500)
            }
        }
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
        FileLogger.log(context, "SSL", "Error ${error.primaryError} en host=$host url=${error.url}")

        // Continuar siempre si es el host del portal o si no se pudo determinar el host
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
