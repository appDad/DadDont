package com.egabel.daddont.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egabel.daddont.data.model.DesireCheckIn
import com.egabel.daddont.data.model.ImpulseKind
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.Prediction
import com.egabel.daddont.data.model.Verdict
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
import com.egabel.daddont.ui.viewmodel.DialogMessage
import com.egabel.daddont.ui.viewmodel.ImpulseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImpulseDetailScreen(
    onBack: () -> Unit,
    viewModel: ImpulseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Return flow
    var showReturnDialog by remember { mutableStateOf(false) }
    var returnRationale by remember { mutableStateOf("") }
    var returnDesire by remember { mutableFloatStateOf(7f) }

    // Check-in flow
    var showCheckIn by remember { mutableStateOf(false) }
    var checkInDesire by remember { mutableFloatStateOf(5f) }

    // Defer flow
    var showDeferDialog by remember { mutableStateOf(false) }

    // Breach flow — acted before it cooled
    var showBreachDialog by remember { mutableStateOf(false) }
    var breachReason by remember { mutableStateOf("") }

    // Cool-until picker flow: date → time
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickedDateMs by remember { mutableStateOf(0L) }
    // Where the picked timestamp should go: true = defer, false = plain decideBy edit
    var pickerIsForDefer by remember { mutableStateOf(false) }
    var deferReason by remember { mutableStateOf("") }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

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

    // ── Breach dialog: record the slip, then keep going or give up ───
    if (showBreachDialog) {
        val isPartnerGated = uiState.impulseWithState?.impulse?.partnerGate == true
        AlertDialog(
            onDismissRequest = { showBreachDialog = false },
            title = { Text("Record the breach", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        "It happens. A slip recorded honestly is training data, not defeat — what happened?",
                        fontSize = 13.sp, color = TextMid
                    )
                    if (isPartnerGated) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "This one was partner-gated. The record is step one — telling them is step two.",
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = PartnerBadge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = breachReason,
                        onValueChange = { breachReason = it },
                        placeholder = { Text("What happened? What triggered it?", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedState,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary path: the slip is logged and the contract continues
                    Button(
                        onClick = {
                            viewModel.recordSlip(breachReason.trim())
                            breachReason = ""
                            showBreachDialog = false
                        },
                        enabled = breachReason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueLeft)
                    ) {
                        Text("Slipped — keep going", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            viewModel.recordVerdict(Verdict.BROKE, breachReason.trim())
                            breachReason = ""
                            showBreachDialog = false
                        },
                        enabled = breachReason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Give up on this one entirely",
                            fontSize = 13.sp,
                            color = if (breachReason.isNotBlank()) RedState else TextDim
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBreachDialog = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }

    // ── Defer dialog: reason + duration chips or custom date ─────────
    if (showDeferDialog) {
        AlertDialog(
            onDismissRequest = { showDeferDialog = false },
            title = { Text("Defer the decision", fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        "Not deciding is a decision too — say why, and pick a new deadline.",
                        fontSize = 13.sp, color = TextMid
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deferReason,
                        onValueChange = { deferReason = it },
                        placeholder = { Text("Why defer? (required)", color = TextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueLeft,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Tomorrow" to 86_400_000L,
                            "3 days" to 3 * 86_400_000L,
                            "1 week" to 7 * 86_400_000L
                        ).forEach { (label, ms) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (deferReason.isNotBlank()) {
                                        viewModel.defer(
                                            System.currentTimeMillis() + ms,
                                            deferReason.trim()
                                        )
                                        deferReason = ""
                                        showDeferDialog = false
                                    }
                                },
                                enabled = deferReason.isNotBlank(),
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = {
                                if (deferReason.isNotBlank()) {
                                    pickerIsForDefer = true
                                    showDeferDialog = false
                                    showDatePicker = true
                                }
                            },
                            enabled = deferReason.isNotBlank(),
                            label = { Text("Pick date…", fontSize = 12.sp) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeferDialog = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            shape = RoundedCornerShape(14.dp)
        )
    }

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
                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
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
                    if (pickerIsForDefer) {
                        viewModel.defer(cal.timeInMillis, deferReason.trim().ifEmpty { "deferred" })
                        deferReason = ""
                    } else {
                        viewModel.setDecideBy(cal.timeInMillis)
                    }
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
            // ── HOLD panel — you made it to the hard stop ────────────────
            if (state == ImpulseState.GREEN && impulse.kind == ImpulseKind.HOLD) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, GreenState)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "You made it",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenState
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "The hold is over — it's allowed now." +
                                    if (uiState.breachEvents.isNotEmpty())
                                        " You slipped ${uiState.breachEvents.size}x along the way, and still got here."
                                    else " Clean run.",
                                fontSize = 13.sp,
                                color = TextMid
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.recordVerdict(Verdict.HELD) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenState)
                            ) {
                                Icon(
                                    Icons.Default.Check, contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Held out — close it", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = { showDeferDialog = true }) {
                                Icon(
                                    Icons.Default.Schedule, contentDescription = null,
                                    modifier = Modifier.size(15.dp), tint = TextDim
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extend the hold instead", fontSize = 13.sp, color = TextDim)
                            }
                        }
                    }
                }
            }

            // ── VERDICT PANEL — pinned first when a decision is due ──────
            if (state == ImpulseState.GREEN && impulse.kind == ImpulseKind.DECISION) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, GreenState)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Verdict time",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenState
                                )
                                impulseWithState.overdueMs?.let { overdue ->
                                    if (overdue > 86_400_000L) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${overdue / 86_400_000L}d overdue",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RedState
                                        )
                                    }
                                }
                            }

                            // Prediction callback — confront them with their own forecast
                            impulse.prediction?.let { p ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when (p) {
                                        Prediction.STILL_WANT ->
                                            "When you captured this, you predicted you'd still want it."
                                        Prediction.MOVED_ON ->
                                            "When you captured this, you predicted you'd have moved on by now."
                                    },
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = TextMid
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.recordVerdict(Verdict.DID_IT) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BlueLeft)
                                ) {
                                    Icon(
                                        Icons.Default.Check, contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Did it", fontSize = 14.sp)
                                }
                                Button(
                                    onClick = { viewModel.recordVerdict(Verdict.KILLED) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedState)
                                ) {
                                    Icon(
                                        Icons.Default.Close, contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kill it", fontSize = 14.sp)
                                }
                            }

                            if (impulse.partnerGate) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    NoteChip("Partner approved") {
                                        viewModel.recordVerdict(Verdict.DID_IT, "Partner approved")
                                    }
                                    NoteChip("Partner declined") {
                                        viewModel.recordVerdict(Verdict.KILLED, "Partner declined")
                                    }
                                    NoteChip("Decided not to ask") {
                                        viewModel.recordVerdict(Verdict.KILLED, "Decided not to ask")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = { showDeferDialog = true }) {
                                Icon(
                                    Icons.Default.Schedule, contentDescription = null,
                                    modifier = Modifier.size(15.dp), tint = TextDim
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Not ready — defer with a reason", fontSize = 13.sp, color = TextDim)
                            }
                        }
                    }
                }
            }

            // ── Header card ──────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(200.dp)
                                .background(stateColor)
                        )

                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
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
                                        text = detailStateLabel(state, impulse.verdict, impulse.kind),
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

                            if (isEditing) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BlueLeft,
                                        unfocusedBorderColor = BorderColor
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

                            // Why they wanted it — captured mental state
                            impulse.rationale?.let { why ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Why: $why",
                                    fontSize = 13.sp,
                                    color = TextMid
                                )
                            }
                            impulse.trigger?.let { trig ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Trigger: $trig",
                                    fontSize = 12.sp,
                                    color = TextDim
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = impulse.tier?.name ?: "Ungraded",
                                    fontSize = 11.sp, color = TextDim
                                )
                                Text(
                                    text = impulse.category?.name ?: "",
                                    fontSize = 11.sp, color = TextDim
                                )
                                impulse.estimatedCost?.let {
                                    Text(
                                        text = "$${"%,.0f".format(it)}",
                                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                        color = TextMid
                                    )
                                }
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                        .format(Date(impulse.createdAt)),
                                    fontSize = 11.sp, color = TextDim
                                )
                            }

                            impulse.decideBy?.let { decideBy ->
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
                                        text = (if (impulse.kind == ImpulseKind.HOLD) "Open at " else "Verdict due ") +
                                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(decideBy)),
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
                                text = "Returned ${impulse.returnCount}x · Deferred ${impulse.deferCount}x · Reactivated ${impulse.reactivationCount}x" +
                                    (if (uiState.breachEvents.isNotEmpty()) " · Slipped ${uiState.breachEvents.size}x" else ""),
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }

            // ── Desire curve ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Desire over time",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMid
                            )
                            uiState.desireCurve.lastOrNull()?.let {
                                Text(
                                    text = "now ${it.strength}/10",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueLeft
                                )
                            }
                        }

                        if (uiState.desireCurve.size >= 2) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DesireSparkline(
                                curve = uiState.desireCurve,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                            val first = uiState.desireCurve.first().strength
                            val last = uiState.desireCurve.last().strength
                            if (first != last) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (last < first)
                                        "Down ${first - last} points since capture — it's fading."
                                    else
                                        "Up ${last - first} points since capture — this one's persistent.",
                                    fontSize = 12.sp,
                                    color = if (last < first) GreenState else RedState
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check in when it hits you again — the curve is the evidence.",
                                fontSize = 12.sp,
                                color = TextDim
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (state != ImpulseState.GRAY) {
                            if (!showCheckIn) {
                                OutlinedButton(
                                    onClick = { showCheckIn = true },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Text("How strong is it right now?", color = TextMid, fontSize = 13.sp)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Slider(
                                        value = checkInDesire,
                                        onValueChange = { checkInDesire = it },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = BlueLeft,
                                            activeTrackColor = BlueLeft
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${checkInDesire.toInt()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueLeft
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.checkInDesire(checkInDesire.toInt())
                                            showCheckIn = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BlueLeft)
                                    ) {
                                        Text("Record", color = BlueLeft, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { showCheckIn = false }) {
                                        Text("Cancel", color = TextDim, fontSize = 13.sp)
                                    }
                                }
                            }
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
                        if (state == ImpulseState.RED || state == ImpulseState.YELLOW) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pickerIsForDefer = false
                                        showDatePicker = true
                                    },
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
                                    Text("Change deadline…", color = BlueLeft, fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Breach — honesty valve for acting before it cooled
                            OutlinedButton(
                                onClick = { showBreachDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, RedState.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = RedState,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("I already acted on it", color = RedState, fontSize = 13.sp)
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
                                    Text("It's back on my mind", color = TextMid, fontSize = 14.sp)
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = BgLight,
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "How strong right now?",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextMid
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Slider(
                                                value = returnDesire,
                                                onValueChange = { returnDesire = it },
                                                valueRange = 1f..10f,
                                                steps = 8,
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = BlueLeft,
                                                    activeTrackColor = BlueLeft
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${returnDesire.toInt()}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlueLeft
                                            )
                                        }
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
                                                    viewModel.recordReturn(
                                                        returnRationale.ifBlank { null },
                                                        returnDesire.toInt()
                                                    )
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

                        if (state == ImpulseState.GRAY) {
                            impulse.verdict?.let { v ->
                                Text(
                                    text = when (v) {
                                        Verdict.DID_IT -> "Verdict: Did it"
                                        Verdict.KILLED -> "Verdict: Killed"
                                        Verdict.BROKE -> "Verdict: Gave up before it finished"
                                        Verdict.HELD -> "Verdict: Held out to the end"
                                    } + (impulse.verdictNote?.let { " — $it" } ?: ""),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (v) {
                                        Verdict.BROKE -> RedState
                                        Verdict.HELD -> GreenState
                                        else -> TextMid
                                    }
                                )
                                impulse.verdictAt?.let {
                                    Text(
                                        text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                            .format(Date(it)),
                                        fontSize = 11.sp,
                                        color = TextDim
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            OutlinedButton(
                                onClick = { viewModel.reactivate() },
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
                                Text("It's back — reactivate", color = TextMid, fontSize = 14.sp)
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

            // ── History: returns + slips, one honest timeline ────────────
            if (uiState.returnEvents.isNotEmpty() || uiState.breachEvents.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "History",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMid
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            data class HistoryLine(
                                val timestamp: Long,
                                val text: String?,
                                val isSlip: Boolean
                            )
                            val lines = (
                                uiState.returnEvents.map {
                                    HistoryLine(it.timestamp, it.rationale, false)
                                } + uiState.breachEvents.map {
                                    HistoryLine(it.timestamp, "Slipped — ${it.note}", true)
                                }
                            ).sortedBy { it.timestamp }

                            lines.forEach { line ->
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
                                            .background(if (line.isSlip) RedState else TextDim)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = SimpleDateFormat(
                                            "MMM d, h:mm a",
                                            Locale.getDefault()
                                        ).format(Date(line.timestamp)),
                                        fontSize = 11.sp,
                                        color = TextDim
                                    )
                                    if (line.text != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = line.text,
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color = if (line.isSlip) RedState else TextMid
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

// ── Components ───────────────────────────────────────────────────────

@Composable
private fun DesireSparkline(curve: List<DesireCheckIn>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (curve.size < 2) return@Canvas
        val minT = curve.first().timestamp
        val maxT = curve.last().timestamp
        val spanT = (maxT - minT).coerceAtLeast(1)

        val points = curve.map { checkIn ->
            Offset(
                x = (checkIn.timestamp - minT).toFloat() / spanT * size.width,
                y = size.height - ((checkIn.strength - 1) / 9f * size.height)
            )
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFF1A60A5),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        points.forEach { p ->
            drawCircle(color = Color(0xFF1A60A5), radius = 5f, center = p)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteChip(label: String, onClick: () -> Unit) {
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

private fun detailStateLabel(state: ImpulseState, verdict: Verdict?, kind: ImpulseKind): String = when (state) {
    ImpulseState.PENDING -> "Classifying…"
    ImpulseState.RED -> if (kind == ImpulseKind.HOLD) "Holding" else "Hot"
    ImpulseState.YELLOW -> "Cooling"
    ImpulseState.GREEN -> if (kind == ImpulseKind.HOLD) "Open" else "Decide"
    ImpulseState.GRAY -> when (verdict) {
        Verdict.DID_IT -> "Did it"
        Verdict.KILLED -> "Killed"
        Verdict.BROKE -> "Gave up"
        Verdict.HELD -> "Held out"
        null -> "Archived"
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
