# System Architecture

## Purpose and authority

This document governs the shared technical contracts of **Music**, an offline local-file music player for Android. Responsibility-specific detail lives in the blueprints routed by [the blueprint manifest](blueprints/README.md). Inspected code and tests override stale prose; a conflict must be fixed here first, then in each manifest consumer.

Status of this document: **Planned**. No application code exists yet. Every contract below is an intended requirement until a blueprint cites code and tests that verify it.

## System boundaries

- **In scope:** playback of audio files already on the device, indexed through Android `MediaStore`; library browsing, queue, playlists, favorites, search, share, and delete.
- **External actors:** the Android `MediaStore` provider (source of truth for files), the system media session surfaces (notification, lock screen, Bluetooth/headset keys, Quick Settings media controls), other apps that receive share intents.
- **Excluded:** streaming, accounts, analytics, ads, cloud sync. The app declares **no `INTERNET` permission**; this is a product promise, not an optimization.

## Platform and toolchain

Versions verified against Google Maven, Maven Central, and GitHub releases on 2026-10-25. Change them only through the version catalog `gradle/libs.versions.toml`.

| Area | Choice | Version |
|---|---|---|
| Language | Kotlin (AGP built-in Kotlin support) | 2.4.21 |
| Build | Gradle / Android Gradle Plugin | 9.8.2 / 9.4.1 |
| Annotation processing | KSP | 2.3.12 |
| UI | Jetpack Compose BOM, Material 3 | 2026.09.00 |
| Playback | AndroidX Media3 (ExoPlayer, Session) | 1.11.1 |
| Persistence | Room / DataStore Preferences | 2.8.5 / 1.3.0 |
| Dependency injection | Koin | 4.2.2 |
| Images | Coil 3 | 3.6.3 |
| Navigation / Lifecycle / Activity | navigation-compose / lifecycle / activity-compose | 2.10.2 / 2.11.0 / 2.13.0 |
| Async | kotlinx-coroutines | 1.11.0 |
| SDK levels | minSdk / compileSdk / targetSdk | 33 / 37 / 36 |

compileSdk is 37 because Compose 1.12, core-ktx 1.19, and lifecycle 2.11 require compiling against API 37; targetSdk stays 36 so runtime behavior follows Android 16 rules.

Rationale: native Kotlin keeps `MediaSessionService`, `MediaStore` observation, and `android.media.audiofx` first-class with no bridge layer; cross-platform stacks would need native plugins for exactly the parts that matter most. `minSdk 33` (Android 13) gives a single permission model (`READ_MEDIA_AUDIO`, `POST_NOTIFICATIONS`) and guaranteed Material You dynamic color.

## Component model

Single Gradle module `:app`, application id and root package `com.music.player`, organized package-by-responsibility. Dependency direction is strictly downward:

```
feature.*  ──▶  core.playback  ──▶  core.model
    │                                   ▲
    └────────▶  core.data  ─────────────┘
core.ui  ◀── feature.*   (core.ui depends only on core.model)
```

| Package | Owns |
|---|---|
| `core.model` | Immutable domain types. No Android framework imports except `Uri`. |
| `core.data` | `MediaStore` sync, Room database, DataStore, repositories. |
| `core.playback` | `PlaybackService` (`MediaSessionService` + ExoPlayer) and `PlayerController` (client wrapper around `MediaController`). |
| `core.ui` | Theme, design tokens, shared composables, artwork color extraction. |
| `feature.*` | Screens and their ViewModels. Features never import each other. |

## Shared contracts

### Track identity

- A track is identified by its `MediaStore.Audio.Media._ID` as `Long trackId`. The playable URI is always `ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, trackId)`; file paths are never used for playback or identity.
- The Media3 `MediaItem.mediaId` is `trackId.toString()`. Any layer that crosses the playback boundary converts with exactly this rule.
- Albums use `MediaStore` `ALBUM_ID`; artists use `ARTIST_ID`; folders use the relative path (`RELATIVE_PATH` + volume name). Playlists and favorites use Room-generated `Long` ids and reference tracks by `trackId`.

