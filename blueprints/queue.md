# Queue

## Outcome or responsibility

Users see and control what plays next: a visible "Up next" list with Play next and Add to queue, drag-to-reorder, swipe-to-remove with undo, shuffle, and repeat.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Playback boundary](../architecture.md#playback-boundary) — the queue is the player timeline; changes go through `PlayerController`.
- [Error and recovery](../architecture.md#error-and-recovery) — remove and clear offer undo.

## Local rules and implications

- "Play next" inserts directly after the current item; repeated "Play next" calls keep their call order (each goes after the previous play-next item).
- "Add to queue" appends to the end.
- Shuffle reorders only upcoming items and keeps the current track; turning shuffle off restores the original order.
- A track deleted from the device is removed from the queue silently.

## Related blueprints

### Required

- [playback-engine](playback-engine.md) — owns the timeline and persistence.

### Impact checks

- [playlists-favorites](playlists-favorites.md) — when "Save queue as playlist" changes.

## Relevant implementation and tests

- `feature/player/queue/`, `core/playback/` — planned

## Acceptance or verification criteria

- [ ] The queue sheet shows the current track and upcoming tracks.
- [ ] Reorder and remove work, and remove can be undone.
- [ ] Unit tests cover play-next ordering and shuffle restore.

## Remaining gaps and unknowns

None
