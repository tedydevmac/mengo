package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import eu.kanade.tachiyomi.source.mangadex.dto.MarkChaptersRequest
import eu.kanade.tachiyomi.source.mangadex.dto.ReadMarkersResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext

class ReadStatusHandler(
    private val client: OkHttpClient,
    private val json: Json,
) {

    suspend fun getReadChapterIds(mangaId: String): List<String> = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId/read"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val readMarkersResponse = json.decodeFromString<ReadMarkersResponse>(response.body.string())
        readMarkersResponse.data
    }

    suspend fun markChaptersRead(mangaId: String, chapterIds: List<String>) = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId/read"
        val body = json.encodeToString(
            MarkChaptersRequest(chapterIdsRead = chapterIds),
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute()
    }

    suspend fun markChaptersUnread(mangaId: String, chapterIds: List<String>) = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId/read"
        val body = json.encodeToString(
            MarkChaptersRequest(chapterIdsUnread = chapterIds),
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute()
    }
}
