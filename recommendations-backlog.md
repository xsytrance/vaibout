# vAIb — Recommendations Backlog

## Design System
- [ ] Add `MotionTokens.kt` — animation durations, easings, haptic patterns
- [ ] Add custom icon set (Phosphor/Fluent line style, 1.5dp stroke)
- [ ] Bundle custom fonts in `res/font/`
- [ ] Implement per-station color themes

## Audio Engine
- [ ] Add `QueueRepository` — ordered track queue with reorder/persist
- [ ] Add `AudioFocusManager` — duck on interruption, resume on regain
- [ ] Add `PlayerService.kt` — foreground service with Media3 Session
- [ ] Add crossfade (configurable: 0/3/5/8s)
- [ ] Add sleep timer (5/10/15/30/45/60min)
- [ ] Add speed selector (0.75x, 1.0x, 1.25x, 1.5x)

## Stations
- [ ] `StationEntity` + `StationTrackCrossRef` entities
- [ ] `StationDao` — CRUD operations
- [ ] `StationsScreen` — grid layout with add/edit/delete
- [ ] Smart stations: "All Tracks", "Favorites", "Recently Played"
- [ ] Station-level EQ + visualizer theme persistence

## Visualizers
- [ ] Abstract `VisualizerRenderer` base class
- [ ] WaveformRenderer (time-domain visualization)
- [ ] ParticleRenderer (GPU particle system with beat-spawning)
- [ ] Touch interaction (tap to pulse, drag to bend waves)
- [ ] Style selector UI in dreamscape screen

## UX
- [ ] Onboarding flow (3 screens: brand, features, permissions)
- [ ] Mini-player (persistent bottom bar, swipe-up to expand)
- [ ] Context menus on long-press (add to station, favorite, share, delete)
- [ ] Swipe-to-delete with undo snackbar
- [ ] Pull-down refresh in Discover
- [ ] Settings screen (EQ defaults, crossfade, theme, sleep timer)

## Data
- [ ] Room migrations (v4 → v5+) for new entities
- [ ] vAIb export/import (JSON via share intent)
- [ ] Backup integration (Android Auto Backup)

## Polish
- [ ] Album art extraction for theme colors (Coil integration)
- [ ] Dynamic color from album art
- [ ] Performance profiling (60fps target on mid-range)
- [ ] Memory leak audit (30-min stability test)
- [ ] Battery optimization (<5%/hr background)
- [ ] Accessibility (TalkBack, high-contrast)
- [ ] Splash screen with animated logo
- [ ] Edge-to-edge + cutout handling