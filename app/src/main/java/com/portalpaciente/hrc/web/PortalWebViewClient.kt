package com.portalpaciente.hrc.web

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
import com.portalpaciente.hrc.ui.PortalViewModel

/**
 * Mantiene la navegación dentro del portal, bloquea el resto y aplica el tema.
 *
 * @param injectManually true si el WebView no soporta DOCUMENT_START_SCRIPT; en ese caso el
 * script se inyecta al empezar y al terminar cada página (el script es idempotente).
 */
class PortalWebViewClient(
    private val context: Context,
    private val viewModel: PortalViewModel,
    private val script: String,
    private val injectManually: Boolean
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        // Cualquier URL del portal (incluyendo /login?state=...) se navega dentro de la app
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
        viewModel.onPageFinished(url)

        // Si terminó en /login sin state, forzar una recarga para que el servidor genere el state
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
        val host = view.url?.let { Uri.parse(it).host } ?: error.url?.let { Uri.parse(it).host }
        if (host == PortalRoutes.HOST) {
            handler.proceed()
            viewModel.onError("Aviso: certificado del portal no verificado. Continuando.")
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
