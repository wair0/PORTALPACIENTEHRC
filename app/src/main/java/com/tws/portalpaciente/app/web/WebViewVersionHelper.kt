package com.tws.portalpaciente.app.web

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog

object WebViewVersionHelper {

    fun checkAndPrompt(context: Context) {
        val info = getWebViewInfo(context)
        if (info.versionCode < 120_000_000) {
            AlertDialog.Builder(context)
                .setTitle("Actualizar WebView")
                .setMessage(
                    "Tu WebView del sistema es antigua (${info.packageName} ${info.versionName}).\n\n" +
                    "Actualizalo desde Play Store para mejorar la carga del portal."
                )
                .setPositiveButton("Abrir Play Store") { _, _ ->
                    openPlayStore(context, info.packageName)
                }
                .setNegativeButton("Después", null)
                .show()
        }
    }

    private data class Info(val packageName: String, val versionName: String, val versionCode: Long)

    private fun getWebViewInfo(context: Context): Info {
        val pm = context.packageManager
        val packages = listOf(
            "com.google.android.webview",
            "com.android.webview",
            "com.google.android.webview.beta",
            "com.android.chrome"
        )
        for (pkg in packages) {
            try {
                val pi = pm.getPackageInfo(pkg, 0)
                val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
                return Info(pkg, pi.versionName ?: "?", code)
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return Info("unknown", "?", 0)
    }

    private fun openPlayStore(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(web)
            }
    }
}