### Library synchronization

- `MediaStore` is the source of truth; Room is a read-optimized cache. The UI reads only from Room via `Flow`.
- Sync is incremental: the last synced `MediaStore.getGeneration(volume)` per volume is stored; a sync reads rows with `GENERATION_MODIFIED` greater than it, plus a full id-set diff to detect deletions.
- Sync is triggered on app start, on permission grant, and by a debounced (≥ 1 s) `ContentObserver` on `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
- Writes to Room happen in batched transactions on `Dispatchers.IO`. The UI must never block on a sync.
- When a track disappears, it is removed from the cache; playlist entries referencing it are kept but rendered as unavailable, and the queue drops it.

### Playback boundary

- `PlaybackService` exclusively owns the ExoPlayer instance and the `MediaSession`. It declares `foregroundServiceType="mediaPlayback"` and handles audio focus via `setAudioAttributes(..., handleAudioFocus = true)` and `setHandleAudioBecomingNoisy(true)`.
- UI code never touches ExoPlayer. It talks to `PlayerController`, which connects a `MediaController` and exposes a single `StateFlow<PlayerState>` plus intent methods (`play(tracks, startIndex)`, `playNext`, `addToQueue`, `toggle`, `seekTo`, `skip`, `setShuffle`, `setRepeat`, `moveQueueItem`, `removeQueueItem`).
- The queue lives in the player's timeline; it is persisted (track ids, index, position) to DataStore on change so it is restored after process death.

### Threading

- Media3 player and controller calls happen on the main thread.
- `MediaStore`, Room, DataStore, and artwork decoding run on `Dispatchers.IO`.
- ViewModels expose `StateFlow` collected with lifecycle awareness (`collectAsStateWithLifecycle`).

### Error and recovery

- Repositories return `Result<T>` for one-shot operations; streams expose failures as a sealed UI state, never as thrown exceptions reaching Compose.
- User-visible failures appear as a non-blocking snackbar with an action when one exists (retry, undo, open settings).
- Missing permission is a first-class state (`LibraryAccess.Denied | Partial | Granted`), not an error.
- Destructive actions (delete, remove from playlist, clear queue) offer undo; file deletion goes through `MediaStore.createDeleteRequest` / `createTrashRequest`, so the system confirms it.
- A track that fails to decode is skipped with a snackbar; playback continues with the next item.

### Visual system

- Material 3 theme whose color scheme is derived from the current track's artwork (Palette-based seed), falling back to system dynamic color, then to the static brand scheme.
- Animations respect the system animator duration scale; when it is 0, transitions are instant.
- Primary playback controls are placed in the bottom third of the screen. Swiping down on the now-playing screen only collapses it to the mini player.
- Missing artwork renders a generated gradient tile from the album title, never a generic grey icon.

### Code standards

- Every source file starts with a header comment describing its purpose; every function, including composables, has a KDoc comment.
- No mock, fake, or placeholder data in production code. Tests may use in-memory Room and test doubles in test source sets only.

## Cross-cutting flows

**Play a song:** library screen → `PlayerController.play(tracks, index)` → `MediaController` → `PlaybackService` → ExoPlayer → `PlayerState` flow → mini player and now-playing UI update; theme seed updates from the new artwork.

**File deleted outside the app:** `ContentObserver` → debounced sync → Room update → library lists update; `PlaybackService` removes the missing `mediaId` from the queue.

## Verification boundaries

- GitHub Actions CI is the only build authority: `lint`, `testDebugUnitTest`, `assembleDebug`. Nothing is built locally.
- Unit tests (JVM) cover mappers, sync diffing, queue logic, and ViewModels. Room DAOs are tested with Robolectric or in-memory databases.
- Behavior that depends on a real device (notification, Bluetooth keys) is verified manually from the CI-built APK and recorded as such in the owning blueprint.

## Blueprints

See [the blueprint manifest](blueprints/README.md).
