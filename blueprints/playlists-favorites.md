# Playlists and Favorites

## Outcome or responsibility

Users create, rename, reorder, and delete playlists, add tracks to them, and mark tracks as favorites, which appear in a built-in Favorites collection.

## Current verified status

**Status:** Partial. Room playlists and favorites, the Playlists, playlist detail, and Favorites screens, the create and rename dialog, and the add-to-playlist sheet are implemented and compile in CI. DAO behavior has no tests yet, and screen behavior is unverified on a device.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — playlist entries reference `trackId`; playlists have Room ids.
- [Library synchronization](../architecture.md#library-synchronization) — entries for removed tracks stay and render as unavailable.
- [Error and recovery](../architecture.md#error-and-recovery) — deleting a playlist or removing entries offers undo.

## Local rules and implications

- Playlists are app-owned in Room, not written to `MediaStore` playlists.
- Favorites is a flag set, not a playlist row, so it cannot be deleted or renamed.
- A playlist can contain the same track more than once; each entry has its own position.
- Undoable edits (delete playlist, remove entry, remove favorite) run in the activity-scoped `MainViewModel`. They are surfaced as `UserMessage` snackbars with Undo, so the undo outlives the screen that started it; for example, deleting a playlist pops its screen.
- Reordering and removal both save the full ordered id list with `PlaylistRepository.setOrder`. Undoing a removal restores the previous list.

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

- `feature/playlists/PlaylistScreens.kt` — `PlaylistEdits` contract, `PlaylistsScreen`, `PlaylistScreen` (drag-to-reorder, remove, rename, delete), `FavoritesScreen`, `NameDialog`, `AddToPlaylistSheet`.
- `feature/playlists/PlaylistViewModels.kt`.
- `ui/MainViewModel.kt` — `addToPlaylist`, `createPlaylist`, `deletePlaylist`, `removeFromPlaylist`, `removeFavorite`, `setFavorite`.
- `core/data/PlaylistRepository.kt`, `core/data/db/PlaylistDao.kt`, `core/data/db/Entities.kt` (`PlaylistEntity`, `PlaylistEntryEntity`, `FavoriteEntity`).
- Tests: none yet.

## Acceptance or verification criteria

- [ ] Create a playlist, add songs from any list, reorder them, and play the playlist.
- [ ] Tap the heart to favorite a song; it appears in Favorites.
- [ ] DAO tests cover ordering, duplicates, and unavailable entries.

## Remaining gaps and unknowns

- DAO tests need Room on a device or Robolectric; neither is set up, so the third criterion is open.
- The first two criteria need a device.
- M3U import and export are a later slice.
