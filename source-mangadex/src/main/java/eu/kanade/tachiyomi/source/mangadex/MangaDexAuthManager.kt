package eu.kanade.tachiyomi.source.mangadex

import android.content.Context
import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.mangadex.dto.TokenResponse
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

class MangaDexAuthManager(
    context: Context,
    private val json: Json,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mangadex_auth", Context.MODE_PRIVATE)

    private val plainClient = OkHttpClient.Builder().build()

    @Volatile
    private var lastRefreshTime = 0L

    // Token management
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun isLoggedIn(): Boolean = getRefreshToken() != null

    fun saveTokens(tokenResponse: TokenResponse) {
        prefs.edit()
            .putString("access_token", tokenResponse.accessToken)
            .putString("refresh_token", tokenResponse.refreshToken)
            .putLong("token_expiry", System.currentTimeMillis() + tokenResponse.expiresIn * 1000L)
            .apply()
    }

    fun isTokenExpired(): Boolean {
        val expiry = prefs.getLong("token_expiry", 0)
        return System.currentTimeMillis() >= expiry - 60_000 // 1 min buffer
    }

    fun wasTokenRefreshedRecently(): Boolean {
        return (System.currentTimeMillis() - lastRefreshTime) < 15 * 60 * 1000
    }

    /**
     * Login using the OAuth2 password grant (for personal clients).
     * Returns true if tokens were obtained successfully.
     */
    suspend fun login(username: String, password: String): Boolean = withIOContext {
        val body = FormBody.Builder()
            .add("client_id", MangaDexConstants.CLIENT_ID)
            .add("client_secret", MangaDexConstants.CLIENT_SECRET)
            .add("grant_type", "password")
            .add("username", username)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url(MangaDexConstants.TOKEN_URL)
            .post(body)
            .build()

        try {
            val response = plainClient.newCall(request).execute()
            if (response.isSuccessful) {
                val tokenResponse = json.decodeFromString<TokenResponse>(response.body.string())
                saveTokens(tokenResponse)
                lastRefreshTime = System.currentTimeMillis()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun refreshAccessToken(): Boolean = withIOContext {
        val refreshToken = getRefreshToken() ?: return@withIOContext false

        val body = FormBody.Builder()
            .add("client_id", MangaDexConstants.CLIENT_ID)
            .add("client_secret", MangaDexConstants.CLIENT_SECRET)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url(MangaDexConstants.TOKEN_URL)
            .post(body)
            .build()

        try {
            val response = plainClient.newCall(request).execute()
            if (response.isSuccessful) {
                val tokenResponse = json.decodeFromString<TokenResponse>(response.body.string())
                saveTokens(tokenResponse)
                lastRefreshTime = System.currentTimeMillis()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun logout() {
        val accessToken = getAccessToken()
        if (accessToken != null) {
            try {
                val request = Request.Builder()
                    .url(MangaDexConstants.LOGOUT_URL)
                    .header("Authorization", "Bearer $accessToken")
                    .post(FormBody.Builder().build())
                    .build()
                plainClient.newCall(request).execute().close()
            } catch (_: Exception) {
            }
        }
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("token_expiry")
            .apply()
    }
}
