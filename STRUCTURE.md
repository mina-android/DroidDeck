# STRUCTURE.md — DroidDeck Project Structure

```text
DroidDeck/
├── .gitignore                                      # Hidden (+H) Android/Gradle/IDE ignore rules
├── AI_INSTRUCTIONS.md                              # Engineering conventions for AI agents
├── CHANGELOG.md                                    # Release history
├── GEMINI.md                                       # Project context & architectural invariants
├── LICENSE                                         # GNU General Public License v3.0
├── README.md                                       # Project documentation (template-compliant)
├── STRUCTURE.md                                    # Directory & module map
├── build.gradle.kts                                # Root Gradle plugin declarations
├── gradle.properties                               # JVM, AndroidX, and Kotlin style flags
├── settings.gradle.kts                             # Module configuration (DroidDeck)
├── assets/
│   ├── logo.png                                    # 3D Material 3 application logo
│   └── screenshots/
│       └── home.png                                # Main dashboard screenshot used in README.md
├── gradle/
│   ├── libs.versions.toml                          # Centralized dependency version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties               # Gradle 8.11.1 wrapper configuration
└── app/
    ├── build.gradle.kts                            # App module config (com.ma.droiddeck, SDK 35)
    ├── proguard-rules.pro                          # R8/ProGuard rules
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml                 # Permissions, Activities, Services, Quick Settings Tile
        │   ├── java/com/ma/droiddeck/
        │   │   ├── DroidDeckApp.kt                  # Application class, notification channels, preferences
        │   │   ├── MainActivity.kt                 # Lockscreen-configurable, rotatable, immersive OLED host
        │   │   ├── data/
        │   │   │   ├── model/DashboardModels.kt    # Immutable domain, widget order, & settings models
        │   │   │   ├── preferences/UserPreferencesRepository.kt # DataStore persistence
        │   │   │   ├── repository/
        │   │   │   │   ├── CalendarWeatherRepository.kt # Calendar, CallLog, Quick Apps, Open-Meteo weather
        │   │   │   │   └── DroidDeckDashboardRepository.kt   # Singleton StateFlow coordinator
        │   │   ├── service/
        │   │   │   ├── BootCompletedReceiver.kt    # Restores Working Hours monitor on boot
        │   │   │   ├── DroidDeckTileService.kt      # Quick Settings notification panel tile
        │   │   │   ├── DroidDeckNotificationService.kt  # NotificationListener + MediaSession bridge
        │   │   │   ├── StatusMonitor.kt            # Battery, network, Bluetooth, volume, ringer, proximity
        │   │   │   └── WorkingHoursLockService.kt  # Screen-off listener for scheduled auto-launch
        │   │   ├── ui/
        │   │   │   ├── components/
        │   │   │   │   ├── CalendarWidgetCard.kt   # Compact next-meeting card inside ClockSection (h/m countdown)
        │   │   │   │   ├── ClockSection.kt         # Borderless multi-style AOD clock, weather, calendar, & timer
        │   │   │   │   ├── IncomingCallBanner.kt   # Pinned cellular/VoIP call alert with Answer/Decline/Mute
        │   │   │   │   ├── MusicControlCard.kt     # Compact media controls, slim seekbar, & volume bar
        │   │   │   │   ├── NotificationPillList.kt # Monochrome icons, Clear All, DND-aware hiding
        │   │   │   │   ├── OngoingCallBanner.kt    # Pinned in-app active call timer, speaker, mute, end call
        │   │   │   │   ├── QuickAppDockCard.kt     # Borderless 4 configurable monochrome quick-launch apps
        │   │   │   │   ├── RecentCallsWidgetCard.kt# Standalone circular 4 recent calls widgets (1 row)
        │   │   │   │   ├── StatusHeader.kt         # Exit button, battery, Car Mode, DND, ringer, rotate, settings
        │   │   │   │   └── WeatherWidgetCard.kt    # Legacy standalone weather card (replaced by ClockSection pill)
        │   │   │   ├── dashboard/
        │   │   │   │   ├── DashboardViewModel.kt   # Minute-aligned ticker, call/media timers, state
        │   │   │   │   └── DroidDeckScreen.kt       # Unified widget zone & Landscape cross-column drag
        │   │   │   ├── settings/
        │   │   │   │   └── SettingsBottomSheet.kt  # Fonts, clock styles, 1-100% brightness, backup, About
        │   │   │   └── theme/Theme.kt              # Pure #000000 OLED theme & Google Fonts provider
        │   │   └── util/
        │   │       ├── CallActionHelper.kt         # CallStyle PendingIntent + TelecomManager fallback
│   │   │   │   │   ├── DndPolicyHelper.kt          # Sound-preserving popup-hiding NotificationManager.Policy
        │   │       ├── MediaProgressCalculator.kt  # Extrapolated live MediaSession playback position
        │   │       ├── PixelShiftCalculator.kt     # Bounded [-4, +4] dp burn-in protection
        │   │       └── WorkingHoursEvaluator.kt    # Day/time/charging schedule evaluator
        │   └── res/
        │       ├── drawable/
        │       │   ├── ic_launcher_background.xml  # Pure white (#FFFFFF) adaptive icon background
        │       │   ├── ic_launcher_foreground.xml  # Safe-margin 3D Material 3 desk slab foreground
        │       │   ├── ic_launcher_monochrome.xml  # Android 13+ themed monochrome icon
        │       │   └── ic_qs_droiddeck.xml         # Quick Settings tile vector icon
        │       ├── mipmap-anydpi-v26/
        │       │   ├── ic_launcher.xml             # Adaptive launcher icon definition
        │       │   └── ic_launcher_round.xml       # Round adaptive launcher icon definition
        │       └── values/
        │           ├── font_certs.xml              # Valid GMS downloadable Google Fonts certificates
        │           ├── strings.xml                 # App strings & channel labels
        │           └── themes.xml                  # Pure-black window theme
        └── test/java/com/ma/droiddeck/
            ├── CalendarCountdownTest.kt          # Unit tests for h/m calendar countdown formatting
            ├── MediaProgressCalculatorTest.kt      # Unit tests for live media progress extrapolation
            ├── PixelShiftCalculatorTest.kt         # Unit tests for OLED pixel shift bounds
            ├── StatusMonitorRingerTest.kt          # Unit tests for DND-decoupled ringer state resolution
            ├── WidgetOrderTest.kt                  # Unit tests for widget order persistence
            └── WorkingHoursEvaluatorTest.kt        # Unit tests for working-hours schedule logic
```
