# Blueprint Manifest

## Loading protocol

1. Match the task by intent and synonyms in the router below.
2. Load one primary blueprint and only its linked architecture sections.
3. Inspect the listed implementation and tests.
4. Load an impact-check blueprint only if its stated trigger applies.
5. For a shared-contract or ambiguous change, load the complete [architecture](../architecture.md) and every entry linking the affected section.
6. Do not load unrelated blueprints.

## Router

| Concepts and synonyms | Responsibility type | Primary blueprint | Required architecture contracts | Required blueprints | Impact checks | Principal implementation and tests |
|---|---|---|---|---|---|---|
| build, gradle, CI, workflow, lint, version catalog, APK | Workflow | [build-and-ci](build-and-ci.md) | [Platform and toolchain](../architecture.md#platform-and-toolchain), [Verification boundaries](../architecture.md#verification-boundaries) | None | Any blueprint when a dependency version changes | `.github/workflows/`, `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts` |
| scan, MediaStore, permission, sync, cache, Room, index | Integration | [library-sync](library-sync.md) | [Track identity](../architecture.md#track-identity), [Library synchronization](../architecture.md#library-synchronization), [Threading](../architecture.md#threading), [Error and recovery](../architecture.md#error-and-recovery) | None | [library-browsing](library-browsing.md) when entity shape changes; [queue](queue.md) when deletion handling changes | `core/data/` |
| songs, albums, artists, folders, tabs, browse, list | Capability | [library-browsing](library-browsing.md) | [Track identity](../architecture.md#track-identity), [Visual system](../architecture.md#visual-system) | [library-sync](library-sync.md) | [search-sort](search-sort.md) when list contracts change | `feature/library/` |
| playback, ExoPlayer, media session, notification, lock screen, Bluetooth, audio focus, service | Component | [playback-engine](playback-engine.md) | [Playback boundary](../architecture.md#playback-boundary), [Track identity](../architecture.md#track-identity), [Threading](../architecture.md#threading), [Error and recovery](../architecture.md#error-and-recovery) | None | [queue](queue.md) and [now-playing](now-playing.md) when `PlayerState` changes | `core/playback/` |
| now playing, mini player, seek bar, controls, expand, collapse | Capability | [now-playing](now-playing.md) | [Playback boundary](../architecture.md#playback-boundary), [Visual system](../architecture.md#visual-system) | [playback-engine](playback-engine.md), [visual-theme](visual-theme.md) | [queue](queue.md) when the queue entry point changes | `feature/player/` |
| queue, up next, play next, add to queue, reorder, shuffle, repeat | Capability | [queue](queue.md) | [Playback boundary](../architecture.md#playback-boundary), [Error and recovery](../architecture.md#error-and-recovery) | [playback-engine](playback-engine.md) | [playlists-favorites](playlists-favorites.md) when "save queue" changes | `feature/player/queue/`, `core/playback/` |
| playlist, favorites, like, heart, collection | Capability | [playlists-favorites](playlists-favorites.md) | [Track identity](../architecture.md#track-identity), [Library synchronization](../architecture.md#library-synchronization), [Error and recovery](../architecture.md#error-and-recovery) | [library-sync](library-sync.md) | [queue](queue.md) when playlist playback changes | `feature/playlists/`, `core/data/` |
| search, filter, sort, order | Capability | [search-sort](search-sort.md) | [Library synchronization](../architecture.md#library-synchronization), [Threading](../architecture.md#threading) | [library-browsing](library-browsing.md) | None | `feature/search/`, `core/data/` |
| delete, trash, share, file action | Capability | [file-operations](file-operations.md) | [Error and recovery](../architecture.md#error-and-recovery), [Track identity](../architecture.md#track-identity) | [library-sync](library-sync.md) | [queue](queue.md) and [playlists-favorites](playlists-favorites.md) when deletion behavior changes | `feature/library/actions/` |
| theme, color, artwork palette, typography, motion, design tokens | Component | [visual-theme](visual-theme.md) | [Visual system](../architecture.md#visual-system) | None | [now-playing](now-playing.md) and [library-browsing](library-browsing.md) when tokens change | `core/ui/` |

All implementation paths are relative to `app/src/main/java/com/music/player/` unless they start with `.github/`, `gradle/`, or a root file name. Tests mirror them under `app/src/test/`.

## Status vocabulary

- `Implemented`: verified in current code and tests.
- `Partial`: some acceptance criteria are verified; gaps are named.
- `Planned`: required behavior is not implemented.
- `Unknown`: evidence is insufficient; inspect before changing.
- `Deprecated`: retained only for migration or compatibility.
- `Superseded`: replaced by the linked blueprint.
