<!--
  Wireframe style guide for Music. Derived from the shipped Compose theme
  (core/ui/theme/Color.kt, Tokens.kt, Type.kt) so wireframes resemble the built app.
-->
# Wireframe style

## Canvas and composition
- 1080x1920 portrait (9:16), one screen per image, plain white canvas.
- One Android phone frame (thin dark-grey bezel, rounded corners, punch-hole camera) centered on the canvas, filling about 85% of the height.
- Status bar at the top of the screen (time on the left, signal/wifi/battery on the right); gesture navigation pill at the bottom.
- No annotations, callouts, or labels outside the phone frame.

## Fidelity
- High-fidelity Material 3 (Material You) Android UI, light theme, flat, no 3D or perspective.
- Real, readable English text exactly as listed for each screen. Sample content uses realistic song, album, and artist names.
- Rounded Material Symbols icons.

## Colors (light brand scheme)
| Role | Hex |
|---|---|
| Background / surface | `#FFFBFE` |
| Primary (accents, selected tab, play button, section titles) | `#6750A4` |
| Primary container (tonal buttons, header gradient, icon circles) | `#EADDFF` |
| Secondary container (folder tiles, selected nav pill) | `#E8DEF8` |
| Surface container high (mini player, sheets) | `#ECE6F0` |
| On surface (primary text) | `#1D1B20` |
| On surface variant (secondary text, idle icons) | `#49454F` |
| Error (destructive actions) | `#B3261E` |

## Typography
- Headlines, titles, labels, and buttons: Outfit (geometric sans), medium to semibold.
- Body text: Roboto.
- Screen titles in top app bars are large (headline medium, 28sp).

## Shapes and spacing
- 4dp grid; screen side margins 16dp.
- Song rows 72dp high with a 48dp artwork tile (8dp corners), title, artist below, duration and a vertical three-dot button on the right.
- Album tiles: square cover with 16dp corners, title and artist below.
- Large detail artwork and Now Playing artwork: 24dp corners. Artist artwork is circular.
- Buttons are fully rounded pills. Bottom sheets have 28dp top corners and a drag handle.

## Artwork
- Album art is either a colorful realistic cover or the app's generated placeholder: a diagonal two-hue gradient (saturated, mid-dark) with up to two white uppercase initials centered. Never a grey music-note icon.

## App chrome
- Bottom navigation bar (surface container tone) with four items: Library, Playlists, Search, Settings. The selected item has a `#E8DEF8` pill behind its icon.
- When a song is loaded, a floating mini player (64dp, 16dp corners, `#ECE6F0`, slight shadow) sits 8dp above the navigation bar with 16dp side margins: artwork, title, artist, play/pause, next, and a thin 2dp `#6750A4` progress line along its bottom.
