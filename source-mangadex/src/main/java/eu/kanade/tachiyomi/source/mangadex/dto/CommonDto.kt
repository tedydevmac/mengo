package eu.kanade.tachiyomi.source.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class Relationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null,
)

@Serializable
data class RelationshipAttributes(
    val name: String? = null,
    val fileName: String? = null,
    val description: String? = null,
    val locale: String? = null,
    val username: String? = null,
    val volume: String? = null,
)

@Serializable
data class TagData(
    val id: String? = null,
    val type: String? = null,
    val attributes: TagAttributes? = null,
)

@Serializable
data class TagAttributes(
    val name: Map<String, String> = emptyMap(),
    val group: String? = null,
)
