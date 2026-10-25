# Library Browsing

## Outcome or responsibility

Users browse their library by Songs, Albums, Artists, and Folders, drill into an album, artist, or folder, and start playback from any list.

## Current verified status

**Status:** Partial. The Library tabs, sort menu, album, artist, and folder detail screens, the track action sheet, and the access states are implemented and compile in CI. Rendering and scrolling behavior is unverified on a device.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — list items and drill-down routes carry ids, never file paths.
- [Visual system](../architecture.md#visual-system) — artwork fallbacks and motion rules apply to every list and header.

## Local rules and implications

- Lists are lazy, keyed by id, and read Room `Flow`s; artwork loads through Coil with `ContentResolver.loadThumbnail` at list-cell size.
- Tapping a song plays the visible list from that song (the list becomes the queue).
- The album detail header uses large artwork and a color derived from it.
- Long-press or the overflow menu opens track actions: Play next, Add to queue, Add to playlist, Favorite, Share, Delete.

### Design plan (approved 2026-10-25)

- **Library screen:** top bar "Library" (search and sort actions), `PrimaryTabRow` Songs / Albums / Artists / Folders; Songs, Artists, Folders are `LazyColumn`; Albums is a 2-column `LazyVerticalGrid` with 16dp gaps.
- **Song row:** 72dp `ListItem`; 48dp artwork (small shape); title body large, artist body medium, both single line; trailing duration label medium + overflow `IconButton` ("More options for <title>").
- **Album tile:** 1:1 artwork (medium shape), title body large, artist body medium. **Artist row:** circular 48dp avatar, track count supporting text. **Folder row:** folder icon tile, track count.
- **List header:** two equal `FilledTonalButton`s "Play all" and "Shuffle", 16dp side padding, 8dp gap.
- **Detail screens (album, artist, folder):** collapsing large header with big artwork and artwork-tinted gradient, title large, subtitle ("Album • 2024 • 12 songs"), then header buttons and song rows. Folders are a flat list of containing folders.
- **Track action sheet:** `ModalBottomSheet` (scrollable) headed by the song row, then actions with leading icons; long-press triggers haptic feedback.
- **Sort:** `DropdownMenu` from the sort action: Title, Artist, Album, Date added, Duration, plus ascending/descending toggle.
- **Access states:** denied → centered permission screen (icon, headline medium, rationale, filled button "Grant access" or "Open settings" when permanently denied); partial (Android 14+) → non-dismissable banner below tabs "Showing part of your library." with "Allow all"; scanning with empty cache → progress + "Scanning your music…"; empty → "No music found".
- **Transitions:** fade-through 250ms between tabs and into detail screens.

## Related blueprints

### Required

- [library-sync](library-sync.md) — provides the cached entities and access state.

### Impact checks

- [search-sort](search-sort.md) — when list item contracts or ordering change.

## Relevant implementation and tests

- `feature/library/LibraryScreen.kt` — top bar, sort `DropdownMenu`, `PrimaryTabRow` with a `HorizontalPager`, and the scanning and empty states.
- `feature/library/DetailScreens.kt` — album, artist, and folder screens with an artwork-tinted gradient header.
- `feature/library/LibraryViewModels.kt`, `feature/library/PermissionScreen.kt`.
- `core/ui/components/LibraryItems.kt` — `SongRow`, `AlbumTile`, `ArtistRow`, `FolderRow`, `PlayShuffleHeader`, `EmptyState`, `LoadingState`.
- `core/ui/components/TrackActions.kt` — the `TrackActions` contract and the action sheet. `ui/MusicApp.kt` implements the contract once for every screen.
- `core/data/artwork/ArtworkFetcher.kt` — Coil fetcher using `ContentResolver.loadThumbnail`.
- Tests: `app/src/test/java/com/music/player/core/ui/components/FormattingTest.kt`, `app/src/test/java/com/music/player/core/data/LibraryQueriesTest.kt`.

## Acceptance or verification criteria

- [ ] The Songs tab lists every indexed track with title, artist, duration, and artwork.
- [ ] Albums, Artists, and Folders tabs drill down to their tracks.
- [ ] Scrolling a large library stays smooth (no dropped frames visible in normal use).

## Remaining gaps and unknowns

- All three criteria need a device.
- Differences from the design plan in the current code:
  - The detail header is a static gradient header, not a collapsing one.
  - Loading uses a progress indicator, not shimmer rows.
  - The planned fade-through transitions between tabs and detail screens are not implemented; navigation uses the default transitions.
  - No partial-access banner exists, because audio has no partial access (see [library-sync](library-sync.md)).
- Genres are deferred to a later slice.
