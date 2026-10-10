package com.tws.portalpaciente.app.web

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
import com.tws.portalpaciente.app.util.FileLogger

object PortalUserAgent {
    private const val TAG = "PortalUA"
    private const val FALLBACK_MAJOR = 130
    var diagnostics = true

    fun apply(context: Context, webView: WebView) {
        try {
            val ctx = context.applicationContext
            val major = webViewMajorVersion()
            val ua = buildUserAgent(major)
            webView.settings.userAgentString = ua
            FileLogger.log(ctx, TAG, "UA set to: $ua (major=$major)")
            applyRequestedWithAllowList(webView.settings, ctx)
            applyUserAgentMetadata(webView.settings, major, ctx)
            injectCompatibilityScripts(ctx, webView, major)
            Log.d(TAG, "UA aplicado: $ua")
            FileLogger.log(ctx, TAG, "apply() completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in apply()", e)
            FileLogger.log(context, TAG, "ERROR in apply(): ${e.message}\n${e.stackTraceToString()}")
        }
    }

    fun buildUserAgent(major: Int = webViewMajorVersion()): String =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$major.0.0.0 Mobile Safari/537.36"

    fun webViewMajorVersion(): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            val version = runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull()
            val major = version?.substringBefore('.')?.toIntOrNull()
            if (major != null && major > 0) return major
        }
        return FALLBACK_MAJOR
    }

    private fun applyRequestedWithAllowList(settings: WebSettings, ctx: Context) {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
                FileLogger.log(ctx, TAG, "X-Requested-With allow list cleared")
            } else FileLogger.log(ctx, TAG, "REQUESTED_WITH_HEADER_ALLOW_LIST not supported")
        } catch (e: Exception) {
            FileLogger.log(ctx, TAG, "Error clearing X-Requested-With: ${e.message}")
        }
    }

    private fun applyUserAgentMetadata(settings: WebSettings, major: Int, ctx: Context) {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
                FileLogger.log(ctx, TAG, "USER_AGENT_METADATA not supported")
                return
            }
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
            WebSettingsCompat.setUserAgentMetadata(settings, metadata)
            FileLogger.log(ctx, TAG, "UserAgentMetadata applied")
        } catch (e: Exception) {
            FileLogger.log(ctx, TAG, "Error setting UserAgentMetadata: ${e.message}")
        }
    }

    /**
     * Conservamos solo las compatibilidades que ya usa el login. No inyectamos CSS,
     * clasificadores visuales ni scripts de diagnóstico en document-start.
     */
    private fun injectCompatibilityScripts(ctx: Context, webView: WebView, major: Int) {
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                FileLogger.log(ctx, TAG, "DOCUMENT_START_SCRIPT no soportado; se omiten scripts tempranos")
                return
            }
            val origin = setOf(PortalRoutes.BASE)
            fun asset(name: String): String = try {
                ctx.assets.open(name).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                FileLogger.log(ctx, TAG, "Could not read $name: ${e.message}")
                ""
            }

            val tts = asset("js/tts-polyfill.js")
            if (tts.isNotEmpty()) WebViewCompat.addDocumentStartJavaScript(webView, tts, origin)

            val uaCompat = asset("js/ua-compat.js")
                .replace("__CHROME_MAJOR__", major.toString())
                .replace("__DIAGNOSTICS__", diagnostics.toString())
            if (uaCompat.isNotEmpty()) WebViewCompat.addDocumentStartJavaScript(webView, uaCompat, origin)
        } catch (e: Exception) {
            FileLogger.log(ctx, TAG, "Error injecting compatibility scripts: ${e.message}")
        }
    }
}
