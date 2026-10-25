# Library Sync

## Outcome or responsibility

The app requests audio access, indexes the device's audio files from `MediaStore` into a Room cache, and keeps that cache current without blocking the UI, even for libraries of 50,000 tracks.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — cache rows are keyed by `trackId`, `albumId`, `artistId`, and folder relative path.
- [Library synchronization](../architecture.md#library-synchronization) — this blueprint implements the generation-based incremental sync and the debounced observer.
- [Threading](../architecture.md#threading) — all queries and writes run on `Dispatchers.IO`.
- [Error and recovery](../architecture.md#error-and-recovery) — the permission result is exposed as `LibraryAccess`, not as an error.

## Local rules and implications

- The first sync on an empty cache reads in pages (for example, 500 rows) and commits each page so the list fills progressively.
- Tracks shorter than a user-configurable minimum (default 30 s) and `IS_RINGTONE`, `IS_NOTIFICATION`, `IS_ALARM` rows are excluded from the library.
- Android 14+ partial media access maps to `LibraryAccess.Partial` with a prompt to grant full access.
- Unknown tags fall back to the file name (title), "Unknown artist", and the folder name (album).

## Related blueprints

### Required

None

### Impact checks

- [library-browsing](library-browsing.md) — when entity shape or DAO queries change.
- [queue](queue.md) — when deletion handling changes.

## Relevant implementation and tests

- `core/data/` — planned

## Acceptance or verification criteria

- [ ] On first launch, the app asks for `READ_MEDIA_AUDIO` and shows a clear rationale when it is denied.
- [ ] After access is granted, local songs appear without restarting the app.
- [ ] A file added or deleted by another app appears or disappears within a few seconds while the app is open.
- [ ] Unit tests cover the sync diff (added, changed, removed rows) and tag fallbacks.

## Remaining gaps and unknowns

- Sync performance at 50,000 tracks is unverified until it is measured on a device.
