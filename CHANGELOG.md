# Changelog

All notable changes to **DroidDeck** (`com.ma.droiddeck`) are documented in this file.

## [1.2.0] - 2026-11-23

### Added
- Unified widget zone making `CLOCK` (`DashboardWidgetType.CLOCK`) a resizable, draggable widget (`S/M/L/XL` + `0.65x–1.40x` slider) with cross-column Landscape drag-and-drop.
- Multiple Clock Faces (`Modern Digital`, `Minimal Analog Dial`, `Bold Condensed`, `Dual Timezone`) and an `expensy`-style **App Font Selector** (`Plus Jakarta Sans`, `DM Sans`, `Inter`, `Space Grotesk`, `Outfit`, `Sora`, `Poppins`, `Nunito`, `Monospace`, `System Default`).
- Dedicated **DND** toggle button in the enlarged `StatusHeader` alongside `Sound / Vibrate / Mute`.
- Foreground heads-up & incoming-call popup suppression (`NotificationManager.Policy` `SUPPRESSED_EFFECT_PEEK` + `SUPPRESSED_EFFECT_FULL_SCREEN_INTENT`, restored on `onStop()`).
- In-app **Ongoing Call** card (`OngoingCallBanner`) with live duration timer, Speakerphone toggle, Mic Mute toggle, and End Call button.
- **2 Recent Calls** movable widget (`RecentCallsWidgetCard`) with CallLog integration and double-tap to call.
- **Calendar & Next Meeting**, **Weather & Ambient Temperature** (Open-Meteo + sensor fallback), and **Quick App Dock** widgets with per-widget visibility toggles.
- Interactive media seekbar scrubbing (`MediaController.transportControls.seekTo`), monochrome notification app icons, **Clear All** notifications button, double-tap notification to open app, notification app blacklist, and sub-pixel anti-burn-in idle dimming.
- Full `1%–100%` brightness dimming levels with quick preset chips, JSON Settings Export/Import backup, and an `expensy`-style **About** section (Version, Privacy, GitHub, Developer dialog).
- Top-left **Exit** button in `StatusHeader` (terminates `WorkingHoursLockService`, finishes task, and kills process), persisted **Orientation Mode** (`Auto`, `Portrait`, `Landscape`), **Show Weather in Clock**, and **Show Calendar in Clock** toggles.
- Embedded compact Weather pill and bordered Calendar card (`in Xh Ym` countdown) inside a borderless `ClockSection`.
- Circular standalone **Recent Contacts** row showing 4 distinct deduplicated contacts (`RecentCallsWidgetCard`), and borderless `QuickAppDockCard` icons.
- Optional `SYSTEM_ALERT_WINDOW` ("Display Over Other Apps") permission in Settings for reliable Android 17 re-fronting over the system call screen and popups.

### Changed
- Renamed application, package (`com.ma.droiddeck`), symbols (`DroidDeck*`), DataStore (`droiddeck_settings`), notification channel (`droiddeck_working_hours`), backup filename (`droiddeck_backup.json`), and GitHub repository links to **DroidDeck**.
- Aligned `README.md` with the universal README template specification (`style=flat` badges, `Android 8.0+` Platform & `Jetpack Compose` Framework badges, pinned developer/donate links, zero emojis) and updated `assets/logo.png` to the transparent adaptive icon foreground.
- Pinned `IncomingCallBanner` and `OngoingCallBanner` directly above `ClockSection` in both Portrait and Landscape.
- Matched `CalendarWidgetCard` event start times to `use24HourFormat` and formatted countdowns in hours and minutes.
- Made `MusicControlCard` more compact with slimmer custom song and volume seekbars (`CompactSeekbar`).
- Hidden notification pills behind a `"Notifications hidden — DND active"` placeholder when DND is active, and removed `"Double-tap..."` helper texts.
- Decoupled Ringer Mute from system DND (muting `STREAM_RING`/`STREAM_NOTIFICATION` instead of `RINGER_MODE_SILENT`).

### Fixed
- Fixed ongoing call Speaker button by extracting and triggering the call notification's Speaker/Audio `PendingIntent` alongside `AudioManager.setCommunicationDevice` + `isSpeakerphoneOn` fallback and `OnCommunicationDeviceChangedListener` sync.
- Fixed glitchy/laggy widget drag-and-drop reordering by maintaining local in-memory drag order/column state, accounting for `12.dp` spacing, and committing once to DataStore on drag end.
- Fixed Android 17 status bar appearing during calls using `FLAG_FULLSCREEN`, `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`, `displayCutoutPadding()`, and an `OnApplyWindowInsetsListener` re-hide guard.
- Fixed placing/answering calls and incoming call popups covering DroidDeck by preferring `TelecomManager.placeCall` / `acceptRingingCall()` and re-fronting `MainActivity` via `DroidDeckNotificationService` + overlay exemption.
- Fixed Google Fonts not changing by replacing truncated GMS font certificates in `font_certs.xml` and applying `fontFamily` across all Material 3 `Typography` slots.
- Fixed DroidDeck activating system DND and muting media on launch by removing automatic `INTERRUPTION_FILTER_PRIORITY` activation and including `PRIORITY_CATEGORY_MEDIA` in `NotificationManager.Policy`.
- Fixed Weather city input state resetting during DataStore updates.

