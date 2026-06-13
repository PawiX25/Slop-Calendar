# Slop Calendar

A home-screen **calendar timeline widget** for Android, built with **Jetpack Compose**,
**Jetpack Glance** and a **Material 3 Expressive** UI. It shows your upcoming events as a
clean agenda — much like the Google Calendar widget — and lets you pick which calendars
appear.

## Features

- 🗓️ **Timeline / agenda widget** that reads the system calendar (`CalendarContract`).
  Each day shows a date pill (filled circle for *today*) and rounded event chips colored
  by their calendar, with automatic text-contrast.
- ➕ Tap **+** to create an event, tap a chip to open it.
- 🎨 **Material You** dynamic color — the widget and app adopt your wallpaper palette
  (Android 12+), with a graceful fallback below that.
- ⚙️ **Material 3 Expressive** companion app (`MaterialExpressiveTheme`) to toggle which
  calendars are shown; the choice is persisted with DataStore.
- 🔄 Auto-refreshes on day rollover, timezone change and calendar edits.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4.0 |
| UI | Jetpack Compose (BOM 2026.05.01), Material 3 Expressive (`material3:1.5.0-alpha18`) |
| Widget | Jetpack Glance 1.1.1 |
| Storage | DataStore Preferences 1.2.1 |
| Build | AGP 8.13.2, Gradle 8.14 · compileSdk/targetSdk 36 · minSdk 26 (Android 8.0+) |

## Build

```bash
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

> Requires the `READ_CALENDAR` permission (requested on first launch). The widget shows a
> "grant access" prompt until it is granted.

## Adding the widget

Install the app, then long-press the home screen → **Widgets** → **Slop Calendar**, or use
the **"Add to home screen"** button inside the app.

## License

MIT
