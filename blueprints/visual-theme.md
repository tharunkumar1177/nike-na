# Visual Theme

## Outcome or responsibility

A cohesive, calm, premium look: a Material 3 theme that adapts to the current artwork, expressive typography, consistent motion, and attractive fallbacks for missing artwork.

## Current verified status

**Status:** Planned.

## Architecture dependencies

- [Visual system](../architecture.md#visual-system) — this blueprint implements the color seed chain, motion rules, and artwork fallback.

## Local rules and implications

- The artwork seed is extracted off the main thread with `androidx.palette` and cached by album id.
- Color scheme changes animate over about 400 ms; they are instant when animations are disabled.
- Light, dark, and follow-system modes are supported; dark is tuned for OLED.
- Design tokens (spacing, shape, type scale, motion durations) live in `core.ui` and are the only source used by features.

## Related blueprints

### Required

None

### Impact checks

- [now-playing](now-playing.md) and [library-browsing](library-browsing.md) — when tokens change.

## Relevant implementation and tests

- `core/ui/` — planned

## Acceptance or verification criteria

- [ ] Switching tracks visibly re-tints the now-playing screen.
- [ ] Albums without artwork show a generated gradient tile with initials.

## Remaining gaps and unknowns

- The detailed visual direction comes from the designer plan for each UI slice.
