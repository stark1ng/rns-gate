# RNS Gate

**EN** · Simple Android gateway for [Reticulum](https://reticulum.network/) with VPN-like UX.  
**RU** · Простой Android-шлюз для Reticulum с UX как у VPN.

Differentiates from Sideband by focusing on a minimal Connect → Chat → Tools flow.

## Status

**0.2.0** — real Reticulum path via **Chaquopy** + **rnspure** (same code as `rns`, pure-Python crypto fallback).  
If Python/RNS fails to initialize, Connect falls back to **Demo** and shows `demo fallback: …` on the Gate screen.

| Layer | Real | Still demo |
|-------|------|------------|
| Gate Connect / Disconnect | `ChaquopyRnsNode` → `rns_bridge.py` → RNS | Fallback `DemoRnsNode` |
| Identity (app-private files) | Yes | Demo random hash |
| TCP Client interface | From Tools/Settings host:port | Simulated |
| LXMF Chat | — | `DemoLxmfMessenger` (banner on Chat) |
| RNode / Auto interfaces | Placeholder / disabled on mobile | Simulated Auto |

## Features

| Screen   | What you get |
|----------|----------------|
| **Gate** | Connect/Disconnect, step infographic, TCP chip, identity hash + uptime, RNS vs Demo badge |
| **Chat** | LXMF-style UI (demo send/auto-reply until LXMF is wired) |
| **Tools**| Identity show/regenerate, peers/paths, TCP host:port (DataStore) |
| **Settings** | Display name, about, language note (EN/RU via system locale) |

## Connect to a real TCP Reticulum interface

1. Run an `rnsd` (or any Reticulum node) with a **TCP Server** interface on a host your phone can reach, e.g.:

   ```
   [[TCP Server]]
     type = TCPServerInterface
     enabled = yes
     listen_ip = 0.0.0.0
     listen_port = 4242
   ```

2. In the app **Tools** (or ensure DataStore defaults), set **TCP host** to that machine’s LAN/VPN IP (not `127.0.0.1` unless using adb reverse) and **TCP port** (default `4242`).
3. Open **Gate** → **Connect**. A foreground notification **“RNS Gate connected”** stays while online.
4. Identity material is stored under the app’s private files dir (`files/rns/`), not shared.

For emulator testing against a host daemon:

```bash
adb reverse tcp:4242 tcp:4242
```

Then TCP host `127.0.0.1` works inside the emulator.

## Permissions

- `INTERNET` / `ACCESS_NETWORK_STATE` — TCP to your rnsd
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — keep node alive while connected
- `POST_NOTIFICATIONS` — Android 13+ notification for the foreground service

## Tech

- Kotlin · Jetpack Compose · Material 3 (dark theme default)
- Min SDK 26 · Compile/Target SDK 35
- Chaquopy **17.0.0** · Python **3.13** (pinned to the build machine; 3.11 also supported by Chaquopy 17)
- Pip: `rnspure` only (`rns`/`lxmf` PyPI wheels ship console-script RECORD paths Chaquopy 17 rejects; Chat still demo)
- Package: `com.rnsgate.app`

## Build

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Requires **JDK 17+**, Android SDK **platform 35**, and a matching **buildPython** (here `/usr/bin/python3.13`).

## Demo fallback

If Chaquopy or `import RNS` fails at startup, `RnsGateApp` wires `DemoRnsNode` and Gate shows a status line like `demo fallback: …`. Chat remains demo in all cases for this release.

## License

MIT — see [LICENSE](LICENSE).

## Language / Язык

App strings ship in **English** (`values`) and **Russian** (`values-ru`).  
The UI follows the system language.
