# Architecture — RNS Gate

## Goals

- VPN-like **Gate** UX: one Connect button + clear connect pipeline.
- Thin abstractions so the demo backend can be swapped for real Reticulum.
- Keep the MVP simple vs. full-featured clients like Sideband.

## Layers

```
UI (Compose screens + ViewModels)
        │
        ▼
Domain ports
  RnsNode          — connection, identity, interfaces, peers
  LxmfMessenger    — conversations + send/receive
  SettingsStore    — DataStore prefs (display name, TCP endpoint)
        │
        ▼
Demo implementations (MVP)
  DemoRnsNode      — simulated state machine
  DemoLxmfMessenger— in-memory chat + auto-reply
        │
        ▼  (future)
Chaquopy + Python `rns` / LXMF
```

## Connect state machine (demo)

`Offline` → `Connecting` with steps:

1. **Identity** — load/create identity hash  
2. **Interfaces** — bring up TCP + Auto (RNode remains placeholder)  
3. **Path / Announce** — simulate announce  
4. **Ready** → `Online` (uptime ticker, demo peers)

`Disconnect` resets to `Offline`.

## Package layout

```
com.rnsgate.app
├── MainActivity, RnsGateApp
├── data/
│   ├── RnsNode.kt, LxmfMessenger.kt, SettingsStore.kt
│   ├── model/Models.kt
│   └── demo/DemoRnsNode.kt, DemoLxmfMessenger.kt
└── ui/
    ├── theme/
    ├── navigation/RnsGateRoot.kt
    ├── gate/, chat/, tools/, settings/
```

## Future: real RNS (Chaquopy)

Marked in code as `TODO(real-rns)`:

1. Add Chaquopy Gradle plugin; ship `rns` (+ LXMF) Python deps.  
2. Implement `ChaquopyRnsNode` starting `RNS.Reticulum` on a background thread.  
3. Map RNS Identity / Interfaces / Transport announces into `GateSnapshot`.  
4. Implement `ChaquopyLxmfMessenger` for real destinations and delivery receipts.  
5. Persist identity material securely (Android Keystore / app-private files).  
6. Foreground service for long-lived node while “connected”.

## Navigation

Bottom bar: Gate · Chat · Tools · Settings (`NavigationBar` + single `NavHost`).

## i18n

- `res/values/strings.xml` — English  
- `res/values-ru/strings.xml` — Russian  

No in-app language switcher yet; system locale applies.
