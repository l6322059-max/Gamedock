# GameDock

A lightweight Android game launcher with a Game Turbo-inspired command center and a draggable floating toolbox, built with Kotlin and Jetpack Compose.

## Features

- Scans launcher-visible apps and detects likely games using app names and package IDs.
- Favorites saved locally on the device, one-tap game launching and instant library search by game name or package ID.
- Gaming dashboard with detected-game counts, available RAM and battery temperature when Android reports it.
- Game Turbo-inspired screen and draggable floating bubble that can stay above other apps and games.
- Floating panel shortcuts to Android display/brightness, sound, battery/power and Do Not Disturb settings.
- Four selectable accent themes and a dark gaming-focused interface.
- GitHub Actions workflow builds a debug APK on every push to `main` or manual dispatch.

## Enable the floating toolbox

1. Open GameDock and go to **Turbo**.
2. Open **Gaming tools** and tap **Enable floating bubble**.
3. Android opens the **Display over other apps** permission page. Allow GameDock.
4. Return to GameDock and tap **Enable floating bubble** again.
5. A draggable **GD ⚡** bubble appears above other apps. Tap it to open quick settings shortcuts.
6. Use **Minimize bubble** to collapse it. Use **Turn off floating tools** or the ongoing notification's **Stop** action to remove it.

Android and Xiaomi/HyperOS may show the permission page or background-service controls differently. If the bubble disappears, check GameDock's overlay permission and battery/background restrictions.

## Android limitations

- The floating bubble requires the user to grant Android's special **Display over other apps** permission.
- Live FPS for another app is not exposed through a universal public Android API. GameDock reports this honestly instead of displaying made-up numbers.
- Battery temperature and available RAM depend on what the device and Android version report.
- A normal third-party app cannot reproduce Xiaomi's privileged Game Turbo thermal policies or OEM-level CPU/GPU tuning.
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
