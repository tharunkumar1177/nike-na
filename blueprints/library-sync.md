# Library Sync

## Outcome or responsibility

The app requests audio access, indexes the device's audio files from `MediaStore` into a Room cache, and keeps that cache current without blocking the UI, even for libraries of 50,000 tracks.

## Current verified status

**Status:** Partial. Permission handling, the `MediaStore` scanner, the incremental sync, and the Room cache are implemented and compile in CI; tag fallbacks and the sync diff rules are unit-tested. Permission dialogs, observer timing, and large-library performance are unverified on a device.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — cache rows are keyed by `trackId`, `albumId`, `artistId`, and folder relative path.
- [Library synchronization](../architecture.md#library-synchronization) — this blueprint implements the generation-based incremental sync and the debounced observer.
- [Threading](../architecture.md#threading) — all queries and writes run on `Dispatchers.IO`.
- [Error and recovery](../architecture.md#error-and-recovery) — the permission result is exposed as `LibraryAccess`, not as an error.

## Local rules and implications

- The first sync on an empty cache reads in pages (for example, 500 rows) and commits each page so the list fills progressively.
- Tracks shorter than a user-configurable minimum (default 30 s) and `IS_RINGTONE`, `IS_NOTIFICATION`, `IS_ALARM` rows are excluded from the library.
- Android 14+ partial media access applies to images and video only, not audio, so `LibraryAccess` has just `Granted` and `Denied`. The design originally planned a `Partial` state; the code omits it because it can't occur.
- Access is re-read on every resume (`MusicApp` calls `refreshAccess`), and the first `Granted` result starts the sync.
- Changing the minimum length clears the per-volume cursors, so the next sync is a full scan that applies the new filter.
- Unknown tags fall back to the file name (title), "Unknown artist", and the folder name (album).

## Related blueprints

### Required

None

### Impact checks

- [library-browsing](library-browsing.md) — when entity shape or DAO queries change.
- [queue](queue.md) — when deletion handling changes.

## Relevant implementation and tests

- `core/data/sync/MediaStoreScanner.kt` — volumes, paged changed rows (500 per page), current id set, and exclusion of ringtone, notification, alarm, recording, and short files.
- `core/data/sync/LibrarySync.kt` — mutex-guarded sync, 1 s debounced `ContentObserver`, `scanning` state.
- `core/data/sync/TagRules.kt` — tag cleaning and fallbacks, `SyncPlanner.deletions` and `needsFullScan`.
- `core/data/db/` — `TrackEntity`, FTS table, DAOs, `MusicDatabase`.
- `core/data/LibraryRepository.kt` — `access`, `refreshAccess`, `rescan`, and the library flows.
- `core/data/prefs/UserPreferences.kt` — minimum length and per-volume cursors.
- `feature/library/PermissionScreen.kt` — rationale and "Open settings" path.
- `feature/settings/SettingsScreen.kt` — "Hide short audio" choice and "Rescan library".
- Tests: `app/src/test/java/com/music/player/core/data/sync/TagRulesTest.kt`.

## Acceptance or verification criteria

- [ ] On first launch, the app asks for `READ_MEDIA_AUDIO` and shows a clear rationale when it is denied.
- [ ] After access is granted, local songs appear without restarting the app.
- [ ] A file added or deleted by another app appears or disappears within a few seconds while the app is open.
- [x] Unit tests cover the sync diff (removed rows and full-scan conditions) and tag fallbacks.

## Remaining gaps and unknowns

- The three device-behavior criteria above need a device; CI has no instrumented tests.
- Added and changed rows are covered only by compile checks, not unit tests, because they come straight from `MediaStore` cursors.
- Sync performance at 50,000 tracks is unverified until it is measured on a device.
