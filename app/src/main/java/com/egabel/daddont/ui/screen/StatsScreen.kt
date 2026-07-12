package com.egabel.daddont.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.egabel.daddont.ui.theme.BgLight
import com.egabel.daddont.ui.theme.BlueLeft
import com.egabel.daddont.ui.theme.BorderColor
import com.egabel.daddont.ui.theme.GreenState
import com.egabel.daddont.ui.theme.PurpleRight
import com.egabel.daddont.ui.theme.RedState
import com.egabel.daddont.ui.theme.TextDim
import com.egabel.daddont.ui.theme.TextMid
import com.egabel.daddont.ui.theme.TitleGradient
import com.egabel.daddont.ui.theme.YellowState
import com.egabel.daddont.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scorecard",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BgLight
    ) { paddingValues ->
        val card = uiState.scorecard
        if (uiState.isLoading || card == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = BlueLeft, strokeWidth = 2.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Money not spent — the headline number
            item {
                StatCard(
                    value = "$${"%,.0f".format(card.moneyNotSpent)}",
                    label = "not spent — killed purchases, last 30 days",
                    accentColor = GreenState
                )
            }

            // Kill rate row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard(
                        value = "${card.killedCount}",
                        label = "killed",
                        accentColor = GreenState,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatCard(
                        value = "${card.didItCount}",
                        label = "did it",
                        accentColor = BlueLeft,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatCard(
                        value = "${card.awaitingVerdict}",
                        label = "awaiting verdict",
                        accentColor = if (card.overdueCount > 0) RedState else YellowState,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (card.overdueCount > 0) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = RedState.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, RedState.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${card.overdueCount} ${if (card.overdueCount == 1) "decision has" else "decisions have"} been waiting more than a day. Deciding is the whole point.",
                            modifier = Modifier.padding(14.dp),
                            fontSize = 13.sp,
                            color = RedState
                        )
                    }
                }
            }

            // Holds survived
            if (card.heldCount > 0) {
                item {
                    StatCard(
                        value = "${card.heldCount}",
                        label = "holds survived to their end time, last 30 days",
                        accentColor = GreenState
                    )
                }
            }

            // Breaches & clean streak — only shown once there's a record
            if (card.cleanStreakDays != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallStatCard(
                            value = "${card.slipCount}",
                            label = "slips (30d)",
                            accentColor = YellowState,
                            modifier = Modifier.weight(1f)
                        )
                        SmallStatCard(
                            value = "${card.brokeCount}",
                            label = "gave up (30d)",
                            accentColor = RedState,
                            modifier = Modifier.weight(1f)
                        )
                        SmallStatCard(
                            value = "${card.cleanStreakDays}d",
                            label = "clean streak",
                            accentColor = GreenState,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (card.moneySpentBreaking > 0) {
                    item {
                        StatCard(
                            value = "$${"%,.0f".format(card.moneySpentBreaking)}",
                            label = "spent by giving up early, last 30 days — this is what cooling protects",
                            accentColor = RedState
                        )
                    }
                }
            }

            // Prediction accuracy
            if (card.predictionsMade > 0) {
                item {
                    StatCard(
                        value = "${card.predictionsCorrect}/${card.predictionsMade}",
                        label = "predictions right — you said how you'd feel, here's how often you were right",
                        accentColor = PurpleRight
                    )
                }
            }

            // Desire decay
            card.avgDesireDecay?.let { decay ->
                item {
                    StatCard(
                        value = if (decay >= 0) "-%.1f".format(decay) else "+%.1f".format(-decay),
                        label = "average desire change from capture to latest check-in (out of 10)",
                        accentColor = if (decay >= 0) GreenState else RedState
                    )
                }
            }

            if (card.topOffenders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keeps coming back",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMid
                    )
                }
                items(card.topOffenders) { impulse ->
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
                                    .height(48.dp)
                                    .background(RedState)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = impulse.content,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextMid
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${impulse.returnCount}x",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RedState
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, accentColor: Color) {
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
                    .height(80.dp)
                    .background(accentColor)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = TextDim
                )
            }
        }
    }
}

@Composable
private fun SmallStatCard(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextDim
            )
        }
    }
}
