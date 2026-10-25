// Root build script: declares plugins for the app module without applying them here.
// The Kotlin Android plugin is declared (not applied) only to pin the Kotlin Gradle Plugin version
// used by AGP's built-in Kotlin support.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
