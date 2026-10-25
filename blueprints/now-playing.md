# Now Playing

## Outcome or responsibility

A persistent mini player above the navigation bar expands with a fluid, gesture-driven transition into a full now-playing screen with artwork, title, seek bar, and controls.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Playback boundary](../architecture.md#playback-boundary) — the UI reads `PlayerState` and calls `PlayerController` only.
- [Visual system](../architecture.md#visual-system) — artwork-derived colors, bottom-third controls, swipe-down collapses, and reduced-motion handling.

## Local rules and implications

- The mini player is hidden when nothing is loaded.
- The expansion follows the finger (drag progress drives the shared artwork size and position), and settles with a spring.
- Swiping horizontally on the artwork skips to the next or previous track.
- The seek bar shows elapsed and remaining time, updated at least 4 times per second while visible.

## Related blueprints

### Required

- [playback-engine](playback-engine.md) — source of state and commands.
- [visual-theme](visual-theme.md) — artwork color scheme and motion tokens.

### Impact checks

- [queue](queue.md) — when the queue entry point on this screen changes.

## Relevant implementation and tests

- `feature/player/` — planned

## Acceptance or verification criteria

- [ ] The mini player appears after a song starts and shows title, artist, artwork, and play/pause.
- [ ] Tapping or dragging up expands it; dragging down collapses it and never leaves the app.
- [ ] The screen's colors change with the current track's artwork.

## Remaining gaps and unknowns

- Lyrics display is a later slice.
