package com.tws.portalpaciente.app.web

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class UiWebViewClient(private val onCrash: () -> Unit) : WebViewClient() {

    private fun local(url: String) = url.startsWith("file:///android_asset/")

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        !local(request.url.toString())

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        if (local(request.url.toString()) || request.url.scheme == "data") null
        else WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onCrash()
        return true
    }
}
