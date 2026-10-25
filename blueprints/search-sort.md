# Search and Sort

## Outcome or responsibility

Users find any song, album, or artist instantly with type-ahead search, and sort lists by title, artist, album, date added, or duration.

## Current verified status

**Status:** Planned.

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

- `feature/search/`, `core/data/` — planned

## Acceptance or verification criteria

- [ ] Typing part of a title or artist shows matching results while typing.
- [ ] The sort choice per tab survives an app restart.

## Remaining gaps and unknowns

None
