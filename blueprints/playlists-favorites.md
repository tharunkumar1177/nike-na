# Playlists and Favorites

## Outcome or responsibility

Users create, rename, reorder, and delete playlists, add tracks to them, and mark tracks as favorites, which appear in a built-in Favorites collection.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — playlist entries reference `trackId`; playlists have Room ids.
- [Library synchronization](../architecture.md#library-synchronization) — entries for removed tracks stay and render as unavailable.
- [Error and recovery](../architecture.md#error-and-recovery) — deleting a playlist or removing entries offers undo.

## Local rules and implications

- Playlists are app-owned in Room, not written to `MediaStore` playlists.
- Favorites is a flag set, not a playlist row, so it cannot be deleted or renamed.
- A playlist can contain the same track more than once; each entry has its own position.

### Design plan (approved 2026-10-25)

- Playlists screen: Favorites pinned first with a heart tile, then playlists (generated gradient tile, name, song count); extended FAB "New playlist"; empty copy "No playlists yet".
- Playlist detail: header with name, song count, and total duration, "Play all" / "Shuffle" buttons, drag-to-reorder rows, overflow with Rename and Delete.
- Create/rename: `AlertDialog` with `OutlinedTextField`, "Cancel" / "Save". Add-to-playlist: `ModalBottomSheet` with "New playlist" row at top.

## Related blueprints

### Required

- [library-sync](library-sync.md) — resolves `trackId` to cached track metadata.

### Impact checks

- [queue](queue.md) — when playlist playback or "save queue" changes.

## Relevant implementation and tests

- `feature/playlists/`, `core/data/` — planned

## Acceptance or verification criteria

- [ ] Create a playlist, add songs from any list, reorder them, and play the playlist.
- [ ] Tap the heart to favorite a song; it appears in Favorites.
- [ ] DAO tests cover ordering, duplicates, and unavailable entries.

## Remaining gaps and unknowns

- M3U import and export are a later slice.
