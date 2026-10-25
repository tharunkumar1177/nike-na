# Roadmap

Ordered vertical slices for Music. Each slice is a thin, user-visible increment verified in GitHub Actions CI. Work one slice at a time on branch `slice/<n>-<name>`; route context through [the blueprint manifest](blueprints/README.md).

**Phase 0 status:** approved 2026-10-25 (native Kotlin stack, minSdk 33, app name "Music", package `com.music.player`). The user asked to ship the MVP as a whole: build slices back to back without pausing for per-slice reports, still verifying each in CI.

## MVP slices

All nine MVP slices are implemented and pass CI: lint, unit tests, and `assembleDebug`. Checked items mean the code is complete. Each acceptance criterion that needs a device stays open in its blueprint until it is tested on a phone.

- [x] **1. Walking skeleton** — Gradle version catalog, `:app` scaffold with Compose theme shell, GitHub Actions (lint, unit tests, assembleDebug, APK artifact). Blueprint: [build-and-ci](blueprints/build-and-ci.md). *Accept:* CI green and the APK opens to an app shell.
- [x] **2. Songs library** — `READ_MEDIA_AUDIO` flow, `MediaStore` sync into Room, Songs list with artwork. Blueprints: [library-sync](blueprints/library-sync.md), [library-browsing](blueprints/library-browsing.md). *Accept:* real device songs listed after granting access.
- [x] **3. Background playback** — `PlaybackService`, `PlayerController`, tap-to-play, media notification, audio focus. Blueprint: [playback-engine](blueprints/playback-engine.md). *Accept:* a tapped song plays with screen off and notification controls.
- [x] **4. Mini player and now playing** — gesture-driven expand/collapse, seek bar, artwork-tinted theme. Blueprints: [now-playing](blueprints/now-playing.md), [visual-theme](blueprints/visual-theme.md). *Accept:* the player expands and collapses fluidly and re-tints per track.
- [x] **5. Albums, artists, folders** — tabs and drill-down screens. Blueprint: [library-browsing](blueprints/library-browsing.md). *Accept:* browse and play by album, artist, and folder.
- [x] **6. Queue** — Up next sheet, play next, add to queue, reorder, remove with undo, shuffle, repeat, persistence. Blueprint: [queue](blueprints/queue.md). *Accept:* reorder the queue, and it survives an app restart.
- [x] **7. Playlists and favorites** — Room playlists CRUD, favorites. Blueprint: [playlists-favorites](blueprints/playlists-favorites.md). *Accept:* create, fill, reorder, and play a playlist; favorite songs.
- [x] **8. Search and sort** — FTS search, per-tab sort. Blueprint: [search-sort](blueprints/search-sort.md). *Accept:* type-ahead search finds songs, albums, and artists.
- [x] **9. Share and delete** — share sheet, trash or delete via `MediaStore` requests. Blueprint: [file-operations](blueprints/file-operations.md). *Accept:* a deleted song disappears from lists and the queue.

## Later slices (blueprints created when started)

- [ ] 10. Lyrics (embedded USLT and `.lrc`, synced scrolling)
- [ ] 11. Equalizer and audio effects (`android.media.audiofx`, presets)
- [ ] 12. Sleep timer and playback speed
- [ ] 13. Home-screen widgets (Glance)
- [ ] 14. Genres tab and smart collections (recently added, most played)
- [ ] 15. Android Auto (`MediaLibrarySession` browse tree)
- [ ] 16. Tag and artwork editing (batch)
