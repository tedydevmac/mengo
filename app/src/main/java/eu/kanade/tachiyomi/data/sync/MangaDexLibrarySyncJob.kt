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
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
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
        val follows = mangaDexSource.fetchAllFollows()
        logcat { "Fetched ${follows.size} follows from MangaDex" }

        var added = 0
        var updated = 0

        for (sManga in follows) {
            val existingManga = getMangaByUrlAndSourceId.await(sManga.url, mangaDexSource.id)
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
                networkToLocalManga(manga)
                added++
            } else if (!existingManga.favorite) {
                // Existing but not favorited: mark as favorite
                val updatedManga = existingManga.copy(favorite = true)
                networkToLocalManga(updatedManga)
                updated++
            }
        }

        logcat { "MangaDex library sync complete: $added added, $updated updated" }
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
