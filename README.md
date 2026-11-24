# Multi Minuteurs

A simple Android app for running several timers at the same time.

Kotlin + Jetpack Compose, no ads, no tracking, no network access. Everything stays on the device.

## Features

- Run as many timers as you want in parallel
- Start, pause, resume, reset, edit or delete each timer on its own
- Hour / minute / second picker wheels (they loop, snap into place and give haptic feedback)
- Each timer gets its own color, and its border fills up as a progress bar
- Ongoing notification with a live countdown and a pause button
- Alarm with sound and vibration when a timer ends, even if the app is closed or the screen is locked
- Timers are saved and rescheduled after a reboot
- Settings: language (system / English / French) and an optional hundredths-of-a-second display
- "Glass" look over a slowly animated background

## Build

### With GitHub Actions (no computer needed)

Every push to `main` builds a debug APK. Open the **Actions** tab, pick the latest run and download the
`MultiMinuteurs-debug` artifact. You can also start a build by hand with **Run workflow**.

### With Android Studio

1. **File → Open** and select this folder.
2. Let Gradle sync.
3. **Build → Build APK(s)**, or run it on a phone with USB debugging on.

Requirements: JDK 17, Android SDK 35. The minimum Android version is 8.0 (API 26).

## Permissions

| Permission | Why |
| --- | --- |
| `POST_NOTIFICATIONS` | Countdown and alarm notifications |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Ring at the exact end time |
| `RECEIVE_BOOT_COMPLETED` | Reschedule timers after a reboot |
| `VIBRATE` | Vibrate when a timer ends |

## Project layout

```
app/src/main/java/com/example/multiminuteurs/
├── MainActivity.kt   entry point
├── Ui.kt             screens, timer cards, dialogs
├── Glass.kt          glass style, animated background, picker wheels, progress border
├── Model.kt          timer data and time formatting
├── TimerStore.kt     saving timers
├── TimerActions.kt   start / pause / edit logic and alarm scheduling
├── Notifs.kt         notification channels and notifications
├── Receivers.kt      alarm and boot receivers
└── Settings.kt       settings and FR/EN strings
```
