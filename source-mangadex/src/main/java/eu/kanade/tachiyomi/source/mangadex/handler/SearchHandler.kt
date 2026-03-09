package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import android.util.Log
import eu.kanade.tachiyomi.source.mangadex.dto.MangaAttributes
import eu.kanade.tachiyomi.source.mangadex.dto.MangaData
import eu.kanade.tachiyomi.source.mangadex.dto.MangaListResponse
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

class SearchHandler(
    private val client: OkHttpClient,
    private val json: Json,
    private val lang: String = MangaDexConstants.SOURCE_LANG,
    private val titleLangProvider: () -> String = { "en" },
) {

    suspend fun fetchPopularManga(page: Int): MangasPage = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga".toHttpUrl().newBuilder().apply {
            addQueryParameter("limit", MangaDexConstants.Limits.MANGA_LIMIT.toString())
            addQueryParameter("offset", ((page - 1) * MangaDexConstants.Limits.MANGA_LIMIT).toString())
            addQueryParameter("order[followedCount]", "desc")
            addQueryParameter("availableTranslatedLanguage[]", lang)
            addQueryParameter("includes[]", "cover_art")
            addQueryParameter("includes[]", "author")
            addQueryParameter("includes[]", "artist")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("contentRating[]", "suggestive")
            addQueryParameter("contentRating[]", "erotica")
            addQueryParameter("hasAvailableChapters", "true")
        }.build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val mangaListResponse = json.decodeFromString<MangaListResponse>(response.body.string())

        val hasNextPage = (page * MangaDexConstants.Limits.MANGA_LIMIT) < mangaListResponse.total
        val titleLang = titleLangProvider()
        val mangas = mangaListResponse.data.map { it.toSManga(titleLang) }
        MangasPage(mangas, hasNextPage)
    }

    suspend fun fetchSearchManga(page: Int, query: String, filters: FilterList): MangasPage = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga".toHttpUrl().newBuilder().apply {
            addQueryParameter("limit", MangaDexConstants.Limits.MANGA_LIMIT.toString())
            addQueryParameter("offset", ((page - 1) * MangaDexConstants.Limits.MANGA_LIMIT).toString())
            if (query.isNotBlank()) {
                addQueryParameter("title", query)
            }
            addQueryParameter("availableTranslatedLanguage[]", lang)
            addQueryParameter("includes[]", "cover_art")
            addQueryParameter("includes[]", "author")
            addQueryParameter("includes[]", "artist")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("contentRating[]", "suggestive")
            addQueryParameter("contentRating[]", "erotica")
            addQueryParameter("order[relevance]", "desc")
            addQueryParameter("hasAvailableChapters", "true")
        }.build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val mangaListResponse = json.decodeFromString<MangaListResponse>(response.body.string())

        val hasNextPage = (page * MangaDexConstants.Limits.MANGA_LIMIT) < mangaListResponse.total
        val titleLang = titleLangProvider()
        val mangas = mangaListResponse.data.map { it.toSManga(titleLang) }
        MangasPage(mangas, hasNextPage)
    }

    suspend fun fetchLatestUpdates(page: Int): MangasPage = withIOContext {
        val url = "${MangaDexConstants.BASE_URL}/manga".toHttpUrl().newBuilder().apply {
            addQueryParameter("limit", MangaDexConstants.Limits.MANGA_LIMIT.toString())
            addQueryParameter("offset", ((page - 1) * MangaDexConstants.Limits.MANGA_LIMIT).toString())
            addQueryParameter("order[latestUploadedChapter]", "desc")
            addQueryParameter("availableTranslatedLanguage[]", lang)
            addQueryParameter("includes[]", "cover_art")
            addQueryParameter("includes[]", "author")
            addQueryParameter("includes[]", "artist")
            addQueryParameter("contentRating[]", "safe")
            addQueryParameter("contentRating[]", "suggestive")
            addQueryParameter("contentRating[]", "erotica")
            addQueryParameter("hasAvailableChapters", "true")
        }.build()

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val mangaListResponse = json.decodeFromString<MangaListResponse>(response.body.string())

        val hasNextPage = (page * MangaDexConstants.Limits.MANGA_LIMIT) < mangaListResponse.total
        val titleLang = titleLangProvider()
        val mangas = mangaListResponse.data.map { it.toSManga(titleLang) }
        MangasPage(mangas, hasNextPage)
    }
}

private fun MangaAttributes.resolveTitle(lang: String): String {
    title[lang]?.let { return it }
    altTitles.forEach { map -> map[lang]?.let { return it } }
    return title.values.firstOrNull()
        ?: altTitles.firstNotNullOfOrNull { it.values.firstOrNull() }
        ?: ""
}

internal fun MangaData.toSManga(titleLang: String = "en"): SManga = SManga.create().apply {
    url = "/manga/$id"
    title = attributes.resolveTitle(titleLang)
    val coverFileName = relationships
        .firstOrNull { it.type == "cover_art" }
        ?.attributes?.fileName
    thumbnail_url = coverFileName
        ?.let { "${MangaDexConstants.CDN_URL}/covers/$id/$it.256.jpg" }
    Log.d("MangaDex", "toSManga: title=$title, coverFileName=$coverFileName, thumbnail_url=$thumbnail_url")
    author = relationships
        .filter { it.type == "author" }
        .mapNotNull { it.attributes?.name }
        .joinToString(", ")
    artist = relationships
        .filter { it.type == "artist" }
        .mapNotNull { it.attributes?.name }
        .joinToString(", ")
    description = attributes.description?.get("en")
        ?: attributes.description?.values?.firstOrNull()
        ?: ""
    status = when (attributes.status) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.COMPLETED
        "hiatus" -> SManga.ON_HIATUS
        "cancelled" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }
    genre = attributes.tags
        .mapNotNull { it.attributes?.name?.get("en") }
        .joinToString(", ")
}
