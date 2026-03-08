package eu.kanade.tachiyomi.source.mangadex

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class MangaDexTokenInterceptor(
    private val authManager: MangaDexAuthManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Only add auth to MangaDex API requests (not CDN/uploads)
        if (request.url.host != "api.mangadex.org") {
            return chain.proceed(request)
        }

        if (!authManager.isLoggedIn()) {
            return chain.proceed(request)
        }

        // Proactive refresh if token is expired
        if (authManager.isTokenExpired() && !authManager.wasTokenRefreshedRecently()) {
            runBlocking { authManager.refreshAccessToken() }
        }

        val token = authManager.getAccessToken() ?: return chain.proceed(request)

        val authedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authedRequest)
    }
}
