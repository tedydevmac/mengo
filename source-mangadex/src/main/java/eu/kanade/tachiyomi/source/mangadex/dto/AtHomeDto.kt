package eu.kanade.tachiyomi.source.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class AtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter,
)

@Serializable
data class AtHomeChapter(
    val hash: String,
    val data: List<String> = emptyList(),
    val dataSaver: List<String> = emptyList(),
)
