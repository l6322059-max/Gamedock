# GameDock

A lightweight Android game launcher built with Kotlin and Jetpack Compose.

## Current features

- Scans launcher-visible apps and detects likely games using app names and package IDs.
- Favorites saved locally on the device.
- One-tap launch from the library.
- Dashboard with detected-game and favorite counts.
- Four selectable accent themes.
- Dark gaming-focused interface.
- GitHub Actions workflow builds a debug APK on every push to `main` or manual dispatch.

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

## Notes and limitations

- Game detection is heuristic; it may miss games or include non-games. Android app visibility rules limit what any launcher can discover.
- Favorites and theme selection are stored locally using app preferences.
- Live FPS, device temperature, and per-game performance telemetry are not claimed as available. Android does not provide ordinary apps with a universal API for reading another app's live FPS.
- This is an initial project foundation. The first GitHub Actions build still needs to run before an APK can be called verified.
