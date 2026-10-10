package com.tws.portalpaciente.app.web

import android.content.Context
import org.json.JSONObject

/**
 * Prepara el tema una sola vez por documento. Se usa document-start cuando WebView lo permite;
 * el fallback manual queda disponible para WebView antiguos.
 */
object PortalInjector {
    fun loadScript(context: Context): String = try {
        val css = context.assets.open("css/portal-theme.css").bufferedReader().use { it.readText() }
        val js = context.assets.open("js/enhancer.js").bufferedReader().use { it.readText() }
        val cssLiteral = JSONObject.quote(css)
        """
        (function () {
          if (window.__hrcStyleLoaderInstalled) return;
          window.__hrcStyleLoaderInstalled = true;
          var css = $cssLiteral;
          var sheet = null;
          try {
            if (typeof CSSStyleSheet !== 'undefined' && 'adoptedStyleSheets' in Document.prototype) {
              sheet = new CSSStyleSheet();
              sheet.replaceSync(css);
              document.adoptedStyleSheets = Array.prototype.slice.call(document.adoptedStyleSheets || []).concat([sheet]);
            }
          } catch (e) { sheet = null; }
          function ensureStyle() {
            if (sheet || document.getElementById('hrc-theme')) return;
            var style = document.createElement('style');
            style.id = 'hrc-theme';
            style.textContent = css;
            (document.head || document.documentElement).appendChild(style);
          }
          window.__hrcTheme = function (enabled) {
            try {
              if (sheet) sheet.disabled = !enabled;
              else {
                var style = document.getElementById('hrc-theme');
                if (style) style.disabled = !enabled;
              }
            } catch (e) {}
          };
          ensureStyle();
          if (!sheet) {
            new MutationObserver(ensureStyle).observe(document, { childList: true, subtree: true });
          }
        })();
        $js
        """.trimIndent()
    } catch (e: Exception) {
        ""
    }
}