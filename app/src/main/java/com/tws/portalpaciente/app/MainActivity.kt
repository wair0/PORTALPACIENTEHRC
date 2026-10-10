package com.tws.portalpaciente.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tws.portalpaciente.app.data.SessionManager
import com.tws.portalpaciente.app.databinding.ActivityMainBinding
import com.tws.portalpaciente.app.ui.PortalViewModel
import com.tws.portalpaciente.app.ui.UiState
import com.tws.portalpaciente.app.util.FileLogger
import com.tws.portalpaciente.app.web.DebugLog
import com.tws.portalpaciente.app.web.PortalInjector
import com.tws.portalpaciente.app.web.PortalJsBridge
import com.tws.portalpaciente.app.web.PortalRoute
import com.tws.portalpaciente.app.web.PortalRoutes
import com.tws.portalpaciente.app.web.PortalUserAgent
import com.tws.portalpaciente.app.web.PortalWebViewClient
import com.tws.portalpaciente.app.web.WebViewVersionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalBadgeUtils::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PortalViewModel by viewModels()
    private val session = SessionManager()
    private val handler = Handler(Looper.getMainLooper())

    private var badge: BadgeDrawable? = null
    private var badgeAttached = false
    private var syncingBottomNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.log(this, "MainActivity", "onCreate started")

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FileLogger.log(this, "MainActivity", "setContentView done")

        WebViewVersionHelper.checkAndPrompt(this)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu)
        binding.toolbar.setNavigationContentDescription(R.string.open_menu)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawer.openDrawer(GravityCompat.START)
        }

        setupWebView()
        setupDebugPanel()
        setupDrawer()
        setupBottomNav()
        observeState()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawer.isDrawerOpen(GravityCompat.START) ->
                        binding.drawer.closeDrawer(GravityCompat.START)
                    binding.webView.canGoBack() -> binding.webView.goBack()
                    else -> finish()
                }
            }
        })

        if (savedInstanceState == null) {
            FileLogger.log(this, "MainActivity", "loading LOGIN url")
            binding.webView.loadUrl(PortalRoutes.LOGIN)
        }
    }

    private fun setupDebugPanel() {
        binding.debugFab.setOnClickListener {
            binding.debugText.text = DebugLog.dump()
            binding.debugPanel.isVisible = true
        }
        binding.debugClose.setOnClickListener { binding.debugPanel.isVisible = false }
        binding.debugRefresh.setOnClickListener { binding.debugText.text = DebugLog.dump() }
        binding.debugCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("log", DebugLog.dump()))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        FileLogger.log(this, "MainActivity", "setupWebView started")
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

        val script = ""
        val injectManually = false

        webView.webViewClient = PortalWebViewClient(this, viewModel, script, injectManually)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val msg = "${consoleMessage.messageLevel()}: ${consoleMessage.message()} -- ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}"
                Log.d("PortalWebView", msg)
                FileLogger.log(this@MainActivity, "WebConsole", msg)
                DebugLog.add("JS", msg)
                return true
            }
        }
        webView.addJavascriptInterface(PortalJsBridge(viewModel), "HRCBridge")

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (url.startsWith("data:")) {
                handleDataUrlDownload(url, contentDisposition, mimeType)
            } else {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Descargando...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
            }
        }
        FileLogger.log(this, "MainActivity", "setupWebView finished")
    }

    private fun handleDataUrlDownload(dataUrl: String, contentDisposition: String?, mimeType: String?) {
        runCatching {
            val base64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val fileName = URLUtil.guessFileName(null, contentDisposition, mimeType) ?: "download"
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadRoute(PortalRoutes.DASHBOARD)
                R.id.nav_exams -> loadRoute(PortalRoutes.EXAMS)
                R.id.nav_profile -> loadRoute(PortalRoutes.PROFILE)
                R.id.nav_logout -> confirmLogout()
            }
            binding.drawer.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (syncingBottomNav) return@setOnItemSelectedListener true
            when (item.itemId) {
                R.id.nav_home -> loadRoute(PortalRoutes.DASHBOARD)
                R.id.nav_exams_bottom -> loadRoute(PortalRoutes.EXAMS)
                R.id.nav_profile_bottom -> loadRoute(PortalRoutes.PROFILE)
            }
            true
        }
    }

    private fun loadRoute(url: String) {
        binding.webView.loadUrl(url)
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Querés salir del portal?")
            .setPositiveButton("Salir") { _, _ ->
                session.clear(binding.webView)
                viewModel.logoutHandled()
                binding.webView.loadUrl(PortalRoutes.LOGIN)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: UiState) {
        val isLogin = state.route == PortalRoute.LOGIN || !state.loggedIn

        binding.toolbar.isVisible = !isLogin
        binding.bottomNav.isVisible = !isLogin
        binding.drawer.setDrawerLockMode(
            if (isLogin) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
        )

        binding.progressBar.isVisible = state.loading && !isLogin
        binding.errorBanner.isVisible = state.error != null
        binding.errorText.text = state.error
        binding.retryButton.setOnClickListener {
            binding.webView.reload()
        }

        if (!isLogin) {
            supportActionBar?.title = state.route.title

            val header = binding.navView.getHeaderView(0)
            val nameView = header.findViewById<TextView>(R.id.profile_name)
            val photoView = header.findViewById<ImageView>(R.id.profile_photo)
            nameView.text = state.profile?.fullName ?: "Paciente"

            syncingBottomNav = true
            val itemId = when (state.route) {
                PortalRoute.DASHBOARD -> R.id.nav_home
                PortalRoute.EXAMS -> R.id.nav_exams_bottom
                PortalRoute.PROFILE -> R.id.nav_profile_bottom
                else -> null
            }
            itemId?.let { binding.bottomNav.selectedItemId = it }
            syncingBottomNav = false

            updateBadge(state.unread)
        }
    }

    private fun updateBadge(count: Int) {
        if (count > 0) {
            if (badge == null) badge = BadgeDrawable.create(this)
            badge?.number = count
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                binding.webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
