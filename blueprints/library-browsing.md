# Library Browsing

## Outcome or responsibility

Users browse their library by Songs, Albums, Artists, and Folders, drill into an album, artist, or folder, and start playback from any list.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Track identity](../architecture.md#track-identity) — list items and drill-down routes carry ids, never file paths.
- [Visual system](../architecture.md#visual-system) — artwork fallbacks and motion rules apply to every list and header.

## Local rules and implications

- Lists are lazy, keyed by id, and read Room `Flow`s; artwork loads through Coil with `ContentResolver.loadThumbnail` at list-cell size.
- Tapping a song plays the visible list from that song (the list becomes the queue).
- The album detail header uses large artwork and a color derived from it.
- Long-press or the overflow menu opens track actions: Play next, Add to queue, Add to playlist, Favorite, Share, Delete.

## Related blueprints

### Required

- [library-sync](library-sync.md) — provides the cached entities and access state.

### Impact checks

- [search-sort](search-sort.md) — when list item contracts or ordering change.

## Relevant implementation and tests

- `feature/library/` — planned

## Acceptance or verification criteria

- [ ] The Songs tab lists every indexed track with title, artist, duration, and artwork.
- [ ] Albums, Artists, and Folders tabs drill down to their tracks.
- [ ] Scrolling a large library stays smooth (no dropped frames visible in normal use).

## Remaining gaps and unknowns

- Genres are deferred to a later slice.
