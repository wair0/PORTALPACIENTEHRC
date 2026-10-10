package com.tws.portalpaciente.app.web

import android.net.Uri
import java.util.UUID

enum class PortalRoute(val title: String) {
    LOGIN("Ingresar"),
    DASHBOARD("Inicio"),
    EXAMS("Estudios"),
    PROFILE("Mi perfil"),
    OTHER("Portal Paciente")
}

object PortalRoutes {
    const val HOST = "tws.sanluis.gob.ar"
    const val BASE = "https://$HOST"
    const val LOGIN = "$BASE/login"
    const val DASHBOARD = "$BASE/patients/dashboard"
    const val EXAMS = "$BASE/patients/exams"
    const val PROFILE = "$BASE/patients/profile"

    fun isPortal(uri: Uri): Boolean = uri.scheme == "https" && uri.host == HOST

    fun newLoginUrl(): String {
        val state = UUID.randomUUID().toString()
        return "$LOGIN?state=$state"
    }

    fun routeOf(url: String?): PortalRoute {
        if (url.isNullOrBlank()) return PortalRoute.OTHER
        val uri = Uri.parse(url)
        if (!isPortal(uri)) return PortalRoute.OTHER
        val path = uri.path.orEmpty().trimEnd('/')
        return when {
            path == "/login" || path.startsWith("/login/") -> PortalRoute.LOGIN
            path.startsWith("/patients/dashboard") -> PortalRoute.DASHBOARD
            path.startsWith("/patients/exams") -> PortalRoute.EXAMS
            path.startsWith("/patients/profile") -> PortalRoute.PROFILE
            else -> PortalRoute.OTHER
        }
    }
}
