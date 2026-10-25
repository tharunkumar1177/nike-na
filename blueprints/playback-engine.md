# Playback Engine

## Outcome or responsibility

Audio keeps playing in the background with full system integration: media notification, lock screen, Quick Settings media controls, Bluetooth and headset keys, audio focus, and pause when headphones disconnect.

## Current verified status

**Status:** Planned.

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

- `core/playback/` — planned

## Acceptance or verification criteria

- [ ] Tapping a song plays it, and a media notification with play/pause/next/previous appears.
- [ ] Playback continues with the screen off and the app swiped away from recents.
- [ ] Unplugging headphones pauses playback; an incoming call pauses, then resumes.
- [ ] Unit tests cover the `PlayerState` mapping and queue persistence.

## Remaining gaps and unknowns

- Android Auto (`MediaLibrarySession` browse tree), equalizer, crossfade, and ReplayGain are later slices.
