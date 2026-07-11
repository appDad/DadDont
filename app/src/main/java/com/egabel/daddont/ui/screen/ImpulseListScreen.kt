package com.egabel.daddont.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.Prediction
import com.egabel.daddont.data.model.Verdict
import com.egabel.daddont.data.repository.ImpulseWithState
import com.egabel.daddont.ui.theme.BgLight
import com.egabel.daddont.ui.theme.BlueLeft
import com.egabel.daddont.ui.theme.BorderColor
import com.egabel.daddont.ui.theme.GreenState
import com.egabel.daddont.ui.theme.ImpulseColors
import com.egabel.daddont.ui.theme.PartnerBadge
import com.egabel.daddont.ui.theme.RedState
import com.egabel.daddont.ui.theme.TextDim
import com.egabel.daddont.ui.theme.TextMid
import com.egabel.daddont.ui.theme.TitleGradient
import com.egabel.daddont.ui.viewmodel.ImpulseListViewModel
import com.egabel.daddont.ui.viewmodel.ListFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpulseListScreen(
    onImpulseClick: (UUID) -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ImpulseListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { viewModel.captureVoiceResult(it) }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceLauncher.launch(buildVoiceIntent())
        }
    }

    // ── Quick-facts sheet after capture ──────────────────────────────
    uiState.pendingFactsFor?.let { impulseId ->
        QuickFactsSheet(
            content = uiState.pendingFactsContent,
            onSave = { desire, cost, prediction ->
                viewModel.saveFacts(impulseId, desire, cost, prediction)
            },
            onSkip = { viewModel.skipFacts() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DadDont",
                        style = TextStyle(
                            brush = TitleGradient,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats", tint = TextDim)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextDim)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        voiceLauncher.launch(buildVoiceIntent())
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                shape = CircleShape,
                containerColor = BlueLeft,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice capture")
            }
        },
        containerColor = BgLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Capture input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.captureText,
                    onValueChange = viewModel::setCaptureText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("What's the impulse — and why?", color = TextDim) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlueLeft,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::captureImpulse,
                    enabled = uiState.captureText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Capture",
                        tint = if (uiState.captureText.isNotBlank()) BlueLeft else TextDim
                    )
                }
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Active" to ListFilter.ACTIVE,
                    "Decided" to ListFilter.ARCHIVE,
                    "To Discuss" to ListFilter.PARTNER
                ).forEach { (label, filter) ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(label, fontSize = 13.sp) },
                        shape = RoundedCornerShape(14.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.filter == filter,
                            borderColor = BorderColor,
                            selectedBorderColor = BlueLeft
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = BlueLeft.copy(alpha = 0.08f),
                            labelColor = TextDim,
                            selectedLabelColor = BlueLeft
                        )
                    )
                }
            }

            val isEmpty = when (uiState.filter) {
                ListFilter.ACTIVE -> uiState.decide.isEmpty() && uiState.cooling.isEmpty()
                else -> uiState.impulses.isEmpty()
            }

            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (uiState.filter) {
                            ListFilter.ACTIVE -> "No active impulses.\nCapture one above."
                            ListFilter.ARCHIVE -> "No verdicts yet."
                            ListFilter.PARTNER -> "Nothing flagged for discussion yet."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDim
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.filter == ListFilter.ACTIVE) {
                        if (uiState.decide.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Decide",
                                    count = uiState.decide.size,
                                    color = GreenState
                                )
                            }
                            items(uiState.decide, key = { it.impulse.id }) { item ->
                                ImpulseCard(item = item, onClick = { onImpulseClick(item.impulse.id) })
                            }
                        }
                        if (uiState.cooling.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Cooling",
                                    count = uiState.cooling.size,
                                    color = TextDim
                                )
                            }
                            items(uiState.cooling, key = { it.impulse.id }) { item ->
                                ImpulseCard(item = item, onClick = { onImpulseClick(item.impulse.id) })
                            }
                        }
                    } else {
                        items(uiState.impulses, key = { it.impulse.id }) { item ->
                            ImpulseCard(item = item, onClick = { onImpulseClick(item.impulse.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun buildVoiceIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "What's the impulse — and why do you want it?")
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            fontSize = 12.sp,
            color = TextDim
        )
    }
}

// ── Quick-facts bottom sheet ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFactsSheet(
    content: String,
    onSave: (desire: Int, cost: Double?, prediction: Prediction?) -> Unit,
    onSkip: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var desire by remember { mutableFloatStateOf(7f) }
    var costText by remember { mutableStateOf("") }
    var prediction by remember { mutableStateOf<Prediction?>(null) }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                text = "\"$content\"",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextMid,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "How badly do you want this right now?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMid
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = desire,
                    onValueChange = { desire = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = BlueLeft,
                        activeTrackColor = BlueLeft
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${desire.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueLeft
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Rough cost in $ (optional)", color = TextDim) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlueLeft,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "When this cools, you'll…",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMid
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PredictionChip(
                    label = "Still want it",
                    selected = prediction == Prediction.STILL_WANT,
                    onClick = {
                        prediction = if (prediction == Prediction.STILL_WANT) null
                                     else Prediction.STILL_WANT
                    }
                )
                PredictionChip(
                    label = "Have moved on",
                    selected = prediction == Prediction.MOVED_ON,
                    onClick = {
                        prediction = if (prediction == Prediction.MOVED_ON) null
                                     else Prediction.MOVED_ON
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        onSave(desire.toInt(), costText.toDoubleOrNull(), prediction)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BlueLeft),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save", color = BlueLeft, fontSize = 14.sp)
                }
                TextButton(onClick = onSkip) {
                    Text("Skip", color = TextDim, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PredictionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        shape = RoundedCornerShape(14.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = BorderColor,
            selectedBorderColor = PartnerBadge
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            selectedContainerColor = PartnerBadge.copy(alpha = 0.08f),
            labelColor = TextDim,
            selectedLabelColor = PartnerBadge
        )
    )
}

// ── Impulse card ─────────────────────────────────────────────────────

@Composable
private fun ImpulseCard(
    item: ImpulseWithState,
    onClick: () -> Unit
) {
    val stateColor by animateColorAsState(
        targetValue = ImpulseColors.borderColor(item.state),
        label = "stateColor"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(88.dp)
                    .background(stateColor)
            )

            Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stateLabel(item),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = stateColor
                        )
                        statusDetail(item)?.let { detail ->
                            Text(
                                text = " · $detail",
                                fontSize = 11.sp,
                                color = if (item.state == ImpulseState.GREEN &&
                                    (item.overdueMs ?: 0) > 86_400_000L) RedState else TextDim
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.impulse.partnerGate) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Discuss with partner",
                                modifier = Modifier.size(15.dp),
                                tint = PartnerBadge
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (item.impulse.returnCount > 0) {
                            Text(
                                text = "${item.impulse.returnCount}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDim
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.impulse.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        Text(
                            text = item.impulse.tier?.name ?: "Ungraded",
                            fontSize = 11.sp,
                            color = TextDim
                        )
                        item.impulse.estimatedCost?.let { cost ->
                            Text(
                                text = "  ·  $${"%,.0f".format(cost)}",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }
                    Text(
                        text = formatRelativeTime(item.impulse.createdAt),
                        fontSize = 11.sp,
                        color = TextDim
                    )
                }
            }
        }
    }
}

private fun stateLabel(item: ImpulseWithState): String = when (item.state) {
    ImpulseState.PENDING -> "Classifying…"
    ImpulseState.RED -> "Hot"
    ImpulseState.YELLOW -> "Cooling"
    ImpulseState.GREEN -> "Decide"
    ImpulseState.GRAY -> when (item.impulse.verdict) {
        Verdict.DID_IT -> "Did it"
        Verdict.KILLED -> "Killed"
        null -> "Archived"
    }
}

private fun statusDetail(item: ImpulseWithState): String? {
    item.overdueMs?.let { overdue ->
        if (overdue > 86_400_000L) {
            return "${overdue / 86_400_000L}d overdue"
        }
        return "verdict due"
    }
    item.msUntilNext?.let { return formatCountdown(it) }
    return null
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSec = ms / 1_000
    val totalMin = totalSec / 60
    return when {
        totalMin < 1 -> "${totalSec}s left"
        totalMin < 60 -> "${totalMin}m ${totalSec % 60}s left"
        totalMin < 1440 -> "${totalMin / 60}h ${totalMin % 60}m left"
        else -> "${totalMin / 1440}d ${(totalMin % 1440) / 60}h left"
    }
}
