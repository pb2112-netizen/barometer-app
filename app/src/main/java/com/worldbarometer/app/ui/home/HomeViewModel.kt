package com.worldbarometer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import com.worldbarometer.app.work.RefreshCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val snapshot: BarometerRepository.Snapshot? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
    val initialLoad: Boolean = true,
    val lensId: String = LensCatalog.DEFAULT_LENS_ID,
)

class HomeViewModel(
    private val repository: BarometerRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastManualRefreshMillis = 0L

    init {
        viewModelScope.launch {
            settingsStore.lensId.collect { lensId ->
                _uiState.update { it.copy(lensId = lensId) }
            }
        }
        viewModelScope.launch {
            repository.observe().collect { snapshot ->
                _uiState.update {
                    it.copy(
                        snapshot = snapshot,
                        isStale = snapshot?.isStale(System.currentTimeMillis()) ?: false,
                    )
                }
            }
        }
        refresh(manual = false)
    }

    /**
     * @param manual true dla pull-to-refresh (throttling min. 60 s wg SPEC_MVP §3).
     */
    fun refresh(manual: Boolean) {
        if (_uiState.value.isRefreshing) return

        val now = System.currentTimeMillis()
        val throttled = manual && now - lastManualRefreshMillis < MANUAL_THROTTLE_MS
        if (manual && !throttled) lastManualRefreshMillis = now

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (throttled) {
                // Throttling (min. 60 s) — pokaż krótki feedback zamiast „martwego" gestu,
                // ale nie odpytuj sieci.
                delay(700)
                _uiState.update { it.copy(isRefreshing = false) }
            } else {
                val result = repository.refresh()
                if (result is BarometerRepository.RefreshResult.Success) {
                    val source = if (manual) {
                        RefreshCoordinator.TriggerSource.MANUAL
                    } else {
                        RefreshCoordinator.TriggerSource.APP
                    }
                    RefreshCoordinator.onFetchSuccess(ServiceLocator.applicationContext, source)
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        initialLoad = false,
                        isOffline = result is BarometerRepository.RefreshResult.Failure,
                    )
                }
            }
        }
    }

    companion object {
        const val MANUAL_THROTTLE_MS = 60_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return HomeViewModel(
                    ServiceLocator.repository,
                    ServiceLocator.settingsStore,
                ) as T
            }
        }
    }
}
