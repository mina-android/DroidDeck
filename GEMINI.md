# GEMINI.md — DroidDeck (`com.ma.droiddeck`)

## Project Overview

**DroidDeck** is an Android-only Always-On Display (AOD) desk dashboard and control center built natively with **Kotlin**, **Jetpack Compose (Material 3)**, **Coroutines/StateFlow**, and **Jetpack DataStore**.

- **Package Name:** `com.ma.droiddeck`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target / Compile SDK:** Android 15 (API 35)
- **License:** GNU General Public License v3.0 (`LICENSE`)

## Core Architectural Invariants

1. **Pure `#000000` OLED Surfaces:**
   - All primary backgrounds, scaffolds, bottom sheets, and card containers must use `PureBlack` (`#000000`).
   - Do not introduce gray surface fills (`#121212`, `#1E1E1E`) on the main dashboard; use thin `1.dp` `OledBorder` (`#1F1F1F`) outlines and low-luminance accent tints instead.
2. **0 FPS Idle Clock & Battery Efficiency:**
   - When `showSeconds` is `false`, `DashboardViewModel` aligns its clock coroutine delay to the exact next minute boundary (`60_000L - (nowMs % 60_000L)`). Never introduce a polling loop faster than the active mode requires.
   - Live media progress extrapolation (`MediaProgressCalculator`) runs a 1-second ticker **strictly while `media.isPlaying == true`**, cancelling immediately when paused or inactive.
   - Proximity blanking (`Sensor.TYPE_PROXIMITY`) must render a 100% empty `#000000` box and set window brightness to `0.0f` when covered.
   - OLED pixel shifting (`PixelShiftCalculator`) offsets the root layout within `[-4, +4]` dp every minute.
3. **Orientation-Adaptive Unified Widget Zone:**
   - The entire area below `StatusHeader` is a unified reorderable widget zone (`CLOCK`, `RECENT_CALLS`, `MUSIC_CONTROLS`, `NOTIFICATIONS`, `QUICK_APPS`).
   - Incoming and ongoing call banners (`IncomingCallBanner`, `OngoingCallBanner`) are always pinned immediately above `ClockSection`, and Weather & Calendar are embedded inside the borderless `ClockSection`.
   - **Portrait:** 1-column vertical drag-and-drop list.
   - **Landscape:** 2-column adaptive grid supporting cross-column drag-and-drop.
4. **Lockscreen & Working Hours Safety:**
   - `MainActivity` respects `DroidDeckSettings.showOnLockScreen` (`setShowWhenLocked(show)`), and `WorkingHoursLockService` only auto-launches when `showOnLockScreen` is enabled.
   - Opening any external notification `PendingIntent` must go through `KeyguardManager.requestDismissKeyguard()` when the device is locked.
5. **Documentation Synchronization:**
   - Any code, schema, permission, or build configuration change requires updating `README.md`, `STRUCTURE.md`, `CHANGELOG.md`, `GEMINI.md`, and `AI_INSTRUCTIONS.md`.
