package com.worldbarometer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
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
)

class HomeViewModel(
    private val repository: BarometerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastManualRefreshMillis = 0L

    init {
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
        val now = System.currentTimeMillis()
        if (manual && now - lastManualRefreshMillis < MANUAL_THROTTLE_MS) return
        if (_uiState.value.isRefreshing) return
        if (manual) lastManualRefreshMillis = now

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = repository.refresh()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    initialLoad = false,
                    isOffline = result is BarometerRepository.RefreshResult.Failure,
                )
            }
        }
    }

    companion object {
        const val MANUAL_THROTTLE_MS = 60_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return HomeViewModel(ServiceLocator.repository) as T
            }
        }
    }
}
