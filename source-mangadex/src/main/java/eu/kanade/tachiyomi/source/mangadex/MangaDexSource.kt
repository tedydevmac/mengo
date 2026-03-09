package eu.kanade.tachiyomi.source.mangadex

import eu.kanade.tachiyomi.source.mangadex.handler.ChapterHandler
import eu.kanade.tachiyomi.source.mangadex.handler.FollowsHandler
import eu.kanade.tachiyomi.source.mangadex.handler.MangaHandler
import eu.kanade.tachiyomi.source.mangadex.handler.PageHandler
import eu.kanade.tachiyomi.source.mangadex.handler.ReadStatusHandler
import eu.kanade.tachiyomi.source.mangadex.handler.SearchHandler
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class MangaDexSource(
    private val authManager: MangaDexAuthManager,
    private val json: Json,
    private val titleLangProvider: () -> String = { "en" },
) : HttpSource() {

    override val name: String = MangaDexConstants.SOURCE_NAME
    override val baseUrl: String = MangaDexConstants.BASE_URL
    override val lang: String = MangaDexConstants.SOURCE_LANG
    override val supportsLatest: Boolean = true

    override fun headersBuilder() = super.headersBuilder().apply {
        add("Referer", "https://mangadex.org")
    }

    override val client: OkHttpClient by lazy {
        network.client.newBuilder()
            .addInterceptor(MangaDexTokenInterceptor(authManager))
            .authenticator(MangaDexTokenAuthenticator(authManager))
            .rateLimit(permits = 5)
            .build()
    }

    private val searchHandler by lazy { SearchHandler(client, json, titleLangProvider = titleLangProvider) }
    private val mangaHandler by lazy { MangaHandler(client, json, titleLangProvider = titleLangProvider) }
    private val chapterHandler by lazy { ChapterHandler(client, json) }
    private val pageHandler by lazy { PageHandler(client, json) }
    val followsHandler by lazy { FollowsHandler(client, json, titleLangProvider = titleLangProvider) }
    val readStatusHandler by lazy { ReadStatusHandler(client, json) }

    // ====== Suspend function overrides (the actual implementation) ======

    override suspend fun getPopularManga(page: Int): MangasPage {
        return searchHandler.fetchPopularManga(page)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return searchHandler.fetchSearchManga(page, query, filters)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return searchHandler.fetchLatestUpdates(page)
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        return mangaHandler.fetchMangaDetails(manga)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        return chapterHandler.fetchChapterList(manga)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return pageHandler.fetchPageList(chapter)
    }

    // ====== Abstract methods from HttpSource (unused - we override suspend functions directly) ======

    override fun popularMangaRequest(page: Int): Request =
        throw UnsupportedOperationException("Not used")

    override fun popularMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Not used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException("Not used")

    override fun searchMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException("Not used")

    override fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Not used")

    override fun mangaDetailsParse(response: Response): SManga =
        throw UnsupportedOperationException("Not used")

    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException("Not used")

    override fun chapterPageParse(response: Response): SChapter =
        throw UnsupportedOperationException("Not used")

    override fun pageListParse(response: Response): List<Page> =
        throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException("Not used")

    // ====== MangaDex-specific public API ======

    fun isLoggedIn(): Boolean = authManager.isLoggedIn()

    suspend fun fetchAllFollows(): List<SManga> = followsHandler.fetchAllFollows()

    suspend fun fetchAllReadingStatuses(): Map<String, String> = followsHandler.fetchAllReadingStatuses()

    // ====== Web URL helpers ======

    override fun getMangaUrl(manga: SManga): String {
        val mangaId = manga.url.substringAfterLast("/")
        return "https://mangadex.org/title/$mangaId"
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val chapterId = chapter.url.substringAfterLast("/")
        return "https://mangadex.org/chapter/$chapterId"
    }

    companion object {
        val ID: Long by lazy {
            val key = "${MangaDexConstants.SOURCE_NAME.lowercase()}/${MangaDexConstants.SOURCE_LANG}/1"
            val bytes = java.security.MessageDigest.getInstance("MD5").digest(key.toByteArray())
            (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
        }
    }
}
