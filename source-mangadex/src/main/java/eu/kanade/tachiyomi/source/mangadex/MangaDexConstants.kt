package eu.kanade.tachiyomi.source.mangadex

object MangaDexConstants {
    const val BASE_URL = "https://api.mangadex.org"
    const val AUTH_BASE_URL = "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect"
    const val AUTH_URL = "$AUTH_BASE_URL/auth"
    const val TOKEN_URL = "$AUTH_BASE_URL/token"
    const val LOGOUT_URL = "$AUTH_BASE_URL/logout"
    const val CDN_URL = "https://uploads.mangadex.org"

    const val CLIENT_ID = "personal-client-e81dd22d-d1be-4ec8-83fb-04f8dc9f3eb5-40f07323"
    const val CLIENT_SECRET = "CFbHQqof0vEvqmwoAeWZYGmAXLIbuQjs"
    const val REDIRECT_URI = "mengo://mangadex-auth"

    const val SOURCE_NAME = "MangaDex"
    const val SOURCE_LANG = "en"

    object Limits {
        const val MANGA_LIMIT = 20
        const val CHAPTER_LIMIT = 500
        const val FOLLOWS_LIMIT = 100
    }
}
