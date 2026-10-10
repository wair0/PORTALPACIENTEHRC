package com.portalpaciente.hrc.web

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.UserAgentMetadata
import androidx.webkit.UserAgentMetadata.BrandVersion
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Hace que el WebView se presente ante el portal como un Chrome móvil normal.
 *
 * Un portal puede detectar un WebView por 5 vías distintas; cada una se cubre acá:
 *  1. Cabecera `User-Agent`            -> [buildUserAgent]
 *  2. Cabecera `X-Requested-With`      -> [applyRequestedWithAllowList]  (WebView envía el package de la app)
 *  3. Client Hints (`Sec-CH-UA*`)      -> [applyUserAgentMetadata]       (deben coincidir con el UA)
 *  4. `navigator.userAgentData`, etc.  -> assets/js/ua-compat.js
 *  5. Objeto `window.chrome` ausente   -> assets/js/ua-compat.js
 *
 * Uso (en MainActivity.setupWebView, DESPUÉS de configurar settings y ANTES de loadUrl):
 *     PortalUserAgent.apply(this, webView)
 */
object PortalUserAgent {

    private const val TAG = "PortalUA"

    /** Versión mayor de Chrome a anunciar si no se puede leer la del WebView instalado. */
    private const val FALLBACK_MAJOR = 130

    /**
     * Si es true, ua-compat.js escribe en consola (visible en logcat tag PortalWebView
     * y en chrome://inspect) lo que el portal puede ver. Útil para diagnosticar.
     */
    var diagnostics = true

    fun apply(context: Context, webView: WebView) {
        val ctx = context.applicationContext
        val major = webViewMajorVersion()
        val ua = buildUserAgent(major)

        webView.settings.userAgentString = ua
        applyRequestedWithAllowList(webView.settings)
        applyUserAgentMetadata(webView.settings, major)
        injectDocumentStartScript(ctx, webView, major)

        Log.d(TAG, "UA aplicado: $ua")
    }

    /** UA de Chrome móvil con formato "reducido" (el que usa Chrome real desde v110). */
    fun buildUserAgent(major: Int = webViewMajorVersion()): String =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$major.0.0.0 Mobile Safari/537.36"

    /**
     * Usamos la versión REAL del WebView instalado: así el UA, los Client Hints y los
     * comportamientos del motor (APIs disponibles) son coherentes entre sí. Un UA que dice
     * Chrome 132 sobre un motor 120 es justamente lo que delata a los scripts de detección.
     */
    fun webViewMajorVersion(): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            val v = runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull()
            val major = v?.substringBefore('.')?.toIntOrNull()
            if (major != null && major > 0) return major
        }
        return FALLBACK_MAJOR
    }

    /** Evita que WebView agregue `X-Requested-With: com.portalpaciente.hrc` a cada request. */
    private fun applyRequestedWithAllowList(settings: WebSettings) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        } else {
            Log.w(TAG, "REQUESTED_WITH_HEADER_ALLOW_LIST no soportado: actualizá Android System WebView")
        }
    }

    /** Alinea `Sec-CH-UA`, `Sec-CH-UA-Mobile`, `Sec-CH-UA-Platform`... con el UA anunciado. */
    private fun applyUserAgentMetadata(settings: WebSettings, major: Int) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        val full = "$major.0.0.0"
        val metadata = UserAgentMetadata.Builder()
            .setBrandVersionList(
                listOf(
                    BrandVersion.Builder().setBrand("Chromium").setMajorVersion("$major").setFullVersion(full).build(),
                    BrandVersion.Builder().setBrand("Google Chrome").setMajorVersion("$major").setFullVersion(full).build(),
                    BrandVersion.Builder().setBrand("Not_A Brand").setMajorVersion("24").setFullVersion("24.0.0.0").build()
                )
            )
            .setFullVersion(full)
            .setPlatform("Android")
            .setPlatformVersion("10.0.0")
            .setMobile(true)
            .setModel("K")
            .setArchitecture("")
            .setBitness(UserAgentMetadata.BITNESS_DEFAULT)
            .build()
        runCatching { WebSettingsCompat.setUserAgentMetadata(settings, metadata) }
            .onFailure { Log.w(TAG, "No se pudo fijar UserAgentMetadata", it) }
    }

    /** Ejecuta ua-compat.js antes que cualquier script del portal. */
    private fun injectDocumentStartScript(ctx: Context, webView: WebView, major: Int) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.w(TAG, "DOCUMENT_START_SCRIPT no soportado: no se inyecta ua-compat.js")
            return
        }
        val js = ctx.assets.open("js/ua-compat.js").bufferedReader().use { it.readText() }
            .replace("__CHROME_MAJOR__", major.toString())
            .replace("__DIAGNOSTICS__", diagnostics.toString())
        WebViewCompat.addDocumentStartJavaScript(webView, js, setOf(PortalRoutes.BASE))
    }
}
