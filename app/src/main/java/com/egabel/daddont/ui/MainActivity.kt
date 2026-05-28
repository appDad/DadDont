package com.egabel.daddont.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.egabel.daddont.Prefs
import com.egabel.daddont.api.tasks.AuthResult
import com.egabel.daddont.api.tasks.GoogleTasksClient
import com.egabel.daddont.ui.navigation.DadDontNavGraph
import com.egabel.daddont.ui.theme.DadDontTheme
import com.egabel.daddont.worker.ClassificationWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tasksClient by lazy { GoogleTasksClient(this) }

    private val accountPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
        if (accountName != null) {
            getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(Prefs.KEY_ACCOUNT, accountName)
                .apply()

            lifecycleScope.launch {
                when (val auth = tasksClient.getAuthTokenForAccount(accountName)) {
                    is AuthResult.Token -> {
                        getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(Prefs.KEY_TOKEN, auth.token)
                            .apply()
                    }
                    is AuthResult.ConsentRequired -> {
                        consentLauncher.launch(auth.intent)
                    }
                    is AuthResult.Failed -> {}
                }
            }
        }
    }

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val accountName = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
            .getString(Prefs.KEY_ACCOUNT, null) ?: return@registerForActivityResult
        lifecycleScope.launch {
            when (val auth = tasksClient.getAuthTokenForAccount(accountName)) {
                is AuthResult.Token -> {
                    getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(Prefs.KEY_TOKEN, auth.token)
                        .apply()
                }
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ClassificationWorker.enqueue(applicationContext)

        setContent {
            DadDontTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val prefs = getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
                    val hasApiKey = !prefs.getString(Prefs.KEY_GEMINI_API_KEY, null).isNullOrBlank()

                    if (!hasApiKey) {
                        SetupScreen(
                            onApiKeySaved = { key ->
                                prefs.edit().putString(Prefs.KEY_GEMINI_API_KEY, key).apply()
                                recreate()
                            },
                            onLinkAccount = {
                                accountPickerLauncher.launch(tasksClient.buildAccountPickerIntent())
                            }
                        )
                    } else {
                        val navController = rememberNavController()
                        DadDontNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    onApiKeySaved: (String) -> Unit,
    onLinkAccount: () -> Unit
) {
    var apiKey by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dad Don't",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Capture impulses. Let time do the work.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (apiKey.isNotBlank()) onApiKeySaved(apiKey.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = apiKey.isNotBlank()
        ) {
            Text("Save & Continue")
        }
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onLinkAccount) {
            Text("Link Google Account (for DadDo)")
        }
    }
}
