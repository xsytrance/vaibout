# vAIb — AAA Visual & Experience Overhaul Plan

> **Branch:** `overhaul/aaa-visual-redesign`
> **Base:** `main` (30 commits, v0.1.0)
> **Author:** Agent supersort, commissioned by xsytrance (Agenor)

---

## 0. Executive Summary

vAIb is a reactive music visualizer app with a dark OLED aesthetic, cyan/violet palette, and an "AI biorhythm" concept. The foundation is solid: ExoPlayer + Room + Compose + OpenGL ES dreamscape. This plan transforms it from an early prototype into a **polished, AAA-grade Android application** with organized music stations, stunning visuals, and addictive UX.

---

## 1. Design System Overhaul

### 1.1 Color Palette Expansion
Current palette has 5 colors. Expand to a full **adaptive chromatic system**:

| Token | Current | Overhaul |
|---|---|---|
| Primary | `#00E5FF` (Cyan) | Dynamic — shifts per station/mood |
| Secondary | `#8B5CF6` (Violet) | Dynamic complement |
| Background | `#05070A` | True OLED black `#000000` + per-station tint |
| Surface | `#05070A` | Elevated surfaces `#0A0F14` → `#121820` |
| Text | `#9AA4B2` | `#E0E6EF` primary, `#8892A4` secondary |

**New system:** Define 6 **Station Themes** — each with its own primary/secondary/ambient gradient. Themes flow into every screen, visualizer, and card automatically.

### 1.2 Typography
Replace bare `Typography()` with a custom **VaibTypography** spec:
- **Display:** `Montserrat` / `Outfit` — 32–48sp, tracking `-2..4`, weight 800
- **Headline:** 22–28sp, weight 700
- **Body:** 14–16sp, weight 400/500, letter-spacing `0.02em`
- **Mono/Caption:** `JetBrains Mono` or `Space Grotesk` — 11–12sp for metadata, timestamps, EQ labels
- Font files bundled in `res/font/`

### 1.3 Animation Language
Define a shared **motion spec**:
| Token | Duration | Easing |
|---|---|---|
| Quick reveal | 150ms | `FastOutLinearIn` |
| Standard transition | 250ms | `FastOutSlowIn` |
| Express pulse | 380ms | `Overshoot(1.2)` |
| Ambient drift | 8–30s | `LinearEasing` (infinite) |
| Card entrance | 400ms stagger | `spring(damping=0.8)` |
| Haptic tap | — | `CLICK` / `TICK` haptics on all interactive elements |

### 1.4 Iconography
Replace Material default icons with a **custom stroke icon set** (outlined, 1.5dp stroke, rounded caps):
- Play/Pause, Skip, Shuffle, Repeat, Heart, Save, Delete, Search, Back
- Source: Fork/adapt from Phosphor Icons or Fluent UI 24px line set
- All icons tinted per-theme, not hardcoded

---

## 2. Station & Track Organization System

### 2.1 Music Stations Architecture
Create a **Station** entity and relationship model:

```kotlin
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                       // "Late Night Synthwave"
    val description: String,                // "Cyberpunk soundscapes 2AM–5AM"
    val icon: String,                       // emoji or SF Symbol name
    val themeId: String,                    // references Theme enum
    val eqPreset: String,                   // EqPreset name
    val visualizerStyle: String,            // VisualizerStyle name
    val sourceType: String,                 // "LOCAL", "INTERNET_ARCHIVE", "MIXED"
    val sortOrder: Int,                     // manual ordering
    val createdAt: Long,
)

// Junction table: station <-> track membership
@Entity(
    primaryKeys = ["stationId", "trackId"],
    tableName = "station_tracks"
)
data class StationTrackCrossRef(
    val stationId: Long,
    val trackId: Long,
    val addedAt: Long,
)
```

**Station types:**
| Type | Behavior |
|---|---|
| **Preset Station** | Auto-generated: "All Tracks", "Internet Archive", "Favorites" |
| **Mood Station** | User-created: "Deep Focus", "Energy Boost", "Chill Evening" |
| **Smart Station** | Rule-based: "Recently Played", "Long Sessions", "High Energy" (future) |

