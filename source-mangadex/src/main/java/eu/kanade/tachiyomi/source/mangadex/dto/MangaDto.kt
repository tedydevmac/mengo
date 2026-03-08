package eu.kanade.tachiyomi.source.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class MangaListResponse(
    val result: String,
    val data: List<MangaData> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
data class MangaResponse(
    val result: String,
    val data: MangaData,
)

@Serializable
data class MangaData(
    val id: String,
    val type: String = "",
    val attributes: MangaAttributes,
    val relationships: List<Relationship> = emptyList(),
)

@Serializable
data class MangaAttributes(
    val title: Map<String, String> = emptyMap(),
    val altTitles: List<Map<String, String>> = emptyList(),
    val description: Map<String, String?>? = null,
    val status: String? = null,
    val year: Int? = null,
    val contentRating: String? = null,
    val tags: List<TagData> = emptyList(),
    val originalLanguage: String? = null,
    val lastChapter: String? = null,
    val lastVolume: String? = null,
    val publicationDemographic: String? = null,
)
