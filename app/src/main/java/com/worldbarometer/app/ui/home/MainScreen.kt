package com.worldbarometer.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldbarometer.app.R
import com.worldbarometer.app.core.BrandPalette
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.LevelPalette
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.ShortSummaryRules
import com.worldbarometer.app.core.SignificantMarkerDot
import com.worldbarometer.app.core.Sparkline
import com.worldbarometer.app.core.DASHBOARD_CHART_WIDTH_FRACTION
import com.worldbarometer.app.core.SparklineChart
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.core.resolveVisibleEventsAnchor
import com.worldbarometer.app.core.hoursAgo
import com.worldbarometer.app.core.isAllowedSourceUrl
import com.worldbarometer.app.core.openUrl
import com.worldbarometer.app.data.model.TopEvent
import com.worldbarometer.app.data.repo.BarometerRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh(manual = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh(manual = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.snapshot != null -> BarometerContent(state, onOpenSettings)
                state.initialLoad -> LoadingState(
                    countryName = LensCatalog.nameFor(state.lensId),
                    onOpenSettings = onOpenSettings,
                )
                else -> EmptyState()
            }
        }
    }
}

@Composable
private fun BarometerContent(state: HomeUiState, onOpenSettings: () -> Unit) {
    val snapshot = state.snapshot ?: return
    val data = snapshot.data
    val level = snapshot.level
    val tone = snapshot.tone
    val levelColor = LevelPalette.color(level, tone)
    val levelLabel = stringResource(LevelPalette.labelRes(level, tone))
    val countryName = data.lensNameEn ?: LensCatalog.nameFor(snapshot.lensId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isOffline || state.isStale) {
            StatusBanner(
                text = if (state.isOffline) {
                    stringResource(R.string.offline_banner)
                } else {
                    stringResource(R.string.stale_banner)
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        CountryLensChip(countryName = countryName, onClick = onOpenSettings)
        Spacer(Modifier.height(12.dp))

        val scoreText = String.format(Locale.US, "%.1f", data.globalScore)
        val eventsAnchor = resolveVisibleEventsAnchor(
            history = data.scoreHistory,
            eventsAnchorAt = data.eventsAnchorAt,
            topEvents = data.topEvents,
            shortSummary = data.shortSummary,
        )
        val showEventHeader = eventsAnchor != null
        val windowEnd = Sparkline.windowEnd(data.scoreHistory, data.updatedAt)
        val anchorDescription = eventsAnchor?.let { anchor ->
            stringResource(
                R.string.significant_peak_description,
                hoursAgo(anchor.timestamp, windowEnd),
                String.format(Locale.US, "%.1f", anchor.score),
            )
        }
        val sparklineDescription = buildString {
            append(
                stringResource(
                    R.string.sparkline_content_description,
                    snapshot.trend.name.lowercase(Locale.US),
                    scoreText,
                ),
            )
            if (anchorDescription != null) append(" $anchorDescription")
        }
        val enablePulse = !state.isStale && !state.isOffline && data.scoreHistory.size >= 3
        // a11y (WB-014 §4.6): ton niesiony tekstem opisu — TalkBack nie polega na kolorze.
        val scoreDescription = buildString {
            append(stringResource(LevelPalette.scoreDescriptionRes(level, tone), scoreText, levelLabel))
            if (anchorDescription != null) append(" $anchorDescription")
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = scoreText,
                color = levelColor,
                fontSize = 92.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = scoreDescription },
            )
            Text(
                text = "/10",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        LevelPill(label = levelLabel, color = levelColor)
        Spacer(Modifier.height(12.dp))

        SparklineChart(
            history = data.scoreHistory,
            updatedAt = data.updatedAt,
            lastPointColor = levelColor,
            enablePulse = enablePulse,
            contentDescription = sparklineDescription,
            peakIndex = eventsAnchor?.historyIndex,
            modifier = Modifier.fillMaxWidth(DASHBOARD_CHART_WIDTH_FRACTION),
        )
        Row(
            modifier = Modifier.fillMaxWidth(DASHBOARD_CHART_WIDTH_FRACTION),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.sparkline_anchor_past),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.sparkline_anchor_now),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (ShortSummaryRules.isDisplayableShortSummary(data.shortSummary)) {
            if (showEventHeader) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SignificantMarkerDot(
                            dotRadius = 3.dp,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            text = stringResource(R.string.last_significant_event),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = data.shortSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = data.shortSummary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = stringResource(R.string.updated_at_format, RelativeTime.formatAbsolute(data.updatedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        DisclaimerBox()

        Spacer(Modifier.height(20.dp))

        if (data.topEvents.isNotEmpty()) {
            EventsSection(events = data.topEvents)
        }

        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Zwijalna sekcja TOP wydarzeń. Domyślnie ZWINIĘTA (cała sekcja).
 * Po rozwinięciu pokazuje listę kart — każda karta także domyślnie zwinięta.
 */
@Composable
private fun EventsSection(events: List<TopEvent>) {
    var sectionExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { sectionExpanded = !sectionExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.top_events_title, events.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = if (sectionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (sectionExpanded) "Collapse section" else "Expand section",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (sectionExpanded) {
            Spacer(Modifier.height(8.dp))
            events.forEach { event ->
                EventCard(event)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun LevelPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EventCard(event: TopEvent) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            ScoreBadge(score = event.score, sentiment = Tone.fromString(event.sentiment))
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // Opis i źródła widoczne dopiero po rozwinięciu karty (domyślnie zwinięta).
                if (expanded) {
                    if (event.summary.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = event.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (event.sources.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.sources_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        event.sources.forEach { source ->
                            val matchedLink = event.sourceLinks.firstOrNull {
                                it.name == source && it.url.isAllowedSourceUrl()
                            }
                            if (matchedLink != null) {
                                val readCd = stringResource(R.string.event_source_read_cd, source)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { openUrl(context, matchedLink.url) }
                                        .semantics { contentDescription = readCd },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = source,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                Text(
                                    text = source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DisclaimerBox() {
    val disclaimerColor = if (isSystemInDarkTheme()) {
        Color(0xFF1D2328)
    } else {
        BrandPalette.warmCream
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = disclaimerColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Text(
            text = stringResource(R.string.dashboard_ai_disclaimer),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScoreBadge(score: Double, sentiment: Tone) {
    // Kolor z pary (score eventu x sentiment eventu) — nie z globalnego tone (AC-5).
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LevelPalette.eventBadgeColor(score, sentiment))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", score),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun StatusBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 2.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingState(
    countryName: String,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CountryLensChip(countryName = countryName, onClick = onOpenSettings)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.empty_state_pull_to_refresh),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
