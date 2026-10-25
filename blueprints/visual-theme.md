# Visual Theme

## Outcome or responsibility

A cohesive, calm, premium look: a Material 3 theme that adapts to the current artwork, expressive typography, consistent motion, and attractive fallbacks for missing artwork.

## Current verified status

**Status:** Partial. The tokens, Outfit typography, brand and OLED schemes, and the artwork seed chain are implemented and compile in CI: Palette seed, then MaterialKolor scheme, then system dynamic color, then brand. So are the 400 ms scheme crossfade with reduced-motion snap, the gradient placeholder, and the app shell. Placeholder rules are unit-tested; visual results are unverified on a device.

## Architecture dependencies

- [Visual system](../architecture.md#visual-system) — this blueprint implements the color seed chain, motion rules, and artwork fallback.

## Local rules and implications

- The artwork seed is extracted off the main thread with `androidx.palette` and cached by album id.
- Color scheme changes animate over about 400 ms; they are instant when animations are disabled.
- Light, dark, and follow-system modes are supported; dark is tuned for OLED.
- Design tokens (spacing, shape, type scale, motion durations) live in `core.ui` and are the only source used by features.

### Design plan (approved 2026-10-25)

- **Color seed chain:** Palette vibrant swatch (then dominant) from current artwork → tonal scheme; fallback `dynamicLight/DarkColorScheme`; static brand scheme primary `#6750A4`, secondary `#625B71`, tertiary `#7D5260`, light background `#FFFBFE`. Dark mode uses true black `#000000` background and `#1C1B1F` surface container (OLED).
- **Typography:** bundled Outfit (OFL) in `res/font/`, applied to display, headline, title, and label roles; body roles use the system font. Display large 57/64sp, headline large 32/40, title large 22/28 w500, body large 16/24, label medium 12/16 w500.
- **Shape:** small 8dp (list artwork, mini player art), medium 16dp (cards, dialogs, album tiles), large 24dp (now-playing artwork), full (artist avatars).
- **Spacing:** 4, 8, 12, 16 (screen margin), 24, 32dp.
- **Elevation:** scrolled top bar and nav bar 3dp tonal; mini player 6dp.
- **Motion:** standard 300ms, quick 150ms, `FastOutSlowInEasing`; player expand spring damping 0.8, stiffness 380; color scheme crossfade 400ms; animator scale 0 → snap.
- **Artwork placeholder:** linear gradient top-left → bottom-right; hue1 = hash(title) mod 360, hue2 = hue1 + 40, saturation 70%, lightness 40%; up to two white initials in title large.
- **States:** shimmer (1000ms loop) for loading rows; empty state = 72dp icon, title large message, body large hint.
- **App shell:** bottom `NavigationBar` with Library, Playlists, Search, Settings; mini player floats 8dp above it with 16dp horizontal margins; full edge-to-edge with list `contentPadding` for insets.

## Related blueprints

### Required

None

### Impact checks

- [now-playing](now-playing.md) and [library-browsing](library-browsing.md) — when tokens change.

## Relevant implementation and tests

- `core/ui/theme/` — `Color.kt`, `Type.kt`, `Tokens.kt`, `Theme.kt`, `ArtworkColors.kt` (LRU-cached seed per album id).
- `core/ui/components/ArtworkPlaceholder.kt`, `core/ui/components/Artwork.kt`.
- `ui/MusicApp.kt` — applies `MusicTheme` with the seed of the current album and the user's appearance settings, and hosts the bottom `NavigationBar` shell.
- `feature/settings/SettingsScreen.kt` — theme mode (System, Light, Dark) and the "Colors from artwork" switch.
- Tests: `app/src/test/java/com/music/player/core/ui/components/PlaceholderArtTest.kt`.

## Acceptance or verification criteria

- [ ] Switching tracks visibly re-tints the now-playing screen.
- [ ] Albums without artwork show a generated gradient tile with initials.

## Remaining gaps and unknowns

- Both criteria need a device. The placeholder's hue and initials rules are unit-tested, but not its rendering.
- Shimmer loading rows from the design plan are not implemented; a progress indicator is used.
