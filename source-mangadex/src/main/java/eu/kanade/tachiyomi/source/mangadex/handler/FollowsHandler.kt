package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import eu.kanade.tachiyomi.source.mangadex.dto.MangaListResponse
import eu.kanade.tachiyomi.source.mangadex.dto.MangaStatusResponse
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

class FollowsHandler(
    private val client: OkHttpClient,
    private val json: Json,
) {

    suspend fun fetchAllFollows(): List<SManga> = withIOContext {
        val allManga = mutableListOf<SManga>()
        var offset = 0
        var total: Int

        do {
            val url = "${MangaDexConstants.BASE_URL}/user/follows/manga".toHttpUrl().newBuilder().apply {
                addQueryParameter("limit", MangaDexConstants.Limits.FOLLOWS_LIMIT.toString())
                addQueryParameter("offset", offset.toString())
                addQueryParameter("includes[]", "cover_art")
                addQueryParameter("includes[]", "author")
                addQueryParameter("includes[]", "artist")
            }.build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val mangaListResponse = json.decodeFromString<MangaListResponse>(response.body.string())

            total = mangaListResponse.total
            allManga.addAll(mangaListResponse.data.map { it.toSManga() })
            offset += MangaDexConstants.Limits.FOLLOWS_LIMIT
        } while (offset < total)

        allManga
    }

    suspend fun fetchAllReadingStatuses(): Map<String, String> = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga/status"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val statusResponse = json.decodeFromString<MangaStatusResponse>(response.body.string())
        statusResponse.statuses.filterValues { it != null }.mapValues { it.value!! }
    }
}
