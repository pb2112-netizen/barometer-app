package com.worldbarometer.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.worldbarometer.app.ui.home.MainScreen
import com.worldbarometer.app.ui.settings.SettingsScreen
import com.worldbarometer.app.ui.theme.BarometerTheme

private enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BarometerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    NotificationPermissionRequester()

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    when (screen) {
        Screen.HOME -> MainScreen(onOpenSettings = { screen = Screen.SETTINGS })
        Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
    }
}

/** Prośba o POST_NOTIFICATIONS (Android 13+) — jednorazowo przy starcie. */
@Composable
private fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* wynik nie blokuje aplikacji — Notifier i tak sprawdza uprawnienie */ }

    val requested = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!requested.value) {
            requested.value = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