### 2.2 Track Entity Enhancement
Extend `TrackEntity`:
- `artist: String?`
- `album: String?`
- `genre: String?`
- `durationMs: Long`
- `playCount: Int`
- `lastPlayedAt: Long?`
- `favorite: Boolean`

Add a **deduplication** strategy for local SAF URIs and Internet Archive streams.

### 2.3 Station Screen (NEW)
Design a full **StationsListScreen** with:
- Grid of station cards (2-column staggered layout)
- Each card: gradient thumbnail, station name, track count, play button
- FAB → "New Station" (name, icon picker, theme picker, add tracks)
- Pull-to-refresh for Internet Archive stations
- Long-press → edit/reorder/delete
- Drag-to-reorder stations in the grid

### 2.4 Now Playing Fullscreen
Redesign `NowPlayingCard` transition to a **full-screen player**:
- Album/artwork area (animated gradient if no artwork)
- Seek bar with waveform thumbnail scrubber
- Full transport row: shuffle, prev, play/pause, next, repeat
- Speed selector (0.75x, 1.0x, 1.25x, 1.5x)
- Expandable EQ panel with per-band sliders using the 5-band hardware EQ
- Add to station / favorite heart button
- Swipe-up gesture to reveal the full player from mini-player

---

## 3. Visualizer Overhaul

### 3.1 Multi-Style Visualizer Engine
Current state: OpenGL ES shader with concentric rings + beat ripple. Keep as foundation, add **3 distinct visualizer styles**:

| Style | Description |
|---|---|
| **Nebula** (default) | Current ring-based shader with improved bloom, particle trails, and color shifting |
| **Waveform** | Time-domain waveform renderer with 3D perspective projection, reactive amplitude |
| **Particles** | GPU particle system — dots that flow, explode on beats, and emit light trails |

**Technical approach:**
- Refactor `VisualizerRenderer` to accept a `VisualizerStyle` enum
- Each style gets its own GLSL fragment shader
- Shared uniform interface: `uTime`, `uEnergy`, `uBeat`, `uBeatAge`, `uResolution`, `uColorPrimary`, `uColorSecondary`
- Add **touch interaction** uniforms: `uTouchX`, `uTouchY`, `uTouchActive`

### 3.2 Enhanced Dreamscape Screen
SoloDreamscapeScreen improvements:
- **Visualizer style selector** — horizontal chip row at top (Nebula / Waveform / Particles)
- **Theme picker** — floating palette button
- **Lyrics overlay** — future placeholder (room for lyrics API)
- **Session stats** — subtle overlay showing play time, BPM estimate, peak energy
- **Double-tap** to toggle between full-visualizer and mini-player-with-visualizer modes
- **Two-finger pinch** to zoom visualizer intensity (sensitivity multiplier)
- **Shake gesture** to randomize theme

### 3.3 Home Screen Visualizer Preview
- Replace static `MountainBarViz` with a **live-rendered mini OpenGL preview** in the HomeCard
- Or use a Canvas-based waveform that responds to `audioEnergy` / `audioBeatPulse` flows from ViewModel

---

## 4. Home Screen Redesign

### 4.1 Layout Structure
```
┌──────────────────────────────┐
│  vAIb out!        [⚙️] [🔍]  │  ← branded top bar
├──────────────────────────────┤
│  ┌────────────────────────┐  │
│  │    NOW PLAYING CARD    │  │  ← full-width hero, tappable → full player
│  └────────────────────────┘  │
│                              │
│  STATIONS (horizontal scroll)│  ← pill chips: All / Favorites / presets + custom
│  ┌─────┬─────┬─────┬──────┐ │
│  │ 🏠  │ 💜  │ 🌊  │ +New │ │
│  └─────┴─────┴─────┴──────┘ │
│                              │
│  ┌─ Station Name ──────────┐│
│  │ ┌────┐ ┌────┐ ┌────┐   ││  ← Station track list (horizontal grid)
│  │ │ art│ │ art│ │ art│   ││     or vertical list with mini-waveform
│  │ └────┘ └────┘ └────┘   ││
│  └─────────────────────────┘│
│                              │
│  My vAIbs (saved moments)   │  ← compact card row (existing)
│  ┌────┐ ┌────┐ ┌────┐       │
│  │    │ │    │ │    │       │
│  └────┘ └────┘ └────┘       │
└──────────────────────────────┘
```

