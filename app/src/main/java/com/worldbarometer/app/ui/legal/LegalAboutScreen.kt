package com.worldbarometer.app.ui.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worldbarometer.app.BuildConfig
import com.worldbarometer.app.R
import com.worldbarometer.app.core.LegalLinks
import com.worldbarometer.app.core.openEmail
import com.worldbarometer.app.core.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalAboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.legal_screen_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            LegalSection(
                title = stringResource(R.string.legal_section_about_title),
                body = stringResource(R.string.legal_section_about_body),
            )

            LegalSection(
                title = stringResource(R.string.legal_section_privacy_title),
                body = stringResource(R.string.legal_section_privacy_body),
            )
            val privacyCd = stringResource(R.string.legal_cd_open_privacy_policy)
            Text(
                text = stringResource(R.string.legal_privacy_online_copy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .semantics { contentDescription = privacyCd }
                    .clickable { openUrl(context, LegalLinks.PRIVACY_POLICY_URL) }
                    .padding(top = 6.dp),
            )

            LegalSection(
                title = stringResource(R.string.legal_section_license_title),
                body = stringResource(R.string.legal_section_license_body),
            )
            val githubCd = stringResource(R.string.legal_cd_open_github)
            Text(
                text = stringResource(R.string.legal_view_source_github),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .semantics { contentDescription = githubCd }
                    .clickable { openUrl(context, LegalLinks.GITHUB_REPO) }
                    .padding(top = 6.dp),
            )
            Spacer(Modifier.height(28.dp))

            LegalSection(
                title = stringResource(R.string.legal_section_sources_title),
                body = stringResource(R.string.legal_section_sources_body),
            )

            Text(
                text = stringResource(R.string.legal_section_contact_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.legal_section_contact_developer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val emailCd = stringResource(R.string.legal_cd_send_email)
            Text(
                text = stringResource(R.string.legal_section_contact_email),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .semantics { contentDescription = emailCd }
                    .clickable {
                        openEmail(context, LegalLinks.DEVELOPER_EMAIL)
                    }
                    .padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.legal_section_contact_github),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .semantics { contentDescription = githubCd }
                    .clickable { openUrl(context, LegalLinks.GITHUB_REPO) }
                    .padding(top = 4.dp),
            )

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(
                    R.string.legal_version_format,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegalSection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(28.dp))
}
