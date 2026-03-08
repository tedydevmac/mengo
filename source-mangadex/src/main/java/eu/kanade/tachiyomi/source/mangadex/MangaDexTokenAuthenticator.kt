package eu.kanade.tachiyomi.source.mangadex

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class MangaDexTokenAuthenticator(
    private val authManager: MangaDexAuthManager,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we didn't send auth in the first place
        if (response.request.header("Authorization") == null) return null

        // Avoid infinite refresh loops
        if (authManager.wasTokenRefreshedRecently()) return null

        val refreshed = runBlocking { authManager.refreshAccessToken() }
        if (!refreshed) return null

        val newToken = authManager.getAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
}
