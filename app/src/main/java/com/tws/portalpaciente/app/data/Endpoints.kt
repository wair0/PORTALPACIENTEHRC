package com.tws.portalpaciente.app.data

/**
 * Único lugar donde se configuran las URLs del portal.
 * Las rutas vacías ("") significan "todavía no mapeada".
 */
object Endpoints {
    const val BASE = "https://tws.sanluis.gob.ar"
    const val HOST = "tws.sanluis.gob.ar"

    const val LOGIN_PAGE = "$BASE/login"
    const val LOGGED_IN_PREFIX = "/patients"

    const val LOGIN_API = ""
    const val LOGIN_USER_FIELD = "dni"
    const val LOGIN_PASS_FIELD = "password"
    const val LOGIN_SEND_STATE = true

    const val DASHBOARD = ""
    const val EXAMS = ""
    const val PROFILE = ""
    const val NOTIFICATIONS = ""
    const val EXAM_FILE = ""

    fun pathFor(kind: String): String = when (kind) {
        "dashboard" -> DASHBOARD
        "exams" -> EXAMS
        "profile" -> PROFILE
        "notifications" -> NOTIFICATIONS
        else -> ""
    }

    val nativeLoginConfigured: Boolean get() = LOGIN_API.isNotBlank()
}
