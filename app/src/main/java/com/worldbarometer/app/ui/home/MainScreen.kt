package com.worldbarometer.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worldbarometer.app.R
import com.worldbarometer.app.core.LensCatalog
import com.worldbarometer.app.core.LevelPalette
import com.worldbarometer.app.core.RelativeTime
import com.worldbarometer.app.core.SignificantMarkerDot
import com.worldbarometer.app.core.DASHBOARD_CHART_WIDTH_FRACTION
import com.worldbarometer.app.core.SparklineChart
import com.worldbarometer.app.core.Tone
import com.worldbarometer.app.core.isAllowedSourceUrl
import com.worldbarometer.app.core.openUrl
import com.worldbarometer.app.data.model.MostSignificantEvent
import com.worldbarometer.app.data.model.TopEvent
import com.worldbarometer.app.data.repo.BarometerRepository
import java.time.Duration
import java.time.Instant
import java.util.Locale

private val HeroFontFamily = FontFamily(
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
)

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

@OptIn(ExperimentalMaterial3Api::class)
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
        val mse = data.mostSignificantEvent
        val sparklineDescription = stringResource(
            R.string.sparkline_content_description,
            snapshot.trend.name.lowercase(Locale.US),
            scoreText,
        )
        val enablePulse = !state.isStale && !state.isOffline && data.scoreHistory.size >= 3
        // a11y (WB-014 §4.6): ton niesiony tekstem opisu — TalkBack nie polega na kolorze.
        val scoreDescription = stringResource(LevelPalette.scoreDescriptionRes(level, tone), scoreText, levelLabel)
        // WB-069: osobny arkusz dla MSE. Wcześniej jedyny tap (na cyfrę) pokazywał lensowe
        // `rationale`, czyli uzasadnienie global_score dla dominanta BIEŻĄCEGO cyklu — inne
        // wydarzenie niż MSE, ilekroć champion siedzi poza widocznym top-3.
        var showMseSheet by remember { mutableStateOf(false) }
        val scoreRowModifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = scoreDescription
        }
        val heroShape = RoundedCornerShape(24.dp)
        val heroBaseColor = MaterialTheme.colorScheme.surface
        val isDarkTheme = isSystemInDarkTheme()
        val heroGradient = remember(levelColor, heroBaseColor, isDarkTheme) {
            val accentAlpha = if (isDarkTheme) 0.07f else 0.12f
            val fadeAlpha = if (isDarkTheme) 0.02f else 0.035f
            Brush.linearGradient(
                colors = listOf(
                    levelColor.copy(alpha = accentAlpha).compositeOver(heroBaseColor),
                    levelColor.copy(alpha = fadeAlpha).compositeOver(heroBaseColor),
                    heroBaseColor,
                ),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient, heroShape),
            color = Color.Transparent,
            shape = heroShape,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = scoreRowModifier,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = scoreText,
                        color = levelColor,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = HeroFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 92.sp,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.02).em,
                        ),
                    )
                    Text(
                        text = "/10",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = HeroFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                            letterSpacing = (-0.02).em,
                        ),
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }

                LevelPill(label = levelLabel, color = levelColor)
                Spacer(Modifier.height(12.dp))

                if (data.scoreHistory.size >= 2) {
                    SparklineChart(
                        history = data.scoreHistory,
                        updatedAt = data.updatedAt,
                        lastPointColor = levelColor,
                        enablePulse = enablePulse,
                        contentDescription = sparklineDescription,
                        // WB-068: żółty odcinek osi czasu = jak długo trwa bieżące MSE.
                        mseDetectedAt = mse?.detectedAt,
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
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(DASHBOARD_CHART_WIDTH_FRACTION)
                            .height(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.sparkline_collecting_history),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // WB-060: "Most significant event" — blok ZAWSZE widoczny (bez warunku sticky/nowosc).
                val mseDescription = stringResource(
                    R.string.mse_description,
                    mse?.label ?: stringResource(R.string.mse_gathering_data),
                    mse?.let { String.format(Locale.US, "%.1f", it.score) } ?: "—",
                    mse?.let { detectedAgoPhrase(it.detectedAt) } ?: "unknown",
                )
                // WB-067: widoczny znacznik czasu ("5h ago"). Dotąd ta fraza istniała
                // wyłącznie w opisie a11y — wzrokowo nie było jej nigdzie.
                val mseAgeText = RelativeTime.formatShortAgo(mse?.detectedAt)
                val mseSheetCd = stringResource(R.string.mse_sheet_cd)
                val mseBlockModifier = if (mse != null) {
                    Modifier
                        .fillMaxWidth()
                        .clickable { showMseSheet = true }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            contentDescription = "$mseDescription. $mseSheetCd"
                        }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { contentDescription = mseDescription }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = mseBlockModifier,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // WB-067: stały żółty (MseMarkerColor) — bez tonowania wg score/sentymentu.
                        SignificantMarkerDot(
                            dotRadius = 3.dp,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            text = stringResource(R.string.most_significant_event),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (mseAgeText != null) {
                            Text(
                                text = " ${stringResource(R.string.mse_age_separator)} $mseAgeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alpha(0.75f),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // WB-065/WB-067: twarde dwie linie. Budżet znaków pilnuje silnik, tu jest
                    // siatka bezpieczeństwa: przy powiększonej czcionce systemowej font schodzi
                    // w dół zamiast zawijać się na trzecią linię albo uciąć tekst wielokropkiem.
                    AutoShrinkText(
                        text = mse?.label ?: stringResource(R.string.mse_gathering_data),
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (mse != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.mse_tap_for_details),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.6f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (showMseSheet && mse != null) {
            MostSignificantEventSheet(
                mse = mse,
                onDismiss = { showMseSheet = false },
            )
        }

        if (data.topEvents.isNotEmpty()) {
            EventsSection(events = data.topEvents)
            Spacer(Modifier.height(16.dp))
        }

        DisclaimerBox()
        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.updated_at_format, RelativeTime.formatAbsolute(data.updatedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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

/**
 * WB-069: szczegóły MSE — champion okna 24h, który może w bieżącym cyklu w ogóle nie
 * być w top-3 (dominant `rationale` opisuje coś innego niż MSE w takim przypadku).
 *
 * `summary` bywa puste na cache sprzed WB-064 — arkusz zostaje użyteczny (etykieta,
 * score, czas wykrycia), po prostu bez akapitu opisu. WB-073: `sourceLinks` analogicznie
 * bywa puste na cache sprzed tego zadania — sekcja źródeł wtedy się nie pokazuje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MostSignificantEventSheet(
    mse: MostSignificantEvent,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignificantMarkerDot(dotRadius = 3.dp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = stringResource(R.string.mse_sheet_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = mse.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreBadge(score = mse.score, sentiment = Tone.fromString(mse.sentiment))
                Spacer(Modifier.size(10.dp))
                val agoPhrase = detectedAgoFullPhrase(mse.detectedAt)
                if (agoPhrase != null) {
                    Text(
                        text = stringResource(R.string.mse_sheet_detected, agoPhrase),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (mse.summary.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = mse.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // Tytuł źródłowy RSS — tylko gdy wnosi coś ponad etykietę.
            if (mse.title.isNotBlank() && !mse.title.equals(mse.label, ignoreCase = true)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = mse.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.8f),
                )
            }
            // WB-073: link źródłowy — ten sam wzorzec co EventCard. Sticky w ledgerze
            // (silnik), więc dostępny nawet gdy champion wypadł z bieżących top_events.
            if (mse.sourceLinks.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.sources_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                mse.sourceLinks.forEach { link ->
                    val readCd = stringResource(R.string.event_source_read_cd, link.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { openUrl(context, link.url) }
                            .semantics { contentDescription = readCd },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = link.name,
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
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.score_rationale_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * WB-067: tekst twardo ograniczony do [maxLines], który przy braku miejsca ZMNIEJSZA font
 * zamiast zawijać się dalej albo ucinać wielokropkiem.
 *
 * Silnik pilnuje budżetu znaków (WB-065), ale nie zna skali czcionki systemowej: przy
 * `fontScale` 1.3 nawet etykieta w budżecie potrzebuje trzech linii. Zmniejszamy font
 * o 1 sp na próbę, aż tekst się zmieści albo dobijemy do [minFontSize]. Rysowanie jest
 * wstrzymane do ustalenia rozmiaru, żeby nie było widać „skakania" tekstu.
 */
@Composable
private fun AutoShrinkText(
    text: String,
    baseStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    minFontSize: TextUnit = 13.sp,
) {
    val baseSize = baseStyle.fontSize
    // Styl bez konkretnego rozmiaru — nie ma czego zmniejszać, rysuj normalnie.
    val canShrink = baseSize.isSp
    var fontSize by remember(text, baseSize) { mutableStateOf(baseSize) }
    var settled by remember(text, baseSize) { mutableStateOf(!canShrink) }

    Text(
        text = text,
        style = if (canShrink) baseStyle.copy(fontSize = fontSize) else baseStyle,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent { if (settled) drawContent() },
        onTextLayout = { result ->
            if (!settled) {
                if (result.hasVisualOverflow && fontSize.value > minFontSize.value) {
                    fontSize = (fontSize.value - 1f).sp
                } else {
                    settled = true
                }
            }
        },
    )
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
            fontFamily = HeroFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Pełna forma słowna czasu wykrycia dla a11y (WB-059), np. "12 hours ago", "less than 1 hour ago".
 * WB-060: cap na 24h+ ("more than a day ago") — zgodny z RelativeTime.formatShortAgo.
 * Widoczny tekst karty pozostaje krótki (RelativeTime.formatShortAgo) — to tylko opis dostępności.
 */
private fun detectedAgoFullPhrase(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String? {
    val instant = RelativeTime.parseOrNull(isoUtc) ?: return null
    val now = Instant.ofEpochMilli(nowMillis)
    if (instant.isAfter(now)) return "less than 1 hour ago"
    val hours = Duration.between(instant, now).toHours()
    return when {
        hours < 1 -> "less than 1 hour ago"
        hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        else -> "more than a day ago"
    }
}

/** WB-060: jak [detectedAgoFullPhrase], bez końcowego "ago" (używane w szablonie mse_description). */
private fun detectedAgoPhrase(isoUtc: String?, nowMillis: Long = System.currentTimeMillis()): String? =
    detectedAgoFullPhrase(isoUtc, nowMillis)?.removeSuffix(" ago")

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
        Row(
            modifier = Modifier
                .padding(14.dp)
                .defaultMinSize(minHeight = 64.dp), // WB-059: miejsce na badge + label z paddingiem
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreBadge(score = event.score, sentiment = Tone.fromString(event.sentiment))
                val agoText = RelativeTime.formatShortAgo(event.detectedAt)
                if (agoText != null) {
                    Spacer(Modifier.height(4.dp))
                    val agoFullPhrase = detectedAgoFullPhrase(event.detectedAt)
                    val agoCd = agoFullPhrase?.let { stringResource(R.string.event_detected_a11y, it) }
                    Text(
                        text = agoText,
                        style = MaterialTheme.typography.labelSmall,
                        // Neutralny, spokojny kolor — niezależny od score/sentymentu eventu (WB-059).
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (agoCd != null) {
                            Modifier.semantics { contentDescription = agoCd }
                        } else {
                            Modifier
                        },
                    )
                }
            }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(R.string.dashboard_ai_disclaimer_icon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = stringResource(R.string.dashboard_ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
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
    val shimmerOffset = remember { Animatable(-300f) }
    LaunchedEffect(Unit) {
        while (true) {
            shimmerOffset.snapTo(-300f)
            shimmerOffset.animateTo(
                targetValue = 1_200f,
                animationSpec = tween(durationMillis = 1_300, easing = LinearEasing),
            )
        }
    }
    val placeholderBase = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val placeholderHighlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(placeholderBase, placeholderHighlight, placeholderBase),
        start = Offset(shimmerOffset.value - 300f, 0f),
        end = Offset(shimmerOffset.value, 180f),
    )
    val loadingDescription = stringResource(R.string.dashboard_loading)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountryLensChip(countryName = countryName, onClick = onOpenSettings)
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = loadingDescription },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShimmerPlaceholder(
                    brush = shimmerBrush,
                    modifier = Modifier.width(176.dp).height(92.dp),
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(10.dp))
                ShimmerPlaceholder(
                    brush = shimmerBrush,
                    modifier = Modifier.width(104.dp).height(24.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(24.dp))
                ShimmerPlaceholder(
                    brush = shimmerBrush,
                    modifier = Modifier
                        .fillMaxWidth(DASHBOARD_CHART_WIDTH_FRACTION)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(28.dp))
                ShimmerPlaceholder(
                    brush = shimmerBrush,
                    modifier = Modifier.fillMaxWidth(0.72f).height(18.dp),
                    shape = RoundedCornerShape(9.dp),
                )
            }
        }
    }
}

@Composable
private fun ShimmerPlaceholder(
    brush: Brush,
    modifier: Modifier,
    shape: RoundedCornerShape,
) {
    Box(modifier = modifier.background(brush, shape))
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
