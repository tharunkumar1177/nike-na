# File Operations

## Outcome or responsibility

Users share audio files to other apps and delete them from the device, with system confirmation.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Error and recovery](../architecture.md#error-and-recovery) — deletion goes through `MediaStore.createTrashRequest` (recoverable) or `createDeleteRequest`.
- [Track identity](../architecture.md#track-identity) — share and delete use the content URI from `trackId`.

## Local rules and implications

- Share uses `ACTION_SEND` (or `ACTION_SEND_MULTIPLE`) with the content URI and a read-URI grant.
- Moving to trash is the default; permanent delete is a secondary choice.
- The library updates through the normal sync observer; no manual cache edits.

## Related blueprints

### Required

- [library-sync](library-sync.md) — reflects the deletion in the cache.

### Impact checks

- [queue](queue.md) and [playlists-favorites](playlists-favorites.md) — when deletion behavior changes.

## Relevant implementation and tests

- `feature/library/actions/` — planned

## Acceptance or verification criteria

- [ ] Share opens the system share sheet with the audio file.
- [ ] Delete shows the system confirmation, and the track disappears from lists and the queue.

## Remaining gaps and unknowns

- Ringtone setting and tag editing are later slices.
