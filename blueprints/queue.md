# Queue

## Outcome or responsibility

Users see and control what plays next: a visible "Up next" list with Play next and Add to queue, drag-to-reorder, swipe-to-remove with undo, shuffle, and repeat.

## Current verified status

**Status:** Partial. The queue sheet, play next, add to queue, reorder by handle, swipe-to-remove with undo, shuffle, repeat, and persistence are implemented and compile in CI. Play-next ordering and shuffle restore are unit-tested; the sheet interactions are unverified on a device.

## Architecture dependencies

- [Playback boundary](../architecture.md#playback-boundary) — the queue is the player timeline; changes go through `PlayerController`.
- [Error and recovery](../architecture.md#error-and-recovery) — remove and clear offer undo.

## Local rules and implications

- "Play next" inserts directly after the current item; repeated "Play next" calls keep their call order (each goes after the previous play-next item).
- "Add to queue" appends to the end.
- Shuffle reorders only upcoming items and keeps the current track; turning shuffle off restores the original order.
- A track deleted from the device is removed from the queue silently.
- Shuffle is a real reorder of the player timeline, done in `PlaybackService` through a `ForwardingPlayer`, with ExoPlayer's shuffle order left unshuffled. Every controller (notification, Bluetooth, the app) therefore sees the same order. Duplicate tracks are tracked by item identity, not by id.
- Controllers send id-only `MediaItem`s; the service resolves them to full items in `onAddMediaItems`.

### Design plan (approved 2026-10-25)

- `ModalBottomSheet` titled "Up next" with shuffle and repeat toggles in the header.
- Current track row highlighted in primary color with an animated bars indicator; upcoming rows have a 24dp drag handle on the right; dragged item lifts to 8dp.
- Swipe left removes with snackbar "Removed from queue" / "Undo".

## Related blueprints

### Required

- [playback-engine](playback-engine.md) — owns the timeline and persistence.

### Impact checks

- [playlists-favorites](playlists-favorites.md) — when "Save queue as playlist" changes.

## Relevant implementation and tests

- `feature/player/QueueSheet.kt` — `QueueActions` contract and the sheet. It uses the `sh.calvin.reorderable` drag handles, `SwipeToDismissBox` for removal, and an in-sheet snackbar with Undo.
- `core/playback/QueueRules.kt` — `PlayNextCursor`, `ShuffleRules`, `QueueSnapshot`.
- `core/playback/PlayerController.kt` — `playNext`, `addToQueue`, `moveQueueItem`, `removeQueueItem`, `insertQueueItem`, `setShuffle`, `cycleRepeat`.
- `core/playback/PlaybackService.kt`, `core/playback/QueueStore.kt` — shuffle reorder and persistence.
- Tests: `app/src/test/java/com/music/player/core/playback/QueueRulesTest.kt`.

## Acceptance or verification criteria

- [ ] The queue sheet shows the current track and upcoming tracks.
- [ ] Reorder and remove work, and remove can be undone.
- [x] Unit tests cover play-next ordering and shuffle restore.

## Remaining gaps and unknowns

- The sheet criteria need a device.
- The error-and-recovery dependency above expects Clear to offer undo, but the sheet has no Clear action yet.
- "Save queue as playlist" is not implemented.
