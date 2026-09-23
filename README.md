# TRACE

Original Android number-link merge game — draw matching powers of two, climb forever.

## Install on phone (automatic)

Every push to **`main`** runs [.github/workflows/release.yml](.github/workflows/release.yml), which:

1. Runs unit tests
2. Builds a signed release APK
3. Publishes/updates the **sideload** GitHub Release

On your phone: open the release → download **`TRACE-latest.apk`** → install (allow unknown sources if asked).

You can also trigger the workflow manually via **Actions → Release APK → Run workflow**.

## Stack

- Kotlin + Jetpack Compose (`minSdk 26`, `targetSdk 35`)
- Pure-Kotlin domain engine with JVM unit tests
- DataStore prefs, SoundPool SFX, VibrationEffect haptics

## Local build

```bash
export ANDROID_HOME=~/android-sdk   # or your SDK path
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

## Play

Connect adjacent tiles (8 directions). Match equals, then rise to the next power of two. Long paths score combos. Goals climb infinitely; low tiles retire from the spawn pool as you rise. Shatter unlocks at level 5, Align at level 10.
