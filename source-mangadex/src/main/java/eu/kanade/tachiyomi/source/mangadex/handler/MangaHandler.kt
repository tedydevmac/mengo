package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import eu.kanade.tachiyomi.source.mangadex.dto.MangaResponse
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

class MangaHandler(
    private val client: OkHttpClient,
    private val json: Json,
    private val titleLangProvider: () -> String = { "en" },
) {

    suspend fun fetchMangaDetails(manga: SManga): SManga = withIOContext {
        val mangaId = manga.url.substringAfterLast("/")
        val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId".toHttpUrl().newBuilder().apply {
            addQueryParameter("includes[]", "cover_art")
            addQueryParameter("includes[]", "author")
            addQueryParameter("includes[]", "artist")
        }.build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val mangaResponse = json.decodeFromString<MangaResponse>(response.body.string())

        mangaResponse.data.toSManga(titleLangProvider()).apply {
            initialized = true
        }
    }

    suspend fun fetchAllMangaTitles(manga: SManga): List<String> = withIOContext {
        val mangaId = manga.url.substringAfterLast("/")
        val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId".toHttpUrl().newBuilder().build()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val mangaResponse = json.decodeFromString<MangaResponse>(response.body.string())
        val attrs = mangaResponse.data.attributes
        val allTitles = mutableSetOf<String>()
        allTitles.addAll(attrs.title.values)
        attrs.altTitles.forEach { map -> allTitles.addAll(map.values) }
        allTitles.filter { it.isNotBlank() }.toList()
    }
}
