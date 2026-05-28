package com.egabel.daddont.api.tasks

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleTasksClient(private val context: Context) {
    private val accountManager = AccountManager.get(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    private var cachedAccount: Account? = null
    private var cachedToken: String? = null

    suspend fun chooseAccount(activity: Activity): Account? = withContext(Dispatchers.Main) {
        val intent = AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
        // Caller launches this intent via ActivityResultLauncher
        // and calls setAccount() with the result
        null
    }

    fun setAccount(account: Account) {
        cachedAccount = account
        cachedToken = null
    }

    fun getSelectedAccount(): Account? = cachedAccount

    suspend fun getAuthToken(activity: Activity): String {
        val account = cachedAccount ?: throw IllegalStateException("No Google account selected")
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                accountManager.getAuthToken(
                    account,
                    "oauth2:https://www.googleapis.com/auth/tasks",
                    Bundle(),
                    activity,
                    { future ->
                        try {
                            val result = future.result
                            val token = result.getString(AccountManager.KEY_AUTHTOKEN)
                            if (token != null) {
                                cachedToken = token
                                cont.resume(token)
                            } else {
                                cont.resumeWithException(Exception("No auth token returned"))
                            }
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    },
                    null
                )
            }
        }
    }

    suspend fun createTask(title: String, token: String): TaskCreateResult = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/tasks/v1/lists/@default/tasks"
        val body = json.encodeToString(
            kotlinx.serialization.serializer<TaskCreateRequest>(),
            TaskCreateRequest(title = title)
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.code == 401) {
            // Token expired — invalidate and throw so caller can retry
            accountManager.invalidateAuthToken("com.google", token)
            cachedToken = null
            throw TokenExpiredException()
        }

        if (!response.isSuccessful) {
            throw Exception("Google Tasks API error ${response.code}: $responseBody")
        }

        json.decodeFromString<TaskCreateResult>(responseBody)
    }

    suspend fun sendToDadDo(title: String, activity: Activity): TaskCreateResult {
        var token = cachedToken ?: getAuthToken(activity)
        return try {
            createTask(title, token)
        } catch (e: TokenExpiredException) {
            token = getAuthToken(activity)
            createTask(title, token)
        }
    }
}

class TokenExpiredException : Exception("Google Tasks auth token expired")
