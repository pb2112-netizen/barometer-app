package com.worldbarometer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = SettingsStore.DEFAULT_NOTIFICATIONS_ENABLED,
    val threshold: Double = SettingsStore.DEFAULT_THRESHOLD,
    val lastUpdatedText: String = "—",
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    repository: BarometerRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsStore.settings, repository.observe()) { settings, snapshot ->
            SettingsUiState(
                notificationsEnabled = settings.notificationsEnabled,
                threshold = settings.threshold,
                lastUpdatedText = snapshot?.let { RelativeTime.format(it.data.updatedAt) } ?: "brak danych",
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setThreshold(value: Double) {
        viewModelScope.launch { settingsStore.setThreshold(value) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SettingsViewModel(
                    ServiceLocator.settingsStore,
                    ServiceLocator.repository,
                ) as T
            }
        }
    }
}
