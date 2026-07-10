package com.worldbarometer.app.ui.settings

import android.util.Log
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
import com.worldbarometer.app.work.RefreshCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = SettingsStore.DEFAULT_NOTIFICATIONS_ENABLED,
    val threshold: Double = SettingsStore.DEFAULT_THRESHOLD,
    val lensId: String = LensCatalog.DEFAULT_LENS_ID,
    val lastUpdatedText: String = "—",
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
                lastUpdatedText = snapshot?.let { RelativeTime.formatAbsoluteFull(it.data.updatedAt) } ?: "No data yet",
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
            Log.d(TAG, "setLensId: $current -> $id")

            // Foreground refresh: jeden request sieciowy + RefreshCoordinator synchronizuje widget.
            // WM expedited usunięty — podwójny fetch z requestLensChangeRefresh (WB-054).
            when (val result = repository.refresh()) {
                is BarometerRepository.RefreshResult.Success -> {
                    RefreshCoordinator.onFetchSuccess(
                        ServiceLocator.applicationContext,
                        RefreshCoordinator.TriggerSource.LENS,
                    )
                    Log.d(TAG, "setLensId: foreground refresh+render done")
                }
                is BarometerRepository.RefreshResult.Failure -> {
                    BarometerWidgetUpdater.requestUpdate(ServiceLocator.applicationContext)
                    Log.d(TAG, "setLensId: offline widget render")
                }
            }
        }
    }

    companion object {
        private const val TAG = "WB-Widget"

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
