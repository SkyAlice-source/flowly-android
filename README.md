# Flowly

A **UI-redesigned fork** of [ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid), built on the [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta) core. All proxy features from Clash.Meta are retained; the focus of this project is a full rework of the UI and interaction.

> Personal UI experiment. Not affiliated with MetaCubeX.

## What changed (UI)

- Full **Material 3** visual system, brand color `#496CEF`, wave / flat-rounded icon style
- `design` / `app` module split: UI lives in the `design` module as Kotlin `Design` classes (View-based), business logic in `app`
- **Activity-per-page** architecture
- Proxy page reworked: selected item uses a soft brand-tinted background (15% brand × surface), tab selected text uses `colorOnSurface`, and the top bar is full-width opaque (no more color band / side transparency)
- Light & dark theme support

## Modules

| Module | Role |
|---|---|
| `app` | Entry, Activities, service binding |
| `design` | UI layer (Design classes, layouts, theme) |
| `core` | Clash.Meta core (Go) build & bridge |
| `service` | Foreground proxy service |
| `common` | Shared utilities |
| `hideapi` | Hidden API compatibility |

Application ID: `com.flowly.net`.

## Build

Requirements: **JDK 17+** (tested on 21), Android SDK, CMake, Golang. No git submodule needed (core builds with the project).

```bash
# configure SDK path in local.properties
echo "sdk.dir=/path/to/android-sdk" > local.properties

# debug
./gradlew assembleMetaDebug

# release
./gradlew assembleMetaRelease
```

APKs are in `app/build/outputs/apk/meta/<buildType>/` for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, and `universal`.

## Requirements

- Android 5.0+ (min), 7.0+ (recommended)
- ABI: `armeabi-v7a` / `arm64-v8a` / `x86` / `x86_64`

## License

GPL-3.0 (inherited from ClashMetaForAndroid / Clash.Meta).

## Author

SkyAlice — [GitHub @SkyAlice-source](https://github.com/SkyAlice-source)
