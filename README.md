# GameDock

A lightweight Android game launcher with a Game Turbo-inspired command center, built with Kotlin and Jetpack Compose.

## Features

- Scans launcher-visible apps and detects likely games using app names and package IDs.
- Favorites saved locally on the device.
- One-tap game launching from the library.
- Gaming dashboard with detected-game and favorite counts.
- Device information: available RAM and battery temperature when Android reports it.
- Game Turbo-inspired screen with a quick-access gaming toolbox.
- Toolbox shortcuts to Android display/brightness, sound, battery/power, Do Not Disturb access, and general settings.
- Four selectable accent themes and a dark gaming-focused interface.
- GitHub Actions workflow builds a debug APK on every push to `main` or manual dispatch.

## About the Turbo toolbox

The toolbox is a floating-style in-app panel. Its shortcuts open Android's own settings pages, where the user can make system changes. It does not silently change protected settings, overclock the device, or claim to improve CPU/GPU performance.

## Android limitations

- Live FPS for another app is not exposed through a universal public Android API. GameDock reports this honestly instead of displaying made-up numbers.
- Battery temperature and available RAM depend on what the device and Android version report.
- A normal third-party app cannot reproduce Xiaomi's privileged Game Turbo controls, thermal policies, or OEM-level CPU/GPU tuning.
- Game detection is heuristic and limited by Android package-visibility rules. It may miss games or include non-games.

## Build an APK

1. Open the **Actions** tab in this repository.
2. Select **Build GameDock APK**.
3. Tap **Run workflow** if a run has not started automatically.
4. Open the completed run and download the **GameDock-debug** artifact.
5. Extract the artifact ZIP to get `app-debug.apk`.

## Build locally

Requires JDK 17 and Android SDK platform 35.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
