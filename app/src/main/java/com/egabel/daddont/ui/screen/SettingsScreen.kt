package com.egabel.daddont.ui.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egabel.daddont.Prefs
import com.egabel.daddont.ui.theme.BgLight
import com.egabel.daddont.ui.theme.BlueLeft
import com.egabel.daddont.ui.theme.BorderColor
import com.egabel.daddont.ui.theme.TextDim
import com.egabel.daddont.ui.theme.TextMid
import com.egabel.daddont.ui.theme.TitleGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)

    var geminiKey by rememberSaveable {
        mutableStateOf(prefs.getString(Prefs.KEY_GEMINI_API_KEY, "") ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Gemini API Key ───────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gemini API Key",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMid
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Used for impulse classification and Talk Me Down",
                        fontSize = 12.sp,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("API Key", color = TextDim) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlueLeft,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            prefs.edit().putString(Prefs.KEY_GEMINI_API_KEY, geminiKey.trim()).apply()
                        },
                        enabled = geminiKey.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (geminiKey.isNotBlank()) BlueLeft else BorderColor
                        )
                    ) {
                        Text(
                            "Save",
                            fontSize = 13.sp,
                            color = if (geminiKey.isNotBlank()) BlueLeft else TextDim
                        )
                    }
                }
            }
        }
    }
}
