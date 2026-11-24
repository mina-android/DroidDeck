# Changelog

All notable changes to **DroidDeck** (`com.ma.droiddeck`) are documented in this file.

## [1.0.0] - 2026-11-24

Initial public release of **DroidDeck** (`com.ma.droiddeck`).

### Added
- **Pure `#000000` OLED Dashboard**: Native Kotlin and Jetpack Compose (Material 3) application targeting SDK 35 (`minSdk 26`) with `1.dp` `#1F1F1F` (`OledBorder`) outlines and selectable accent palettes (`Pure White`, `Warm Amber`, `Night Red`, `Soft Cyan`, `Dynamic Material You`).
- **Orientation-Adaptive Unified Widget Zone (`DroidDeckScreen`)**: Resizable and reorderable `CLOCK`, `RECENT_CALLS`, `MUSIC_CONTROLS`, `NOTIFICATIONS`, and `QUICK_APPS` widgets supporting 1-column Portrait drag-and-drop and 2-column Landscape cross-column drag-and-drop.
- **Clock, Typography & Embedded Utilities (`ClockSection`)**: Multiple clock faces (`Modern Digital`, `Minimal Analog Dial`, `Bold Condensed`, `Dual Timezone`), Google Fonts selector (`Plus Jakarta Sans`, `DM Sans`, `Inter`, `Space Grotesk`, `Outfit`, `Sora`, `Poppins`, `Nunito`, `Monospace`, `System Default`), embedded Weather pill (Open-Meteo + ambient sensor fallback), embedded Calendar card (`Xh Ym` countdown), and built-in Pomodoro timer.
- **Media, Volume & Notification Engine (`DroidDeckNotificationService`)**: Direct `NotificationListenerService` and `MediaSessionManager` integration with dimmed monochrome artwork, live-extrapolated playback progress (`MediaProgressCalculator`), interactive seekbar scrubbing, integrated Media Volume controls, up to 5 dismissible notification pills with app blacklisting, and DND-aware hiding.
- **Incoming & Ongoing Call Controls (`CallActionHelper`, `IncomingCallBanner`, `OngoingCallBanner`)**: Pinned cellular/VoIP call banners above the Clock with Answer, Decline, Mute Ringer, live duration timer, Speakerphone toggle, Mic Mute toggle, and End Call actions via `TelecomManager` and `Notification.CallStyle`, plus optional `SYSTEM_ALERT_WINDOW` re-fronting.
- **Battery & OLED Burn-In Protection**: Minute-aligned clock ticker (0 FPS idle), periodic `[-4, +4]` dp pixel shifting (`PixelShiftCalculator`), `1%–100%` brightness dimming with tap-to-brighten, and `Sensor.TYPE_PROXIMITY` face-down blanking (`StatusMonitor`).
- **Working Hours & Lockscreen Automation (`WorkingHoursLockService`, `BootCompletedReceiver`)**: `FOREGROUND_SERVICE_SPECIAL_USE` listener monitoring `ACTION_SCREEN_OFF` during user-configured days, time windows, and charging states, with a `Show Over Lockscreen` toggle.
- **System Entry Points & Desk Utilities**: Quick Settings tile (`DroidDeckTileService`), top-left Exit button in `StatusHeader`, Sound/Vibrate/Mute ringer cycling pill decoupled from DND, persisted `Auto`/`Portrait`/`Landscape` orientation toggle, 4 circular deduplicated Recent Contacts (`RecentCallsWidgetCard`), borderless `QuickAppDockCard`, and JSON Settings Export/Import (`droiddeck_backup.json`) backed by Jetpack DataStore (`droiddeck_settings`).
- **Adaptive Icon & Release Packaging**: 3D Material 3 adaptive launcher icon (`ic_launcher`, `ic_launcher_round`, monochrome layer), `assets/logo.png`, ABI-split + universal release APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`, `universal`) with R8 minification, and unit tests (`WorkingHoursEvaluatorTest`, `PixelShiftCalculatorTest`, `MediaProgressCalculatorTest`, `WidgetOrderTest`, `SettingsBackupTest`, `CalendarCountdownTest`).
- **Repository Hygiene**: Ignored local `AI_INSTRUCTIONS.md` and `GEMINI.md` in `.gitignore`, aligned `README.md` `shields.io` badges for GitHub Camo rendering, and converted Developer/Donate links (including Ko-fi) to inline `shields.io` badges.
