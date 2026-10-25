# Build and CI

## Outcome or responsibility

Every push and pull request is built, linted, and tested by GitHub Actions, which publishes a debug APK artifact. CI is the only build authority.

## Current verified status

**Status:** Planned. No workflow or Gradle files exist yet.

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

- `.github/workflows/ci.yml` — planned
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` — planned

## Acceptance or verification criteria

- [ ] A push to any branch triggers the workflow.
- [ ] Lint, unit tests, and `assembleDebug` pass.
- [ ] A debug APK artifact is downloadable from the run.
- [ ] The APK launches to an app shell screen on an Android 13+ device.

## Remaining gaps and unknowns

- Release signing and Play Store distribution are out of scope until requested.
