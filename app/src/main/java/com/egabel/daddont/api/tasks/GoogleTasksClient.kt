package com.egabel.daddont.api.tasks

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.egabel.daddont.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume

class GoogleTasksClient(private val context: Context) {

    private val http = OkHttpClient()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    @Suppress("DEPRECATION")
    fun buildAccountPickerIntent(): Intent =
        AccountManager.newChooseAccountIntent(null, null, arrayOf(ACCOUNT_TYPE), null, null, null, null)

    suspend fun getAuthTokenForAccount(accountName: String): AuthResult =
        withContext(Dispatchers.IO) {
            val account = Account(accountName, ACCOUNT_TYPE)
            suspendCancellableCoroutine { cont ->
                AccountManager.get(context).getAuthToken(account, SCOPE, null, null, { future ->
                    runCatching {
                        val bundle = future.result
                        @Suppress("DEPRECATION")
                        val authIntent = bundle.getParcelable<Intent>(AccountManager.KEY_INTENT)
                        if (authIntent != null) cont.resume(AuthResult.ConsentRequired(authIntent))
                        else {
                            val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                            if (token != null) cont.resume(AuthResult.Token(token))
                            else cont.resume(AuthResult.Failed("No token in bundle"))
                        }
                    }.onFailure { e -> cont.resume(AuthResult.Failed(e.message ?: "Auth failed")) }
                }, null)
            }
        }

    fun invalidateToken(token: String) {
        AccountManager.get(context).invalidateAuthToken(ACCOUNT_TYPE, token)
    }

    suspend fun createTask(token: String, title: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = buildJsonObject {
                    put("title", title)
                }.toString()

                post(token, TASKS_BASE, body) != null
            } catch (e: Exception) {
                false
            }
        }

    private fun post(token: String, url: String, body: String): String? = execute(
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody(jsonMedia))
            .build()
    )

    class AuthExpiredException : Exception("Token expired")

    private fun execute(request: Request): String? {
        return try {
            http.newCall(request).execute().use { r ->
                if (r.isSuccessful) return r.body?.string()

                val body = r.body?.string()
                Log.w(TAG, "API error ${r.code}: $body")

                if (r.code == 401) {
                    val oldToken = request.header("Authorization")?.removePrefix("Bearer ")
                    val newToken = refreshToken(oldToken)
                    if (newToken != null) {
                        val retry = request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        return http.newCall(retry).execute().use { r2 ->
                            if (r2.isSuccessful) r2.body?.string()
                            else {
                                Log.e(TAG, "Retry also failed: ${r2.code}")
                                throw AuthExpiredException()
                            }
                        }
                    }
                    throw AuthExpiredException()
                }
                null
            }
        } catch (e: AuthExpiredException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            null
        }
    }

    private fun refreshToken(oldToken: String?): String? {
        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        val accountName = prefs.getString(Prefs.KEY_ACCOUNT, null) ?: return null
        val am = AccountManager.get(context)

        if (oldToken != null) am.invalidateAuthToken(ACCOUNT_TYPE, oldToken)

        return try {
            val account = Account(accountName, ACCOUNT_TYPE)
            val bundle = am.getAuthToken(account, SCOPE, null, false, null, null).result
            val newToken = bundle?.getString(AccountManager.KEY_AUTHTOKEN)
            if (newToken != null) {
                prefs.edit().putString(Prefs.KEY_TOKEN, newToken).apply()
                Log.d(TAG, "Token refreshed silently")
            }
            newToken
        } catch (e: Exception) {
            Log.e(TAG, "Silent token refresh failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "TasksApiClient"
        private const val ACCOUNT_TYPE = "com.google"
        private const val SCOPE = "oauth2:https://www.googleapis.com/auth/tasks"
        private const val TASKS_BASE = "https://www.googleapis.com/tasks/v1/lists/@default/tasks"
    }
}

sealed class AuthResult {
    data class Token(val token: String) : AuthResult()
    data class ConsentRequired(val intent: Intent) : AuthResult()
    data class Failed(val message: String) : AuthResult()
}