### Fixed (Previous)
- Fixed incoming call Answer/Decline/Mute Ringer buttons using Android 12+ `Notification.CallStyle` extras, API 34+ `ActivityOptions` background-start permission, and `TelecomManager` (`ANSWER_PHONE_CALLS`) fallback.

## [1.1.0] - 2026-11-23

### Added
- Integrated Media Volume controls (mute toggle, `-`/`+` step buttons, and slider) inside `MusicControlCard`.
- Sound / Vibrate / Mute cycling pill button in `StatusHeader` backed by `AudioManager.ringerMode`.
- On-screen Rotate button in `StatusHeader` (tap to toggle Portrait/Landscape, long-press to reset to Auto-Rotate).
- `Show Over Lockscreen` toggle in `SettingsBottomSheet` (`DroidDeckSettings.showOnLockScreen`).
- Long-press drag-and-drop widget reordering directly on the dashboard (`IncomingCallBanner`, `MusicControlCard`, `NotificationPillList`), persisted in Jetpack DataStore.
- Unit tests in `MediaProgressCalculatorTest` and `WidgetOrderTest`.

### Changed
- Scaled the 3D adaptive launcher icon foreground (`0.62x`) and legacy mipmap PNGs (`0.60x`) onto a pure `#FFFFFF` white background with safe margins.
- Enlarged AOD clock typography (`98.sp` Portrait / `84.sp` Landscape) and `MusicControlCard` artwork (`56.dp`), transport buttons (`48.dp`–`52.dp`), and progress bar (`3.dp`).
- Fixed live media playback progress updates by extrapolating `PlaybackState.lastPositionUpdateTime` and `playbackSpeed` strictly while `isPlaying == true`.

### Removed
- Removed all homescreen/launcher shortcuts (`res/xml/shortcuts.xml`, dynamic shortcut registration, and in-app shortcut pinning) while retaining the Android Quick Settings tile.

## [1.0.0] - 2026-11-23

### Added
- Native Android Kotlin + Jetpack Compose (Material 3) project structure targeting SDK 35 (`minSdk 26`).
- Pure `#000000` OLED theme with selectable accent palettes (Pure White, Warm Amber, Night Red, Soft Cyan, Material You Dynamic).
- Adaptive orientation dashboard:
  - **Portrait:** Clock, date, and Pomodoro focus timer at the top; incoming call alert, music controls, and notification pills below.
  - **Landscape:** Clock, date, and Pomodoro focus timer on the left; incoming call alert, music controls, and notification pills on the right.
- System `DroidDeckNotificationService` (`NotificationListenerService`) integrating:
  - `MediaSessionManager` playback metadata, monochrome artwork, progress bar, and transport controls (Play/Pause/Previous/Next).
  - Compact AOD Notification Pill List (up to 5 recent non-ongoing notifications with inline expand, keyguard-safe launch, and dismiss).
  - Interactive Incoming Call Alert Banner for cellular and VoIP calls with Answer, Decline, and Mute Ringer actions.
- `StatusMonitor` tracking battery percentage, charging status, battery temperature (`°C`), Wi-Fi, Cellular, Bluetooth, Do Not Disturb, and proximity sensor blanking.
- Battery & OLED protection suite: minute-aligned clock tick (0 FPS idle), periodic `[-4, +4]` dp pixel shifting, auto-dim floor with tap-to-brighten, and proximity face-down blanking.
- Working Hours Auto-Launch (`WorkingHoursLockService` + `BootCompletedReceiver`) to automatically display DroidDeck over the lockscreen during configured days, time windows, and optional charging state.
- Entry-point shortcuts: Android Quick Settings Tile (`DroidDeckTileService`), static/dynamic launcher shortcuts, and in-app Pin to Home Screen action.
- 3D Material 3 adaptive launcher icon (`ic_launcher`, `ic_launcher_round`, monochrome layer) and `assets/logo.png`.
- ABI split APK configuration (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`, plus `universal`) in `app/build.gradle.kts`.
- Full project documentation (`README.md`, `GEMINI.md`, `STRUCTURE.md`, `AI_INSTRUCTIONS.md`, `CHANGELOG.md`), GNU GPL-3.0 `LICENSE`, and hidden `.gitignore`.
