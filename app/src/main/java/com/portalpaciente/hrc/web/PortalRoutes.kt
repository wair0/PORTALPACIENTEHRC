package com.portalpaciente.hrc.web

import android.net.Uri

/** Pantallas conocidas del portal. [title] se muestra en la barra superior nativa. */
enum class PortalRoute(val title: String) {
    LOGIN("Ingresar"),
    DASHBOARD("Inicio"),
    EXAMS("Estudios"),
    PROFILE("Mi perfil"),
    OTHER("Portal Paciente")
}

/**
 * Rutas del portal oficial. IMPORTANTE: el login se carga SIEMPRE sin parámetro `state`.
 * El servidor lo genera en cada carga (redirección) y la WebView la sigue sola; nunca se
 * guarda ni se reutiliza un `state` viejo.
 */
object PortalRoutes {
    const val HOST = "tws.sanluis.gob.ar"
    const val BASE = "https://$HOST"
    const val LOGIN = "$BASE/login"
    const val DASHBOARD = "$BASE/patients/dashboard"
    const val EXAMS = "$BASE/patients/exams"
    const val PROFILE = "$BASE/patients/profile"

    /** Solo HTTPS y solo el host del portal se navegan dentro de la app. */
    fun isPortal(uri: Uri): Boolean = uri.scheme == "https" && uri.host == HOST

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
