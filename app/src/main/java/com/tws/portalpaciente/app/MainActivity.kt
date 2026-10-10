package com.tws.portalpaciente.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.tws.portalpaciente.app.data.SessionManager
import com.tws.portalpaciente.app.databinding.ActivityMainBinding
import com.tws.portalpaciente.app.util.DownloadsLogWriter
import com.tws.portalpaciente.app.ui.PortalViewModel
import com.tws.portalpaciente.app.ui.UiState
import com.tws.portalpaciente.app.web.DebugLog
import com.tws.portalpaciente.app.web.PortalJsBridge
import com.tws.portalpaciente.app.web.PortalRoutes
import com.tws.portalpaciente.app.web.PortalUserAgent
import com.tws.portalpaciente.app.web.PortalWebViewClient
import com.tws.portalpaciente.app.web.WebViewVersionHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: PortalViewModel by viewModels()
    private val session = SessionManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DownloadsLogWriter.initialize(applicationContext)
        DebugLog.initialize(applicationContext)
        DebugLog.add("APP", "Inicio de MainActivity; logs en Descargas/HRC-logs")
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WebViewVersionHelper.checkAndPrompt(this)
        setupWebView()
        observeState()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
            }
        })
        if (savedInstanceState == null) binding.webView.loadUrl(PortalRoutes.LOGIN)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        session.configureCookies(webView)
        CookieManager.getInstance().flush()
        WebView.setWebContentsDebuggingEnabled(true)
        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = false
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }
        PortalUserAgent.apply(this, webView)
        val uiMapScript = runCatching {
            assets.open("js/ui-map.js").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        // Sin CSS propio, sin enhancer visual, sin header/barra inferior ni scripts de forced reflow.
        webView.webViewClient = PortalWebViewClient(this, viewModel, uiMapScript)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "${consoleMessage.messageLevel()}: ${consoleMessage.message()} -- ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                Log.d("PortalWebView", msg)
                DebugLog.add("JS", msg)
                return true
            }
        }
        webView.addJavascriptInterface(PortalJsBridge(viewModel, this), "HRCBridge")
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("data:")) {
                handleDataUrlDownload(url, contentDisposition, mimeType)
            } else {
                runCatching {
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setMimeType(mimeType)
                    request.addRequestHeader("User-Agent", userAgent)
                    request.setDescription("Descargando...")
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType)
                    )
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                }.onFailure { DebugLog.add("Download", "No se pudo iniciar descarga: ${it.message}") }
            }
        }
    }

    private fun handleDataUrlDownload(dataUrl: String, contentDisposition: String?, mimeType: String?) {
        runCatching {
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val fileName = URLUtil.guessFileName(null, contentDisposition, mimeType) ?: "documento"
            DebugLog.add("Download", "Descarga data URL detectada: $fileName (${bytes.size} bytes)")
        }.onFailure { DebugLog.add("Download", "Error al procesar data URL: ${it.message}") }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> updateUi(state) }
            }
        }
    }

    private fun updateUi(state: UiState) {
        binding.errorBanner.isVisible = state.error != null
        binding.errorText.text = state.error
        binding.retryButton.setOnClickListener { binding.webView.reload() }
    }
}
