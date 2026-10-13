// Top-level build file. Plugin versions are pinned here.
// AGP 8.7.3 requires Gradle 8.9+ and JDK 17.
// Kotlin 2.0.x uses the Compose compiler Gradle plugin (same version as Kotlin).
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
