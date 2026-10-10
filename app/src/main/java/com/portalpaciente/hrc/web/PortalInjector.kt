package com.portalpaciente.hrc.web

import android.content.Context
import org.json.JSONObject

/** Arma el script que se inyecta en el portal: CSS del tema + `enhancer.js`. */
object PortalInjector {

    @Volatile
    private var cached: String? = null

    fun script(context: Context): String =
        cached ?: synchronized(this) {
            cached ?: build(context.applicationContext).also { cached = it }
        }

    private fun build(context: Context): String {
        val css = context.assets.open("css/portal-theme.css").bufferedReader().use { it.readText() }
        val js = context.assets.open("js/enhancer.js").bufferedReader().use { it.readText() }
        // JSONObject.quote produce un literal de cadena JS válido y escapado.
        val styleLoader = """
            (function () {
              if (window.__hrcStyleLoaded) return;
              window.__hrcStyleLoaded = true;
              var s = document.createElement('style');
              s.id = 'hrc-theme';
              s.textContent = ${JSONObject.quote(css)};
              (document.head || document.documentElement).appendChild(s);
            })();
        """.trimIndent()
        return styleLoader + "\n" + js
    }
}
