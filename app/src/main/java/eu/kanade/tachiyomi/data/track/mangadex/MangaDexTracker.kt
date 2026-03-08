package eu.kanade.tachiyomi.data.track.mangadex

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.mangadex.MangaDexAuthManager
import eu.kanade.tachiyomi.source.mangadex.MangaDexSource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

class MangaDexTracker(id: Long) : BaseTracker(id, "MangaDex") {

    companion object {
        const val READING = 1L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val DROPPED = 4L
        const val PLAN_TO_READ = 6L
        const val REREADING = 7L

        private val SCORE_LIST = IntRange(0, 10)
            .map(Int::toString)
            .toImmutableList()
    }

    private val authManager: MangaDexAuthManager by injectLazy()
    private val mangaDexSource: MangaDexSource by injectLazy()

    override fun getLogo() = R.drawable.brand_mangadex

    override fun getStatusList(): List<Long> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ, REREADING)
    }

    override fun getStatus(status: Long): StringResource? = when (status) {
        READING -> MR.strings.reading
        PLAN_TO_READ -> MR.strings.plan_to_read
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        DROPPED -> MR.strings.dropped
        REREADING -> MR.strings.repeating
        else -> null
    }

    override fun getReadingStatus(): Long = READING

    override fun getRereadingStatus(): Long = REREADING

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): ImmutableList<String> = SCORE_LIST

    override fun displayScore(track: DomainTrack): String {
        return track.score.toInt().toString()
    }

    override val isLoggedIn: Boolean
        get() = authManager.isLoggedIn()

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toLong() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                    track.finished_reading_date = System.currentTimeMillis()
                } else if (track.status != REREADING) {
                    track.status = READING
                    if (track.last_chapter_read == 1.0) {
                        track.started_reading_date = System.currentTimeMillis()
                    }
                }
            }
        }
        return track
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track {
        track.status = if (hasReadChapters) READING else PLAN_TO_READ
        track.score = 0.0
        return track
    }

    override suspend fun search(query: String): List<TrackSearch> {
        val mangasPage = mangaDexSource.getSearchManga(
            page = 1,
            query = query,
            filters = eu.kanade.tachiyomi.source.model.FilterList(),
        )
        return mangasPage.mangas.map { sManga ->
            TrackSearch.create(id).apply {
                title = sManga.title
                tracking_url = "https://mangadex.org/title/${sManga.url.substringAfterLast("/")}"
                cover_url = sManga.thumbnail_url ?: ""
                summary = sManga.description ?: ""
                publishing_status = when (sManga.status) {
                    1 -> "Ongoing"
                    2 -> "Completed"
                    3 -> "Licensed"
                    4 -> "Publishing finished"
                    5 -> "Cancelled"
                    6 -> "On hiatus"
                    else -> "Unknown"
                }
            }
        }
    }

    override suspend fun refresh(track: Track): Track {
        return track
    }

    override suspend fun login(username: String, password: String) {
        saveCredentials(username, password)
    }

    override fun logout() {
        authManager.logout()
        super.logout()
    }
}
