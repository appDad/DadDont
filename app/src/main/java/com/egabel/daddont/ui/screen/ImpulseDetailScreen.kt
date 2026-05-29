package com.egabel.daddont.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.ui.theme.BgLight
import com.egabel.daddont.ui.theme.BlueLeft
import com.egabel.daddont.ui.theme.BorderColor
import com.egabel.daddont.ui.theme.ImpulseColors
import com.egabel.daddont.ui.theme.PartnerBadge
import com.egabel.daddont.ui.theme.RedState
import com.egabel.daddont.ui.theme.TextDim
import com.egabel.daddont.ui.theme.TextMid
import com.egabel.daddont.ui.theme.TitleGradient
import com.egabel.daddont.ui.viewmodel.DialogMessage
import com.egabel.daddont.ui.viewmodel.ImpulseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
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
    var showReturnDialog by remember { mutableStateOf(false) }
    var returnRationale by remember { mutableStateOf("") }

    // Edit state
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    // Delete confirmation
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Cool-until picker flow: date → time
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMs by remember { mutableStateOf(0L) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete impulse?", fontWeight = FontWeight.SemiBold) },
            text = { Text("This can't be undone.", color = TextMid) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete { onBack() }
                }) {
                    Text("Delete", color = RedState)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }

    // ── Date picker dialog ───────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        pickedDateMs = ms
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) {
                    Text("Next", color = BlueLeft)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            shape = RoundedCornerShape(14.dp)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Time picker dialog ───────────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 20, initialMinute = 0)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick a time", fontWeight = FontWeight.SemiBold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker returns UTC midnight — extract y/m/d in UTC,
                    // then build the target in the device's local timezone.
                    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = pickedDateMs
                    }
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.setCustomCoolUntil(cal.timeInMillis)
                    showTimePicker = false
                }) {
                    Text("Set", color = BlueLeft)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Impulse",
                        style = TextStyle(
                            brush = TitleGradient,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDim
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val content = uiState.impulseWithState?.impulse?.content ?: ""
                        editText = content
                        isEditing = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextDim)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextDim)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgLight
    ) { paddingValues ->
        val impulseWithState = uiState.impulseWithState
        if (impulseWithState == null) {
            return@Scaffold
        }

        val impulse = impulseWithState.impulse
        val state = impulseWithState.state
        val stateColor by animateColorAsState(
            targetValue = ImpulseColors.borderColor(state),
            label = "stateColor"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header card ──────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left accent bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(160.dp)
                                .background(stateColor)
                        )

                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                            // State row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(stateColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ImpulseColors.label(state),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = stateColor
                                    )
                                    impulseWithState.msUntilNext?.let { ms ->
                                        Text(
                                            text = " · ${formatCountdown(ms)}",
                                            fontSize = 12.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                                if (impulse.partnerGate) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubbleOutline,
                                            contentDescription = "Discuss with partner",
                                            modifier = Modifier.size(15.dp),
                                            tint = PartnerBadge
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Discuss",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PartnerBadge
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Impulse content — inline edit or display
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlueLeft,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            if (editText.isNotBlank()) {
                                                viewModel.updateContent(editText.trim())
                                            }
                                            isEditing = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BlueLeft)
                                    ) {
                                        Text("Save", color = BlueLeft, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { isEditing = false }) {
                                        Text("Cancel", color = TextDim, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                Text(
                                    text = impulse.content,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Metadata row
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = impulse.tier?.name ?: "Ungraded",
                                    fontSize = 11.sp,
                                    color = TextDim
                                )
                                Text(
                                    text = impulse.category?.name ?: "",
                                    fontSize = 11.sp,
                                    color = TextDim
                                )
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                        .format(Date(impulse.createdAt)),
                                    fontSize = 11.sp,
                                    color = TextDim
                                )
                            }

                            // Custom cool-until indicator
                            impulse.customCoolUntil?.let { coolUntil ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = BlueLeft
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cools at ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(coolUntil))}",
                                        fontSize = 11.sp,
                                        color = BlueLeft
                                    )
                                }
                            }

                            if (impulse.partnerGate && impulse.partnerReason != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = impulse.partnerReason,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = PartnerBadge.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Returned ${impulse.returnCount}x · Reactivated ${impulse.reactivationCount}x",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }

            // ── Actions card ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Cool-until button (only for active, non-gray impulses)
                        if (state != ImpulseState.GRAY && state != ImpulseState.PENDING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BlueLeft),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = BlueLeft,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cool until…", color = BlueLeft, fontSize = 13.sp)
                                }
                                if (impulse.customCoolUntil != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.setCustomCoolUntil(null) },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Text("Reset", color = TextDim, fontSize = 13.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (state != ImpulseState.GRAY) {
                            if (!showReturnDialog) {
                                OutlinedButton(
                                    onClick = { showReturnDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = TextMid,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Impulse returned", color = TextMid, fontSize = 14.sp)
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BgLight,
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        OutlinedTextField(
                                            value = returnRationale,
                                            onValueChange = { returnRationale = it },
                                            placeholder = {
                                                Text("Why still want this? (optional)", color = TextDim)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BlueLeft,
                                                unfocusedBorderColor = BorderColor,
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.recordReturn(returnRationale.ifBlank { null })
                                                    returnRationale = ""
                                                    showReturnDialog = false
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(1.dp, BlueLeft)
                                            ) {
                                                Text("Record", color = BlueLeft, fontSize = 13.sp)
                                            }
                                            TextButton(onClick = {
                                                returnRationale = ""
                                                showReturnDialog = false
                                            }) {
                                                Text("Cancel", color = TextDim, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state == ImpulseState.GRAY && impulse.dismissedAt != null) {
                            OutlinedButton(
                                onClick = { viewModel.recordReturn() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = TextMid,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reactivate", color = TextMid, fontSize = 14.sp)
                            }
                        }

                        if (state == ImpulseState.GREEN) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cooled — your call",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImpulseColors.borderColor(ImpulseState.GREEN)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (impulse.partnerGate) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DismissChip("Love of your life approved") {
                                        viewModel.dismiss(DismissalType.PARTNER_APPROVED)
                                    }
                                    DismissChip("Love of your life declined") {
                                        viewModel.dismiss(DismissalType.PARTNER_DECLINED)
                                    }
                                    DismissChip("Decided not to ask") {
                                        viewModel.dismiss(DismissalType.DECIDED_NOT_TO_ASK)
                                    }
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DismissChip("Done / Executed") {
                                        viewModel.dismiss(DismissalType.DONE)
                                    }
                                    DismissChip("No longer want") {
                                        viewModel.dismiss(DismissalType.NO_LONGER_WANT)
                                    }
                                }
                            }

                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { viewModel.togglePartnerFlag() }) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (impulse.partnerGate) PartnerBadge else TextDim
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (impulse.partnerGate) "Remove partner flag" else "Needs their blessing",
                                fontSize = 13.sp,
                                color = if (impulse.partnerGate) PartnerBadge else TextMid
                            )
                        }
                    }
                }
            }

            // ── Talk Me Down ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val shouldSuggest = impulse.returnCount >= 3 && !uiState.showTalkMeDown
                        TextButton(onClick = { viewModel.toggleTalkMeDown() }) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = BlueLeft
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.showTalkMeDown) "Hide dialog" else "Talk me through this",
                                fontSize = 13.sp,
                                color = BlueLeft
                            )
                        }
                        if (shouldSuggest) {
                            Text(
                                text = "You've returned to this ${impulse.returnCount} times. Want to talk it through?",
                                fontSize = 12.sp,
                                color = TextDim,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── Dialog conversation ──────────────────────────────────────
            if (uiState.showTalkMeDown) {
                items(uiState.dialogMessages) { message ->
                    DialogBubble(message)
                }

                if (uiState.isDialogLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BlueLeft
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking…", fontSize = 12.sp, color = TextDim)
                        }
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
                            placeholder = { Text("What are you thinking?", color = TextDim) },
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
                            onClick = viewModel::sendDialogMessage,
                            enabled = uiState.dialogInput.isNotBlank() && !uiState.isDialogLoading
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (uiState.dialogInput.isNotBlank()) BlueLeft else TextDim
                            )
                        }
                    }
                }
            }

            // ── Return history ───────────────────────────────────────────
            if (uiState.returnEvents.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Return History",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMid
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.returnEvents.forEach { event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(TextDim)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = SimpleDateFormat(
                                            "MMM d, h:mm a",
                                            Locale.getDefault()
                                        ).format(Date(event.timestamp)),
                                        fontSize = 11.sp,
                                        color = TextDim
                                    )
                                    if (event.rationale != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = event.rationale,
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = TextMid
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(14.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = BorderColor,
            selectedBorderColor = BlueLeft
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = TextMid
        )
    )
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

@Composable
private fun DialogBubble(message: DialogMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 14.dp
            ),
            color = if (isUser) BlueLeft.copy(alpha = 0.08f) else BgLight,
            border = BorderStroke(1.dp, if (isUser) BlueLeft.copy(alpha = 0.2f) else BorderColor)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
