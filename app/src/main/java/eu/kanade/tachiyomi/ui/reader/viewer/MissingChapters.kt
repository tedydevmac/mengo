package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import tachiyomi.domain.chapter.service.calculateChapterGap as domainCalculateChapterGap

fun calculateChapterGap(higherReaderChapter: ReaderChapter?, lowerReaderChapter: ReaderChapter?): Int {
    // Don't show gap warnings for fill chapters from alternate sources
    if (higherReaderChapter?.chapter?.url?.startsWith(ChapterLoader.FILL_PREFIX) == true ||
        lowerReaderChapter?.chapter?.url?.startsWith(ChapterLoader.FILL_PREFIX) == true) {
        return 0
    }
    return domainCalculateChapterGap(
        higherReaderChapter?.chapter?.toDomainChapter(),
        lowerReaderChapter?.chapter?.toDomainChapter(),
    )
}
