package eu.kanade.tachiyomi.source.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChapterListResponse(
    val result: String,
    val data: List<ChapterData> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
data class ChapterData(
    val id: String,
    val type: String = "",
    val attributes: ChapterAttributes,
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class ChapterAttributes(
    val title: String? = null,
    val volume: String? = null,
    val chapter: String? = null,
    val translatedLanguage: String = "",
    val publishAt: String? = null,
    val readableAt: String? = null,
    val pages: Int = 0,
    val externalUrl: String? = null,
)
