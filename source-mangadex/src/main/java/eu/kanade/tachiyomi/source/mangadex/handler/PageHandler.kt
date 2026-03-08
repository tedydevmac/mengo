package eu.kanade.tachiyomi.source.mangadex.handler

import eu.kanade.tachiyomi.source.mangadex.MangaDexConstants
import eu.kanade.tachiyomi.source.mangadex.dto.AtHomeResponse
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

class PageHandler(
    private val client: OkHttpClient,
    private val json: Json,
    private val useDataSaver: () -> Boolean = { false },
) {

    suspend fun fetchPageList(chapter: SChapter): List<Page> = withIOContext {
        val chapterId = chapter.url.substringAfterLast("/")
        val url = "${MangaDexConstants.BASE_URL}/at-home/server/$chapterId"

        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val atHome = json.decodeFromString<AtHomeResponse>(response.body.string())

        val dataSaver = useDataSaver()
        val filenames = if (dataSaver) atHome.chapter.dataSaver else atHome.chapter.data
        val pathPrefix = if (dataSaver) "data-saver" else "data"

        filenames.mapIndexed { index, filename ->
            Page(
                index = index,
                imageUrl = "${atHome.baseUrl}/$pathPrefix/${atHome.chapter.hash}/$filename",
            )
        }
    }
}
