# Problems

Known defects, their root cause, and the fix applied.

## 1. App closes when tapping the mini player

**Status:** Fixed in [PR #3](https://github.com/tharunkumar1177/nike-na/pull/3). CI green; not yet confirmed on a device.

### Problem

Tapping the mini player (the "now playing" bar above the bottom navigation) closes the app instead of opening the full Now Playing screen.

### Root cause

- Tapping the mini player animates the player's expansion progress from 0 (mini) to 1 (full screen) using a spring with damping ratio 0.8 (`Motion.playerSpring()` in `core/ui/theme/Tokens.kt`).
- A damping ratio below 1 makes the spring bounce, so the progress briefly goes slightly above 1 before settling.
- `PlayerSheet` in `feature/player/PlayerSheet.kt` uses that raw progress to compute the sheet's corner radius as `lerp(16.dp, 0.dp, p)`. When `p` is above 1, the radius becomes negative.
- `RoundedCornerShape` throws an `IllegalArgumentException` on a negative corner size, and the uncaught exception closes the app.
- Shadow and tonal elevation are interpolated the same way and would also go negative.

### Fix

Cap the progress to the 0 to 1 range before it is used to compute the sheet's size, shape, elevation, and fades:

```kotlin
val p = expansion.value.coerceIn(0f, 1f)
```

The spring animation itself is unchanged. Only the values derived from it are capped, which also covers the opposite case when a downward fling bounces below 0 while collapsing.

### Verification

- CI (lint, unit tests, debug build) passes.
- Still needed: on a device, tap the mini player and confirm the full player opens, then collapse it by tap, swipe, and back gesture.
