# AI_INSTRUCTIONS.md — Engineering Guidelines for DroidDeck

## Coding & Architecture Rules

1. **Language & UI Toolkit:**
   - Write 100% idiomatic Kotlin targeting JVM 17 and Jetpack Compose Material 3.
   - Keep `MainActivity` thin; all state aggregation belongs in `DroidDeckDashboardRepository` and `DashboardViewModel`.
   - Use `StateFlow` and `collectAsStateWithLifecycle()` for all reactive UI bindings.
2. **Android System Services:**
   - `DroidDeckNotificationService` (`NotificationListenerService`) is the single bridge for active notifications, incoming/ongoing call alerts (`Notification.CATEGORY_CALL`), call notification actions (Speaker/Mute/Hang up), and `MediaSessionManager` transport controls, and registers a background-to-front handler for `MainActivity` when `SYSTEM_ALERT_WINDOW` is granted.
   - Always guard `PendingIntent.send()`, `NotificationListenerService.cancelNotification()`, and `ConnectivityManager` / `BluetoothManager` calls with `runCatching` to handle OS permission revocations gracefully.
   - `WorkingHoursLockService` must use `FOREGROUND_SERVICE_SPECIAL_USE` with a `PRIORITY_MIN` silent notification and register `ACTION_SCREEN_OFF` using `ContextCompat.RECEIVER_NOT_EXPORTED`.
3. **OLED & Power Discipline:**
   - Never add continuous `InfiniteTransition` animations on the idle dashboard screen; reserve animations strictly for transient events (e.g., active incoming calls or user-initiated bottom sheets).
   - Keep `PureBlack` (`#000000`) as the canvas color for all dashboard states.
4. **Testing & Verification:**
   - Run `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` after any code modification.
   - Keep pure logic (such as `WorkingHoursEvaluator`, `PixelShiftCalculator`, `MediaProgressCalculator`, and `UserPreferencesRepository` JSON serialization) decoupled from Android `Context` so it remains unit-testable on the JVM.
5. **Documentation & Changelog Sync:**
   - Update `CHANGELOG.md` under the active version for every feature, fix, or refactor.
   - Update `STRUCTURE.md` whenever files are added, moved, or removed.
   - Keep `README.md` strictly compliant with the universal template rules (no emojis, neutral `style=flat` badges, no personal PII).
