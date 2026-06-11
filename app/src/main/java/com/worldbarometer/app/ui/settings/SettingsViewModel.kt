package com.worldbarometer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.data.local.SettingsStore
import com.worldbarometer.app.data.repo.BarometerRepository
import com.worldbarometer.app.di.ServiceLocator
import com.worldbarometer.app.widget.BarometerWidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = SettingsStore.DEFAULT_NOTIFICATIONS_ENABLED,
    val threshold: Double = SettingsStore.DEFAULT_THRESHOLD,
    val lensId: String = LensCatalog.DEFAULT_LENS_ID,
    val lastUpdatedText: String = "—",
    val isChangingLens: Boolean = false,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val repository: BarometerRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(settingsStore.settings, repository.observe()) { settings, snapshot ->
            SettingsUiState(
                notificationsEnabled = settings.notificationsEnabled,
                threshold = settings.threshold,
                lensId = settings.lensId,
                lastUpdatedText = snapshot?.let { RelativeTime.format(it.data.updatedAt) } ?: "No data yet",
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setThreshold(value: Double) {
        viewModelScope.launch { settingsStore.setThreshold(value) }
    }

    fun setLensId(id: String) {
        if (!LensCatalog.isValid(id)) return
        viewModelScope.launch {
            val current = settingsStore.currentLensId()
            if (current == id) return@launch
            settingsStore.setLensId(id)
            // 1) Kraj od razu (Glance state, bez czekania na sieć).
            BarometerWidgetUpdater.requestUpdate(ServiceLocator.applicationContext, lensId = id)
            repository.refresh()
            // 2) Score/summary po opóźnieniu — Glance odrzuca drugie update() tuż po pierwszym.
            delay(BarometerWidgetUpdater.GLANCE_SECOND_UPDATE_DELAY_MS)
            BarometerWidgetUpdater.requestUpdate(ServiceLocator.applicationContext, lensId = id)
        }
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
