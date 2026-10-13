# Building

## On GitHub (no computer needed)

Push to `main` or run **Actions > Android Build > Run workflow**. The workflow:

1. checks out the code, installs JDK 17 and the Android SDK,
2. installs Gradle 8.9 with caching,
3. generates the Gradle wrapper if `gradle/wrapper/gradle-wrapper.jar` is missing,
4. runs `./gradlew --no-daemon assembleDebug`,
5. uploads `app/build/outputs/apk/debug/*.apk` as the artifact **PrivacyDisplay-debug-apk**.

The debug APK is signed with the standard Android debug key, so it installs directly.

## Pinned versions

| Component | Version |
| --- | --- |
| Android Gradle Plugin | 8.7.3 |
| Kotlin / Compose compiler plugin | 2.0.21 |
| Gradle | 8.9 |
| JDK | 17 |
| Compose BOM | 2024.10.01 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 |

## Locally (optional)

```sh
gradle wrapper --gradle-version 8.9   # once, if there is no gradlew yet
./gradlew assembleDebug
```
