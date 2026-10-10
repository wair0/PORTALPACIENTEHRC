package com.tws.portalpaciente.app.util

import android.content.Context

/**
 * El antiguo portal_debug.log ya no se genera.
 * El diagnóstico visual ahora se guarda de forma separada en portal_ui_map.log.
 */
object FileLogger {
    @Suppress("UNUSED_PARAMETER")
    fun log(context: Context, tag: String, message: String) {
        // Intencionalmente desactivado: no crear ni seguir anexando portal_debug.log.
    }

    @Suppress("UNUSED_PARAMETER")
    fun clear(context: Context) {
        // No se crea ni se modifica el registro antiguo.
    }
}
