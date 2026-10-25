# Now Playing

## Outcome or responsibility

A persistent mini player above the navigation bar expands with a fluid, gesture-driven transition into a full now-playing screen with artwork, title, seek bar, and controls.

## Current verified status

**Status:** Partial. The mini player and Now Playing are one gesture-driven surface (`PlayerSheet`). It has the drag-progress morph, spring settle, reduced-motion snap, seek bar with ±10 s accessibility actions, transport, favorite, overflow actions, and swipe-to-skip. It compiles in CI; gesture feel and re-tinting are unverified on a device.

## Architecture dependencies

- [Playback boundary](../architecture.md#playback-boundary) — the UI reads `PlayerState` and calls `PlayerController` only.
- [Visual system](../architecture.md#visual-system) — artwork-derived colors, bottom-third controls, swipe-down collapses, and reduced-motion handling.

## Local rules and implications

- The mini player is hidden when nothing is loaded.
- The expansion follows the finger (drag progress drives the shared artwork size and position), and settles with a spring.
- Swiping horizontally on the artwork skips to the next or previous track.
- The seek bar shows elapsed and remaining time, updated at least 4 times per second while visible.

### Design plan (approved 2026-10-25)

- **Mini player:** 64dp tall floating card, 48dp art, title title medium + artist body small, play/pause 48dp, 2dp progress line along the bottom edge; horizontal swipe skips, tap or drag up expands.
- **Transition:** drag progress 0→1 drives artwork size and position (48dp → up to 320dp); mini controls fade out over 0–30%, full controls fade in over 70–100%; background morphs from surface to an artwork-tinted blurred gradient (`RenderEffect` blur on the artwork); release settles with spring damping 0.8, stiffness 380. Reduced motion → instant switch.
- **Now playing, top to bottom:** top bar (collapse chevron, "Now playing", overflow: Add to playlist, Share); artwork 1:1, max 320dp, large shape, shadow; then in the bottom third: title headline small (marquee) + artist title medium on surface variant + favorite heart; slider with elapsed/remaining label small; transport row shuffle 48, previous 64, play/pause 80dp primary container, next 64, repeat 48; bottom row "Up next: <title>" opening the queue.
- **Accessibility:** transport content descriptions ("Play", "Pause", "Next track", "Previous track"); slider supports ±10 s accessibility actions; marquee disabled under reduced motion.

## Related blueprints

### Required

- [playback-engine](playback-engine.md) — source of state and commands.
- [visual-theme](visual-theme.md) — artwork color scheme and motion tokens.

### Impact checks

- [queue](queue.md) — when the queue entry point on this screen changes.

## Relevant implementation and tests

- `feature/player/PlayerSheet.kt` — the `PlayerUiActions` contract, the morphing surface, mini and full content, seek bar, and transport.
- `ui/MusicApp.kt` — owns the expansion `Animatable`, places the sheet above the navigation bar, and opens it on `MainActivity.ACTION_OPEN_PLAYER` (notification tap).
- `core/ui/theme/ArtworkColors.kt` — seed extraction used to re-tint the theme per track.
- Tests: none specific; the surface is UI-only.

## Acceptance or verification criteria

- [ ] The mini player appears after a song starts and shows title, artist, artwork, and play/pause.
- [ ] Tapping or dragging up expands it; dragging down collapses it and never leaves the app.
- [ ] The screen's colors change with the current track's artwork.

## Remaining gaps and unknowns

- All criteria need a device; there are no UI tests in CI.
- Differences from the design plan in the current code:
  - The mini player also has a Next button.
  - The overflow menu also has Go to album and Go to artist.
  - The play/pause button uses the `primary` container color.
  - The background blur uses `Modifier.blur`, which falls back to no blur below API 31; minSdk 33 makes that moot.
- Lyrics display is a later slice.
