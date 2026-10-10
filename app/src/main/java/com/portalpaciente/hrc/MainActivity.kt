package com.portalpaciente.hrc

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.URLUtil
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
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.portalpaciente.hrc.data.SessionManager
import com.portalpaciente.hrc.databinding.ActivityMainBinding
import com.portalpaciente.hrc.ui.PortalViewModel
import com.portalpaciente.hrc.ui.UiState
import com.portalpaciente.hrc.web.PortalInjector
import com.portalpaciente.hrc.web.PortalJsBridge
import com.portalpaciente.hrc.web.PortalRoute
import com.portalpaciente.hrc.web.PortalRoutes
import com.portalpaciente.hrc.web.PortalWebViewClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalBadgeUtils::class)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PortalViewModel by viewModels()
    private val session = SessionManager()

    private var badge: BadgeDrawable? = null
    private var badgeAttached = false
    private var syncingBottomNav = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Datos médicos: impide capturas de pantalla y miniaturas en "recientes".
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu)
        binding.toolbar.setNavigationContentDescription(R.string.open_menu)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawer.openDrawer(GravityCompat.START)
        }

        setupWebView()
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
            binding.webView.loadUrl(PortalRoutes.LOGIN)
        }
    }

    // Full content from zip is longer; this is a placeholder to close the phase
}
