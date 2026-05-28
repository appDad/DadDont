package com.egabel.daddont.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.ui.theme.ImpulseColors
import com.egabel.daddont.ui.theme.PartnerBadge
import com.egabel.daddont.ui.viewmodel.DialogMessage
import com.egabel.daddont.ui.viewmodel.ImpulseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImpulseDetailScreen(
    onBack: () -> Unit,
    viewModel: ImpulseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showReturnDialog by remember { mutableStateOf(false) }
    var returnRationale by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impulse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val impulseWithState = uiState.impulseWithState
        if (impulseWithState == null) {
            return@Scaffold
        }

        val impulse = impulseWithState.impulse
        val state = impulseWithState.state
        val stateColor = ImpulseColors.borderColor(state)
        val containerColor = ImpulseColors.containerColor(state)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card with gradient
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        containerColor.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(stateColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = ImpulseColors.label(state),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = stateColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    impulseWithState.msUntilNext?.let { ms ->
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "· ${formatCountdown(ms)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                if (impulse.partnerGate) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = "Discuss with partner",
                                            modifier = Modifier.size(18.dp),
                                            tint = PartnerBadge
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Discuss",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PartnerBadge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = impulse.content,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = impulse.tier?.name ?: "Ungraded",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = impulse.category?.name ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                        .format(Date(impulse.createdAt)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            if (impulse.partnerGate && impulse.partnerReason != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = impulse.partnerReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = PartnerBadge.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Returned ${impulse.returnCount}x · Reactivated ${impulse.reactivationCount}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state != ImpulseState.GRAY) {
                        if (!showReturnDialog) {
                            OutlinedButton(
                                onClick = { showReturnDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Impulse returned")
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    OutlinedTextField(
                                        value = returnRationale,
                                        onValueChange = { returnRationale = it },
                                        label = { Text("Why still want this? (optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.recordReturn(returnRationale.ifBlank { null })
                                                returnRationale = ""
                                                showReturnDialog = false
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("Record") }
                                        TextButton(onClick = {
                                            returnRationale = ""
                                            showReturnDialog = false
                                        }) { Text("Cancel") }
                                    }
                                }
                            }
                        }
                    }

                    if (state == ImpulseState.GRAY && impulse.dismissedAt != null) {
                        OutlinedButton(
                            onClick = { viewModel.recordReturn() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reactivate")
                        }
                    }

                    if (state == ImpulseState.GREEN) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(
                            text = "Cooled — your call",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (impulse.partnerGate) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { viewModel.dismiss(DismissalType.PARTNER_APPROVED) },
                                    label = { Text("Love of your life approved") }
                                )
                                AssistChip(
                                    onClick = { viewModel.dismiss(DismissalType.PARTNER_DECLINED) },
                                    label = { Text("Love of your life declined") }
                                )
                                AssistChip(
                                    onClick = { viewModel.dismiss(DismissalType.DECIDED_NOT_TO_ASK) },
                                    label = { Text("Decided not to ask") }
                                )
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AssistChip(
                                    onClick = { viewModel.dismiss(DismissalType.DONE) },
                                    label = { Text("Done / Executed") }
                                )
                                AssistChip(
                                    onClick = { viewModel.dismiss(DismissalType.NO_LONGER_WANT) },
                                    label = { Text("No longer want") }
                                )
                            }
                        }

                        if (!uiState.sentToDadDo) {
                            Button(
                                onClick = { viewModel.sendToDadDo() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Move to DadDo")
                            }
                        } else {
                            Text(
                                text = "Sent to DadDo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    TextButton(onClick = { viewModel.togglePartnerFlag() }) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (impulse.partnerGate) PartnerBadge
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (impulse.partnerGate) "Remove partner flag" else "Needs their blessing")
                    }
                }
            }

            // Talk Me Down section
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val shouldSuggest = impulse.returnCount >= 3 && !uiState.showTalkMeDown
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.toggleTalkMeDown() }) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Talk me through this")
                        }
                    }
                    if (shouldSuggest) {
                        Text(
                            text = "You've returned to this ${impulse.returnCount} times. Want to talk it through?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }

            // Dialog conversation
            if (uiState.showTalkMeDown) {
                items(uiState.dialogMessages) { message ->
                    DialogBubble(message)
                }

                if (uiState.isDialogLoading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.dialogInput,
                            onValueChange = viewModel::setDialogInput,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("What are you thinking?") },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = viewModel::sendDialogMessage,
                            enabled = uiState.dialogInput.isNotBlank() && !uiState.isDialogLoading
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }

            // Return history
            if (uiState.returnEvents.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Return History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.returnEvents) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                .format(Date(event.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (event.rationale != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = event.rationale,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val totalMin = ms / 60_000
    return when {
        totalMin < 60 -> "${totalMin}m left"
        totalMin < 1440 -> "${totalMin / 60}h ${totalMin % 60}m left"
        else -> "${totalMin / 1440}d ${(totalMin % 1440) / 60}h left"
    }
}

@Composable
private fun DialogBubble(message: DialogMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
