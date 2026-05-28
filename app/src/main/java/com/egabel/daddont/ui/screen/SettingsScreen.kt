package com.egabel.daddont.ui.screen

import android.accounts.AccountManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LinkOff
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.egabel.daddont.Prefs
import com.egabel.daddont.api.tasks.AuthResult
import com.egabel.daddont.api.tasks.GoogleTasksClient
import com.egabel.daddont.ui.theme.BgLight
import com.egabel.daddont.ui.theme.BlueLeft
import com.egabel.daddont.ui.theme.BorderColor
import com.egabel.daddont.ui.theme.GreenState
import com.egabel.daddont.ui.theme.TextDim
import com.egabel.daddont.ui.theme.TextMid
import com.egabel.daddont.ui.theme.TitleGradient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val tasksClient = GoogleTasksClient(context)

    var geminiKey by rememberSaveable {
        mutableStateOf(prefs.getString(Prefs.KEY_GEMINI_API_KEY, "") ?: "")
    }
    var linkedAccount by rememberSaveable {
        mutableStateOf(prefs.getString(Prefs.KEY_ACCOUNT, null))
    }
    var accountStatus by rememberSaveable { mutableStateOf("") }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val accountName = linkedAccount ?: return@rememberLauncherForActivityResult
        scope.launch {
            when (val auth = tasksClient.getAuthTokenForAccount(accountName)) {
                is AuthResult.Token -> {
                    prefs.edit().putString(Prefs.KEY_TOKEN, auth.token).apply()
                    accountStatus = "Linked"
                }
                else -> { accountStatus = "Consent failed — try again" }
            }
        }
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (accountName != null) {
            prefs.edit().putString(Prefs.KEY_ACCOUNT, accountName).apply()
            linkedAccount = accountName
            accountStatus = "Authorizing..."

            scope.launch {
                when (val auth = tasksClient.getAuthTokenForAccount(accountName)) {
                    is AuthResult.Token -> {
                        prefs.edit().putString(Prefs.KEY_TOKEN, auth.token).apply()
                        accountStatus = "Linked"
                    }
                    is AuthResult.ConsentRequired -> {
                        consentLauncher.launch(auth.intent)
                    }
                    is AuthResult.Failed -> {
                        accountStatus = "Failed: ${auth.message}"
                    }
                }
            }
        }
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

            // ── Google Tasks / DadDo ─────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Tasks (DadDo)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMid
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Link your Google account to move cooled impulses into DadDo",
                        fontSize = 12.sp,
                        color = TextDim
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (linkedAccount != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GreenState,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = linkedAccount!!,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMid
                                )
                                if (accountStatus.isNotEmpty()) {
                                    Text(
                                        text = accountStatus,
                                        fontSize = 12.sp,
                                        color = TextDim
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    accountPickerLauncher.launch(tasksClient.buildAccountPickerIntent())
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text("Change Account", fontSize = 13.sp, color = TextMid)
                            }
                            OutlinedButton(
                                onClick = {
                                    prefs.edit()
                                        .remove(Prefs.KEY_ACCOUNT)
                                        .remove(Prefs.KEY_TOKEN)
                                        .apply()
                                    linkedAccount = null
                                    accountStatus = ""
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Icon(
                                    Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = TextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unlink", fontSize = 13.sp, color = TextMid)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                accountPickerLauncher.launch(tasksClient.buildAccountPickerIntent())
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BlueLeft)
                        ) {
                            Text("Link Google Account", fontSize = 13.sp, color = BlueLeft)
                        }
                    }
                }
            }
        }
    }
}
