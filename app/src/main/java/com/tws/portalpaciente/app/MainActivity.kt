package com.tws.portalpaciente.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.tws.portalpaciente.app.data.SessionManager
import com.tws.portalpaciente.app.web.UiBridge
import com.tws.portalpaciente.app.web.UiWebViewClient

class MainActivity : AppCompatActivity() {

    lateinit var session: SessionManager
        private set
    private lateinit var web: WebView

    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            session.loggedIn = true
            web.evaluateJavascript("if(window.HRC&&HRC.receive)HRC.receive('auth','{\"ok\":true}');", null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        CookieManager.getInstance().setAcceptCookie(true)
        web = WebView(this)
        setContentView(web)
        setupWebView()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })
        web.loadUrl("file:///android_asset/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }
        web.webViewClient = UiWebViewClient {
            Toast.makeText(this, "La vista se reinició", Toast.LENGTH_SHORT).show()
        }
        web.addJavascriptInterface(UiBridge(this), "HRCNative")
    }

    fun startLogin(dni: String, password: String) {
        authLauncher.launch(Intent(this, AuthActivity::class.java))
    }

    fun fetchData(kind: String) {
        Toast.makeText(this, "Datos de $kind (endpoints pendientes)", Toast.LENGTH_SHORT).show()
    }

    fun openExam(id: String) {
        Toast.makeText(this, "Estudio $id", Toast.LENGTH_SHORT).show()
    }

    fun logout() {
        session.logout()
        web.loadUrl("file:///android_asset/index.html")
    }
}
