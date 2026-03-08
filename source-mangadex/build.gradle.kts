plugins {
    id("mihon.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.source.mangadex"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.sourceApi)
    implementation(projects.core.common)

    // Networking
    implementation(libs.bundles.okhttp)

    // Serialization
    implementation(kotlinx.bundles.serialization)

    // Coroutines
    implementation(platform(kotlinx.coroutines.bom))
    implementation(kotlinx.bundles.coroutines)

    // DI
    implementation(libs.injekt)

    // Logging
    implementation(libs.logcat)

    // Preferences
    implementation(libs.preferencektx)
}
