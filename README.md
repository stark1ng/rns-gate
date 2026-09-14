# RNS Gate

**EN** · Simple Android gateway for [Reticulum](https://reticulum.network/) with VPN-like UX.  
**RU** · Простой Android-шлюз для Reticulum с UX как у VPN.

Differentiates from Sideband by focusing on a minimal Connect → Chat → Tools flow.

## Status

MVP **0.1.0** with a **demo backend** (`DemoRnsNode`, `DemoLxmfMessenger`).  
Real Reticulum via Chaquopy is stubbed with TODOs — see `ARCHITECTURE.md`.

## Features

| Screen   | What you get |
|----------|----------------|
| **Gate** | Big Connect/Disconnect, step infographic (Identity → Interfaces → Path/Announce → Ready), TCP/Auto chips, RNode placeholder, hash + uptime when online |
| **Chat** | LXMF-style conversation list + thread, demo send/auto-reply |
| **Tools**| Show/regenerate identity, peers, interface status, TCP host:port (DataStore) |
| **Settings** | Display name, about, language note (EN/RU via system locale) |

## Tech

- Kotlin · Jetpack Compose · Material 3 (dark theme default)
- Min SDK 26 · Compile/Target SDK 35
- Gradle Kotlin DSL · AGP 8.7.3 · Kotlin 2.0.21 · JDK 17
- Package: `com.rnsgate.app`

## Build (Android Studio — recommended)

1. Open `/workspace/rns-gate` (or clone) in **Android Studio Ladybug+** / recent stable.
2. Let Gradle sync (needs network once for dependencies).
3. Run on emulator or device (API 26+).

```bash
./gradlew :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Build notes

- Requires **JDK 17+** (JDK 21 works) and Android SDK with **platform 35** + build-tools.
- `local.properties` (`sdk.dir=...`) is gitignored; Android Studio creates it automatically.
- On this Linux box, `./gradlew :app:assembleDebug` was verified successfully after installing cmdline-tools + `platforms;android-35`.

## Demo usage

1. Open **Gate** → tap **Connect** — watch the step animation until **Online**.
2. Open **Chat** → **New chat** → send a message — demo peer replies.
3. **Tools** — inspect identity/peers, save TCP endpoint.
4. **Settings** — set display name.

## License

MIT — see [LICENSE](LICENSE).

## Language / Язык

App strings ship in **English** (`values`) and **Russian** (`values-ru`).  
The UI follows the system language.
