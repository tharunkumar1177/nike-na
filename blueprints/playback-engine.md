# Playback Engine

## Outcome or responsibility

Audio keeps playing in the background with full system integration: media notification, lock screen, Quick Settings media controls, Bluetooth and headset keys, audio focus, and pause when headphones disconnect.

## Current verified status

**Status:** Partial. `PlaybackService` (`MediaSessionService` with ExoPlayer) and `PlayerController` are implemented and compile in CI, with audio focus, pause on becoming noisy, a local wake lock, queue persistence, and a skip on decode errors. Queue rules and the persistence codec are unit-tested. Notification, lock screen, Bluetooth, and background behavior are unverified on a device.

## Architecture dependencies

- [Playback boundary](../architecture.md#playback-boundary) — this blueprint owns `PlaybackService` and `PlayerController`.
- [Track identity](../architecture.md#track-identity) — `MediaItem.mediaId` is `trackId.toString()`; URIs come from `trackId`.
- [Threading](../architecture.md#threading) — player and controller calls are main-thread only.
- [Error and recovery](../architecture.md#error-and-recovery) — decode failures skip to the next track and emit a user message.

## Local rules and implications

- The manifest declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, and the service with `foregroundServiceType="mediaPlayback"`.
- The session activity `PendingIntent` opens the app on the now-playing screen.
- Queue, index, position, shuffle, and repeat are persisted and restored on the next connection, paused.
- Gapless playback is on by default (ExoPlayer native).

## Related blueprints

### Required

None

### Impact checks

- [queue](queue.md) and [now-playing](now-playing.md) — when `PlayerState` or the controller API changes.

## Relevant implementation and tests

- `core/playback/PlaybackService.kt`:
  - ExoPlayer configuration, the shuffle `ForwardingPlayer`, and the session activity (`MainActivity.ACTION_OPEN_PLAYER`).
  - Queue restore, removal of deleted tracks, and debounced saves.
  - `onAddMediaItems` resolves id-only items, and `onPlaybackResumption` restores the queue.
- `core/playback/PlayerController.kt` — `MediaController` wrapper exposing `PlayerState`, position, and messages. `MainActivity` connects it in `onStart` and disconnects it in `onStop`.
- `core/playback/MediaItems.kt`, `core/playback/PlayerState.kt`, `core/playback/QueueStore.kt`, `core/playback/QueueRules.kt`.
- `app/src/main/AndroidManifest.xml` — permissions and the exported `mediaPlayback` service.
- Tests: `app/src/test/java/com/music/player/core/playback/QueueRulesTest.kt` (play-next cursor, shuffle and restore, snapshot codec).

## Acceptance or verification criteria

- [ ] Tapping a song plays it, and a media notification with play/pause/next/previous appears.
- [ ] Playback continues with the screen off and the app swiped away from recents.
- [ ] Unplugging headphones pauses playback; an incoming call pauses, then resumes.
- [x] Unit tests cover queue persistence (snapshot encode and decode).
- [ ] Unit tests cover the `PlayerState` mapping.

## Remaining gaps and unknowns

- The device-behavior criteria need a device; CI has no instrumented tests.
- The `PlayerState` mapping reads a live `MediaController`, so it has no JVM unit test yet.
- Android Auto (`MediaLibrarySession` browse tree), equalizer, crossfade, and ReplayGain are later slices.
