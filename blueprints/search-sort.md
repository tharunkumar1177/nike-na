# Search and Sort

## Outcome or responsibility

Users find any song, album, or artist instantly with type-ahead search, and sort lists by title, artist, album, date added, or duration.

## Current verified status

**Status:** Partial. The FTS4 search (title, artist, album, file name; `unicode61` strips diacritics), grouped results, the 150 ms debounce, and per-tab sort in DataStore are implemented and compile in CI. Text normalization, FTS match building, and sorting are unit-tested; type-ahead behavior is unverified on a device.

## Architecture dependencies

- [Library synchronization](../architecture.md#library-synchronization) — search reads the Room cache only.
- [Threading](../architecture.md#threading) — queries run on `Dispatchers.IO`, debounced at about 150 ms.

## Local rules and implications

- Search is case- and accent-insensitive and matches title, artist, album, and file name, using a Room FTS4 table.
- Results are grouped as Songs, Albums, and Artists.
- The sort choice is stored per tab in DataStore.

## Related blueprints

### Required

- [library-browsing](library-browsing.md) — result rows and actions reuse browsing components.

### Impact checks

None

## Relevant implementation and tests

- `feature/search/SearchScreen.kt`, `feature/search/SearchViewModel.kt` — focused field and grouped Songs (first 50), Albums (horizontal row), and Artists.
- `core/data/LibraryQueries.kt` — `LibrarySorting`, `SearchText` (normalize, tokens, `ftsMatch`, `matches`).
- `core/data/LibraryRepository.kt` (`search`, `sort`, `setSort`), `core/data/db/LibraryDao.kt`, `core/data/db/Entities.kt` (`TrackFtsEntity`).
- `core/data/prefs/UserPreferences.kt` — sort per `LibraryTab`.
- `feature/library/LibraryScreen.kt` — sort menu.
- Tests: `app/src/test/java/com/music/player/core/data/LibraryQueriesTest.kt`.

## Acceptance or verification criteria

- [ ] Typing part of a title or artist shows matching results while typing.
- [ ] The sort choice per tab survives an app restart.

## Remaining gaps and unknowns

- Both criteria need a device.
