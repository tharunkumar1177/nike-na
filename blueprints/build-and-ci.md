# Build and CI

## Outcome or responsibility

Every push and pull request is built, linted, and tested by GitHub Actions, which publishes a debug APK artifact. CI is the only build authority.

## Current verified status

**Status:** Partial. The workflow and Gradle build exist, and CI passes lint, unit tests, and `assembleDebug` on the current branch. Launching the APK on a device is not verified by CI.

## Architecture dependencies

- [Platform and toolchain](../architecture.md#platform-and-toolchain) — every plugin and library version comes from `gradle/libs.versions.toml`, matching that table.
- [Verification boundaries](../architecture.md#verification-boundaries) — the workflow runs exactly `lint`, `testDebugUnitTest`, and `assembleDebug`.

## Local rules and implications

- No Gradle wrapper JAR is committed because none can be generated locally. CI installs Gradle with `gradle/actions/setup-gradle@v6` and a pinned `gradle-version`, then calls `gradle` directly.
- Actions are pinned by major tag: `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 21), `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`.
- The workflow uploads the lint report and unit-test reports when a job fails, so the reviewer can diagnose it without rerunning.
- AGP 9 built-in Kotlin is used; the separate `org.jetbrains.kotlin.android` plugin is not applied.

## Related blueprints

### Required

None

### Impact checks

- Any blueprint — check it when a dependency version changes in the catalog.

## Relevant implementation and tests

- `.github/workflows/ci.yml` — triggers on `push`, `pull_request`, and `workflow_dispatch`; uploads `music-debug-apk` and, on failure, `build-reports`.
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` — the root declares `kotlin-android` with `apply false` only to pin the Kotlin Gradle plugin version; the app module uses AGP built-in Kotlin, KSP, and the Room Gradle plugin (`app/schemas/`).

## Acceptance or verification criteria

- [x] A push to any branch triggers the workflow.
- [x] Lint, unit tests, and `assembleDebug` pass.
- [x] A debug APK artifact is downloadable from the run.
- [ ] The APK launches to an app shell screen on an Android 13+ device.

## Remaining gaps and unknowns

- No instrumented (device or emulator) tests run in CI, so on-device behavior across all blueprints is unverified.
- Release signing and Play Store distribution are out of scope until requested.
