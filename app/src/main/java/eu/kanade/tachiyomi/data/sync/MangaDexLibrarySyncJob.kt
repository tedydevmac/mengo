package eu.kanade.tachiyomi.data.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.mangadex.MangaDexSource
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.manga.interactor.GetMangaByUrlAndSourceId
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class MangaDexLibrarySyncJob(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val mangaDexSource: MangaDexSource = Injekt.get()
    private val getMangaByUrlAndSourceId: GetMangaByUrlAndSourceId = Injekt.get()
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get()
    private val getCategories: GetCategories = Injekt.get()
    private val createCategoryWithName: CreateCategoryWithName = Injekt.get()
    private val setMangaCategories: SetMangaCategories = Injekt.get()

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                return Result.retry()
            }
        }

        if (!mangaDexSource.isLoggedIn()) {
            logcat(LogPriority.WARN) { "MangaDex library sync skipped: not logged in" }
            return Result.success()
        }

        setForegroundSafely()

        return withIOContext {
            try {
                syncLibrary()
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Result.success()
                } else {
                    logcat(LogPriority.ERROR, e) { "MangaDex library sync failed" }
                    Result.failure()
                }
            }
        }
    }

    private suspend fun syncLibrary() {
        logcat { "Starting MangaDex library sync..." }

        val (follows, readingStatuses) = coroutineScope {
            val followsDeferred = async { mangaDexSource.fetchAllFollows() }
            val statusesDeferred = async { mangaDexSource.fetchAllReadingStatuses() }
            followsDeferred.await() to statusesDeferred.await()
        }
        logcat { "Fetched ${follows.size} follows and ${readingStatuses.size} statuses from MangaDex" }

        // Build status → category ID map, creating categories as needed
        val statusToCategoryId = buildStatusCategoryMap(readingStatuses.values.toSet())

        var added = 0
        var updated = 0

        for (sManga in follows) {
            val mangaId = sManga.url.substringAfterLast("/")
            val existingManga = getMangaByUrlAndSourceId.await(sManga.url, mangaDexSource.id)
            val localManga: Manga
            if (existingManga == null) {
                // Insert new manga into the database as a favorite
                val manga = Manga.create().copy(
                    url = sManga.url,
                    title = sManga.title,
                    source = mangaDexSource.id,
                    thumbnailUrl = sManga.thumbnail_url,
                    artist = sManga.artist,
                    author = sManga.author,
                    description = sManga.description,
                    genre = sManga.getGenres(),
                    status = sManga.status.toLong(),
                    favorite = true,
                    initialized = sManga.initialized,
                )
                localManga = networkToLocalManga(manga)
                added++
            } else if (!existingManga.favorite) {
                // Existing but not favorited: mark as favorite
                localManga = networkToLocalManga(existingManga.copy(favorite = true))
                updated++
            } else {
                localManga = existingManga
            }

            // Assign to category based on MangaDex reading status
            val status = readingStatuses[mangaId]
            val categoryId = status?.let { statusToCategoryId[it] }
            if (categoryId != null) {
                setMangaCategories.await(localManga.id, listOf(categoryId))
            }
        }

        logcat { "MangaDex library sync complete: $added added, $updated updated" }
    }

    private suspend fun buildStatusCategoryMap(statuses: Set<String>): Map<String, Long> {
        val statusNames = mapOf(
            "reading" to "Reading",
            "plan_to_read" to "Plan to Read",
            "completed" to "Completed",
            "re_reading" to "Re-reading",
            "on_hold" to "On Hold",
            "dropped" to "Dropped",
        )

        val existingCategories = getCategories.await()
        val categoryByName = existingCategories.associateBy { it.name }
        val result = mutableMapOf<String, Long>()

        for (status in statuses) {
            val name = statusNames[status] ?: continue
            val existing = categoryByName[name]
            if (existing != null) {
                result[status] = existing.id
            } else {
                createCategoryWithName.await(name)
                // Re-fetch to get the assigned ID
                val created = getCategories.await().find { it.name == name }
                if (created != null) {
                    result[status] = created.id
                }
            }
        }

        return result
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.ID_LIBRARY_PROGRESS,
            androidx.core.app.NotificationCompat.Builder(context, Notifications.CHANNEL_LIBRARY_PROGRESS)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Syncing MangaDex library...")
                .setOngoing(true)
                .setProgress(0, 0, true)
                .build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        private const val TAG = "MangaDexLibrarySync"
        private const val WORK_NAME_AUTO = "MangaDexLibrarySync-auto"
        private const val WORK_NAME_MANUAL = "MangaDexLibrarySync-manual"

        fun setupTask(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<MangaDexLibrarySyncJob>(
                6,
                TimeUnit.HOURS,
                10,
                TimeUnit.MINUTES,
            )
                .addTag(TAG)
                .addTag(WORK_NAME_AUTO)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()

            context.workManager.enqueueUniquePeriodicWork(
                WORK_NAME_AUTO,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun startNow(context: Context): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                return false
            }

            val request = OneTimeWorkRequestBuilder<MangaDexLibrarySyncJob>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun cancelTask(context: Context) {
            context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
        }
    }
}
