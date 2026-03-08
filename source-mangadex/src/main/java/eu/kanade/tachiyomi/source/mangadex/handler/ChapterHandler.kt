package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import eu.kanade.tachiyomi.source.mangadex.dto.ChapterData
import eu.kanade.tachiyomi.source.mangadex.dto.ChapterListResponse
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChapterHandler(
    private val client: OkHttpClient,
    private val json: Json,
    private val lang: String = MangaDexConstants.SOURCE_LANG,
) {

    suspend fun fetchChapterList(manga: SManga): List<SChapter> = withIOContext {
        val mangaId = manga.url.substringAfterLast("/")
        val chapters = mutableListOf<SChapter>()
        var offset = 0
        var total: Int

        do {
            val url = "${MangaDexConstants.BASE_URL}/manga/$mangaId/feed".toHttpUrl().newBuilder().apply {
                addQueryParameter("limit", MangaDexConstants.Limits.CHAPTER_LIMIT.toString())
                addQueryParameter("offset", offset.toString())
                addQueryParameter("translatedLanguage[]", lang)
                addQueryParameter("order[chapter]", "desc")
                addQueryParameter("includes[]", "scanlation_group")
                addQueryParameter("includes[]", "user")
                addQueryParameter("contentRating[]", "safe")
                addQueryParameter("contentRating[]", "suggestive")
                addQueryParameter("contentRating[]", "erotica")
            }.build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val chapterListResponse = json.decodeFromString<ChapterListResponse>(response.body.string())

            total = chapterListResponse.total
            chapters.addAll(
                chapterListResponse.data
                    .filter { it.attributes.externalUrl == null && it.attributes.pages > 0 }
                    .map { it.toSChapter() },
            )
            offset += MangaDexConstants.Limits.CHAPTER_LIMIT
        } while (offset < total)

        chapters
    }

    private fun ChapterData.toSChapter(): SChapter = SChapter.create().apply {
        url = "/chapter/$id"
        name = buildString {
            attributes.volume?.let { append("Vol.$it ") }
            attributes.chapter?.let { append("Ch.$it") }
            attributes.title?.let {
                if (isNotEmpty()) append(" - ")
                append(it)
            }
            if (isEmpty()) append("Oneshot")
        }
        chapter_number = attributes.chapter?.toFloatOrNull() ?: -1f
        date_upload = attributes.readableAt?.let { parseIsoDate(it) } ?: 0L
        scanlator = relationships
            .firstOrNull { it.type == "scanlation_group" }
            ?.attributes?.name
    }

    private fun parseIsoDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(dateString)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
}
