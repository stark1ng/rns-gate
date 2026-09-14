# Architecture — RNS Gate

## Goals

- VPN-like **Gate** UX: one Connect button + clear connect pipeline.
- Thin ports so Demo and real Reticulum share the same UI.
- Prefer real RNS via Chaquopy; keep Demo as automatic fallback.

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
        ├─► ChaquopyRnsNode  (preferred)
        │     └─ Python rns_bridge.py → rnspure (RNS)
        │     └─ RnsNodeService (foreground while Online)
        │
        └─► DemoRnsNode / DemoLxmfMessenger  (fallback / chat)
```

## Startup selection (`RnsGateApp`)

1. Construct `ChaquopyRnsNode` and call `initializePython()` (starts Chaquopy, `init_storage`, `ping`).
2. On success → `rnsNode = ChaquopyRnsNode`.
3. On failure → wrap `DemoRnsNode` and set `statusMessage = "demo fallback: …"`.

## Connect state machine

`Offline` → `Connecting` with steps:

1. **Identity** — load/create identity under `files/rns/identity`
2. **Interfaces** — write `files/rns/reticulum/config` with `TCPClientInterface` from Settings; start `RNS.Reticulum`
3. **Path / Announce** — destination announce (`rnsgate` / `node`)
4. **Ready** → `Online` (status poll, foreground service)

`Disconnect` detaches interfaces (does **not** call `RNS.exit()` / `os._exit`), stops the service, resets Gate UI. Re-connect in the same process resets the Reticulum singleton best-effort.

## Python bridge (`app/src/main/python/rns_bridge.py`)

| Call | Role |
|------|------|
| `init_storage(dir)` | App-private config + identity paths |
| `start(host, port, name)` | Write TCP config, start RNS, announce |
| `stop()` | `Transport.detach_interfaces` + clear singleton |
| `status()` | Hash, TCP up, path-table estimate, uptime |
| `regenerate_identity()` | New identity file (while offline) |
| `probe_announce()` | Extra announce |
| `ping()` | Import / version check |

## Package layout

```
com.rnsgate.app
├── MainActivity, RnsGateApp
├── service/RnsNodeService
├── data/
│   ├── RnsNode.kt, LxmfMessenger.kt, SettingsStore.kt
│   ├── model/Models.kt
│   ├── chaquopy/ChaquopyRnsNode.kt
│   └── demo/DemoRnsNode.kt, DemoLxmfMessenger.kt
└── ui/ …
app/src/main/python/rns_bridge.py
```

## Chaquopy notes

- Plugin **17.0.0**, Python **3.13**, ABIs `arm64-v8a` + `x86_64`.
- Pip installs **`rnspure`** (pure-Python wheel; contents match `rns`). PyPI `lxmf`/`rns` are not installed yet (Chaquopy RECORD issue with `../../bin/…` scripts).
- Identity stays in app-private storage; no hardcoded secrets.

## LXMF

Not wired yet. Chat UI shows an honest demo banner. Future: vendor `lxmf` with `--no-deps` on top of `rnspure`, or wait for cleaner wheels.

## Navigation

Bottom bar: Gate · Chat · Tools · Settings.

## i18n

- `res/values/strings.xml` — English  
- `res/values-ru/strings.xml` — Russian  

No in-app language switcher; system locale applies.
