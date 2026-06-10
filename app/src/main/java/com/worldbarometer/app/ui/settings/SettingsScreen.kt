package com.worldbarometer.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ustawienia", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Powiadomienia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Alert przy wzroście powyżej progu (limit: raz na 3 h).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Próg powiadomień",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = String.format(Locale.US, "Wyślij alert, gdy ocena ≥ %.1f", state.threshold),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.threshold.toFloat(),
                onValueChange = { viewModel.setThreshold(it.toDouble()) },
                valueRange = 1f..10f,
                // 18 kroków co 0.5 w zakresie 1..10
                steps = 17,
                enabled = state.notificationsEnabled,
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Ostatnia aktualizacja danych",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = state.lastUpdatedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            PrivacyPolicySection()
        }
    }
}

@Composable
private fun PrivacyPolicySection() {
    Text(
        text = "Polityka prywatności",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = PRIVACY_POLICY,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val PRIVACY_POLICY =
    "World Barometer nie zbiera, nie przechowuje ani nie udostępnia żadnych danych " +
        "osobowych. Aplikacja nie wymaga logowania, nie korzysta z lokalizacji, kontaktów " +
        "ani identyfikatorów reklamowych.\n\n" +
        "Jedyne połączenie sieciowe to pobranie publicznego pliku z wynikiem " +
        "(barometer.json) przez HTTPS. Nie wysyłamy żadnych informacji o Tobie ani o " +
        "Twoim urządzeniu. Ustawienia (próg, przełącznik powiadomień) i ostatni wynik są " +
        "zapisywane wyłącznie lokalnie na urządzeniu.\n\n" +
        "Uprawnienia: internet (pobranie danych) oraz powiadomienia (Android 13+). " +
        "Powiadomienia są generowane lokalnie na telefonie — nie używamy push z serwera.\n\n" +
        "Treści pochodzą z publicznych źródeł RSS (m.in. BBC, Al Jazeera, The Guardian) " +
        "i są oceniane automatycznie. Mają charakter informacyjny i mogą zawierać błędy."
