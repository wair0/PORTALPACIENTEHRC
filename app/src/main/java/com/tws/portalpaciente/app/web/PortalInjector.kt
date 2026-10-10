package com.tws.portalpaciente.app.web

import android.content.Context

object PortalInjector {
    fun loadScript(context: Context): String {
        return try {
            val css = context.assets.open("css/portal-theme.css").bufferedReader().use { it.readText() }
            val js = context.assets.open("js/enhancer.js").bufferedReader().use { it.readText() }
            """
            (function(){
              function inject(){
                if (!document.head) {
                  setTimeout(inject, 50);
                  return;
                }
                var s = document.createElement('style');
                s.textContent = `$css`;
                document.head.appendChild(s);
                $js
              }
              if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', inject);
              } else {
                inject();
              }
            })();
            """.trimIndent()
        } catch (e: Exception) {
            ""
        }
    }
}
