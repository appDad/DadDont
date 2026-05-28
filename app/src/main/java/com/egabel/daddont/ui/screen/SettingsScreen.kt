package com.egabel.daddont.ui.screen

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.egabel.daddont.Prefs
import com.egabel.daddont.api.tasks.AuthResult
import com.egabel.daddont.api.tasks.GoogleTasksClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Gemini API Key
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gemini API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Used for impulse classification and Talk Me Down",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("API Key") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            prefs.edit().putString(Prefs.KEY_GEMINI_API_KEY, geminiKey.trim()).apply()
                        },
                        enabled = geminiKey.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }

            HorizontalDivider()

            // Google Tasks / DadDo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Google Tasks (DadDo)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Link your Google account to move cooled impulses into DadDo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (linkedAccount != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = linkedAccount!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (accountStatus.isNotEmpty()) {
                                    Text(
                                        text = accountStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    accountPickerLauncher.launch(tasksClient.buildAccountPickerIntent())
                                }
                            ) {
                                Text("Change Account")
                            }
                            OutlinedButton(
                                onClick = {
                                    prefs.edit()
                                        .remove(Prefs.KEY_ACCOUNT)
                                        .remove(Prefs.KEY_TOKEN)
                                        .apply()
                                    linkedAccount = null
                                    accountStatus = ""
                                }
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unlink")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                accountPickerLauncher.launch(tasksClient.buildAccountPickerIntent())
                            }
                        ) {
                            Text("Link Google Account")
                        }
                    }
                }
            }
        }
    }
}
