package com.portalpaciente.hrc.ui

import androidx.lifecycle.ViewModel
import com.portalpaciente.hrc.data.ProfileInfo
import com.portalpaciente.hrc.web.PortalRoute
import com.portalpaciente.hrc.web.PortalRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val loading: Boolean = true,
    val route: PortalRoute = PortalRoute.LOGIN,
    val loggedIn: Boolean = false,
    val profile: ProfileInfo? = null,
    val unread: Int = 0,
    val error: String? = null,
    val pendingLogout: Boolean = false
)

class PortalViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPageStarted() = _uiState.update { it.copy(loading = true, error = null) }

    fun onPageFinished(url: String?) {
        val route = PortalRoutes.routeOf(url)
        _uiState.update {
            it.copy(
                loading = false,
                route = route,
                loggedIn = route != PortalRoute.LOGIN && route != PortalRoute.OTHER
            )
        }
    }

    fun onError(message: String) = _uiState.update { it.copy(loading = false, error = message) }

    fun onProfile(name: String, photo: String, unread: Int) {
        _uiState.update {
            it.copy(profile = ProfileInfo(name, photo), unread = unread)
        }
    }

    fun requestLogout() = _uiState.update { it.copy(pendingLogout = true) }
    fun logoutHandled() = _uiState.update { it.copy(pendingLogout = false, profile = null, unread = 0, loggedIn = false) }
}