### 4.2 Key Interactions
- **Tap track** → play immediately (replace current track)
- **Long-press track** → context menu (Add to Station, Favorite, Share, Delete)
- **Swipe left on track** → delete / archive (with undo snackbar)
- **Pull down** → refresh Discover content
- **Tap Now Playing card** → expand to full player with visualizer

---

## 5. Discover Screen Redesign

### 5.1 Content Organization
Replace flat list with **categorized sections**:
- **Trending** — most downloaded CC audio this week
- **By Mood** — curated playlists: "Deep", "Chill", "Energetic", "Cosmic", "Focus"
- **Search results** — same as current, but with card-based layout
- **Recently discovered** — section for items you've previously viewed

### 5.2 Discover Card Design
```
┌──────────────────────────────┐
│  ┌────────────────────────┐  │
│  │    Album Art / Waveform │  │  ← generated art from audio
│  │    gradient placeholder  │  │
│  └────────────────────────┘  │
│  Track Title                 │
│  Artist / Creator            │
│  ┌──────────────────────────┐│
│  │ ▶ Play  ·  + Save ·  ℹ  ││
│  └──────────────────────────┘│
└──────────────────────────────┘
```

### 5.3 Audio Preview
- **30-second preview** on long-press (use IA's preview stream URLs if available)
- Or tap → add to "Up Next" queue; queue visible as a bottom sheet

---

## 6. Audio Engine Improvements

### 6.1 Playlist / Queue System
New entity:
```kotlin
@Entity(tableName = "queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackUri: String,
    val trackName: String,
    val artist: String?,
    val position: Int,
    val addedAt: Long,
)
```

**Features:**
- Add tracks to an ordered queue
- Reorder by drag-and-drop
- "Play Next" / "Add to Up Next" from any source
- Queue persists across app restarts
- Auto-generate queue from station playback order (shuffle-able)

### 6.2 Enhanced EQ
- Replace chip-based EQ preset selector with **interactive 5-band equalizer panel**
- Use `Equalizer` API's band level range for proper per-device calibration
- Visual frequency bars that show real-time EQ effect
- Ability to name custom EQ presets
- Save EQ preset per-station

### 6.3 Fade & Crossfade
- **Crossfade** between queued tracks (configurable: 0s, 3s, 5s, 8s)
- **Volume fade-out** on track end (0.5s, 1s, 2s)
- **Fade-in** on track start (0.5s, 1s)

### 6.4 Sleep Timer
- Configurable timer (5min, 10min, 15min, 30min, 45min, 1h)
- Fade out gradually in the last 30 seconds
- Cancelable notification

### 6.5 Sound Quality
- Consider adding `Oboe` or `AAudio` low-latency audio path for pro devices
- Optional: bit-perfect output passthrough for lossless formats

---

## 7. Notification & Background Playback

### 7.1 MediaStyle Notification
- **Media3 Session** integration for proper media notification
- Album art (or generated gradient) in notification
- Transport controls (prev, play/pause, next)
- Swipe to dismiss stops playback (configurable)
- Tap notification → opens app to Now Playing

### 7.2 Background Audio
- Foreground service for audio playback when app is in background
- Handle audio focus (duck on interruption, resume on focus regain)
- Bluetooth / headset media button support
- Android Auto / Wear OS stub (future phase)

---

## 8. Data Layer Improvements

### 8.1 Migration
- `RoomDatabase` version 4 → version 5+ with new entities
- Migrations: add `stations`, `queue`, enhanced `tracks` tables
- Back up user vAIbs before destructive migrations

### 8.2 Repository Pattern
Introduce a clean **Repository** layer:
```
Repository
├── LocalTrackRepository (Room + SAF)
├── RemoteTrackRepository (Internet Archive)
├── StationRepository
├── QueueRepository
└── VaibRepository
```

### 8.3 Backup / Export
- Export vAIbs as JSON file (share intent)
- Import vAIbs from JSON
- Optional: Android Auto Backup integration

---

## 9. Onboarding & First-Run Experience

### 9.1 Welcome Flow
3-screen onboarding:
1. **Brand screen** — "vAIb out!" logo with ambient animation, tagline
2. **Feature showcase** — swipeable cards showing: Visualizer, Stations, EQ, Discover
3. **Permission request** — RECORD_AUDIO explained in-context with illustration

### 9.2 First Track Prompt
- If no track is loaded on first launch → show a warm "Choose your first track" card
- Pre-populate Discover with "Editor's Picks" from Internet Archive

---

## 10. Technical Architecture

### 10.1 Package Structure (proposed)
```
com.xsytrance.vaib/
├── MainActivity.kt
├── MainViewModel.kt
├── core/
│   ├── design/
│   │   ├── VaibTheme.kt          ← tokens + theme
│   │   ├── VaibColors.kt         ← full color system
│   │   ├── VaibTypography.kt     ← custom font specs
│   │   ├── VaibAtmosphere.kt     ← station atmosphere model
│   │   └── MotionTokens.kt       ← animation specs
│   ├── audio/
│   │   ├── AudioEngine.kt        ← unified ExoPlayer wrapper
│   │   ├── AudioVisualizer.kt    ← energy + beat (existing refactored)
│   │   ├── EqController.kt       ← enhanced multi-band EQ
│   │   ├── AudioFocusManager.kt  ← duck + focus handling
│   │   └── AudioModels.kt
│   ├── data/
│   │   ├── entities/             ← Room entities
│   │   ├── dao/                  ← VaibDao, TrackDao, StationDao, QueueDao
│   │   ├── repository/           ← Repository layer
│   │   └── prefs/                ← Settings + track prefs
│   └── service/
│       ├── PlayerService.kt      ← Foreground service
│       └── NotificationBuilder.kt
├── stations/
│   ├── StationsScreen.kt
│   ├── StationCard.kt
│   └── StationEditor.kt
├── player/
│   ├── NowPlayingScreen.kt       ← full-screen player
│   ├── MiniPlayer.kt             ← bottom sheet / inline
│   ├── TransportControls.kt
│   ├── ProgressBar.kt
│   └── EqualizerPanel.kt
├── library/
│   ├── LibraryScreen.kt          ← all tracks + stations + vaibs
│   └── TrackList.kt
├── discover/
│   ├── DiscoverScreen.kt         ← redesigned
│   ├── DiscoverCard.kt
│   └── InternetArchiveApi.kt
├── visualizer/
│   ├── VisualizerSurface.kt      ← OpenGL wrapper
│   ├── renderers/
│   │   ├── NebulaRenderer.kt     ← current shader improved
│   │   ├── WaveformRenderer.kt   ← new
│   │   └── ParticleRenderer.kt   ← new
│   └── VisualizerModels.kt
├── vaib/
│   ├── VaibCard.kt
│   ├── VaibModels.kt
│   └── SaveVaibDialog.kt
└── ui/
    ├── HomeScreen.kt             ← redesigned hub
    ├── OnboardingScreen.kt       ← new
    └── components/               ← shared composables
        ├── GradientText.kt
        ├── AnimatedGradientBg.kt
        └── HapticButton.kt
```

### 10.2 Key Dependencies (add/update)
```kotlin
// Already present and keep:
implementation("androidx.media3:media3-exoplayer:1.4.1")
implementation("androidx.media3:media3-session:1.4.1")
implementation("androidx.room:room-runtime:2.6.1")

// Add:
implementation("androidx.media3:media3-ui:1.4.1")        // PlayerNotificationManager
implementation("androidx.lifecycle:lifecycle-service:2.8.6") // Service lifecycle
implementation("coil-compose:2.8.0")                       // if adding album art loading
```

---

## 11. Phased Implementation Plan

### Phase 1 — Foundation (Week 1–2)
- [ ] **Setup:** Create new branch (`overhaul/aaa-visual-redesign` — ✅ done)
- [ ] **Design tokens:** Expand `VaibColors`, create `MotionTokens`, update `VaibTypography`
- [ ] **Station entity:** Add `StationEntity`, `StationTrackCrossRef`, DAOs, migrations
- [ ] **Repository layer:** Extract repositories from ViewModel
- [ ] **Package restructure:** Move files into target package structure

### Phase 2 — Core Player (Week 2–3)
- [ ] **Queue system:** Add Queue entity, queue management in ViewModel
- [ ] **Full-screen player:** `NowPlayingScreen` with expanded controls, EQ panel
- [ ] **Mini-player:** Persistent bottom bar when navigating away from player
- [ ] **Foreground service:** Background playback with Media3 Session notification
- [ ] **Audio focus:** Proper duck/interrupt handling
- [ ] **Crossfade:** Configurable track transitions

### Phase 3 — Visualizers (Week 3–4)
- [x] **Shader refactor:** Abstract `VisualizerRenderer` base, add style selector
- [x] **Nebula shader:** Improve existing (better bloom, particles, responsive energy)
- [x] **Waveform shader:** New time-domain visualization
- [x] **Particle shader:** New GPU particle system with beat-reactive spawning
- [x] **Home screen mini-viz:** Live visualizer preview in NowPlayingCard
- [x] **Touch interaction:** Visualizer responds to taps and drags

### Phase 4 — Stations & Organization (Week 4–5)
- [x] **Stations screen:** Grid layout, add/edit/delete/reorder stations
- [x] **Station tracks:** Add tracks to stations, browse by station
- [x] **Library screen:** Unified view of all stations + tracks + vAIbs
- [x] **Smart stations:** Auto-generated "All Tracks", "Favorites", "Recent"
- [x] **Search improvements:** Filter by station, mood, source

### Phase 5 — Polish & Onboarding (Week 5–6)
- [ ] **Onboarding flow:** 3-screen intro with permission explanation
- [ ] **Discover redesign:** Categorized sections, better cards, preview
- [ ] **Animations:** Entry/exit transitions, shared element transitions, haptics
- [ ] **Sleep timer:** Notification-based timer service
- [ ] **Settings screen:** EQ defaults, crossfade duration, theme picker, sleep timer
- [ ] **Export/import:** vAIb backup to/from JSON

### Phase 6 — AAA Polish (Week 6–7)
- [ ] **Performance:** Profile GPU rendering, optimize shader complexity
- [ ] **Battery optimization:** Reduce wake locks, efficient visualizer polling
- [ ] **Accessibility:** TalkBack labels, high-contrast mode support
- [ ] **Dynamic colors:** Extract dominant color from album art for theme
- [ ] **Edge-to-edge:** Proper cutout handling, gesture navigation
- [ ] **Splash screen:** Animated logo with ambient sound wave

---

## 12. Version Target

| | Current | Target |
|---|---|---|
| Version name | `0.1.0` | `1.0.0` |
| Version code | `1` | `100` |
| Min SDK | 29 (Android 10) | 26 (Android 8.0) |
| Target SDK | 36 | 36 |

---

## 13. Quality Gates

Before merge to `main`:
- [ ] All existing tests pass
- [ ] No lint errors or warnings
- [ ] APK size < 15 MB (baseline)
- [ ] 60 fps on mid-range device during fullscreen visualizer
- [ ] Memory stable (no leaks over 30-min playback session)
- [ ] Battery drain < 5% per hour with screen off + background audio
- [ ] Passes Android Vitals thresholds (ANR rate < 1%, crash rate < 0.1%)

---

*This plan lives on the `overhaul/aaa-visual-redesign` branch. Each phase produces a working intermediate state — the app should be shippable after any phase.*