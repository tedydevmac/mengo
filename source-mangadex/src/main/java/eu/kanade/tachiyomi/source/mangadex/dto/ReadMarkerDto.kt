package eu.kanade.tachiyomi.source.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadMarkersResponse(
    val result: String,
    val data: List<String> = emptyList(),
)

@Serializable
data class MarkChaptersRequest(
    val chapterIdsRead: List<String> = emptyList(),
    val chapterIdsUnread: List<String> = emptyList(),
)

@Serializable
data class MangaStatusResponse(
    val result: String,
    val statuses: Map<String, String?> = emptyMap(),
)
