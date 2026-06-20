# Changelog — your-pax

## v1.1.1-alpha (Latest)

### App Control Features

| Feature | Description |
|---------|-------------|
| **Mode Selector** | Settings dropdown to switch `app_only` / `web_only` / `web_app` at runtime. Backed by `switch_mode` API + `switch_mode.sh`. |
| **Service Control** | Start/Stop Web/NAP/SPP buttons in Android app. Calls `/start_*_service` / `/stop_*_service` endpoints. |
| **Mode Indicator** | HomeScreen status bar badge shows current mode (APP/WEB/BOTH). |
| **SSE Live Events** | `EventBus` collector → real-time `LazyColumn` on HomeScreen. Shows scan progress, attack updates, system health. |
| **Health Monitor** | Backend broadcasts `system_status` every 60s via SSE. |

### Security Hardening

| Fix | Details |
|-----|---------|
| **RateLimiter** | Token-bucket per-IP, 30 req/min, blocks burst attacks on API endpoints. |
| **CSRF Rotation** | Token auto-rotates every 3600s via daemon thread. |
| **HTTPS** | Self-signed cert via openssl, toggleable via `use_https`/`https_port` config. |
| **Path traversal blocked** | `clear_files` validates pattern (blocks `..`, `~`, `/root`, `/etc`, etc.). `serve_file` validates via `os.path.commonpath`. |

### Bug Fixes (13 from final audit)

| # | File | Bug |
|---|------|-----|
| 1 | `bluetooth_nap.py` | Missing `shared_data` param + b_action/b_target globals |
| 2 | `conflict_manager.py` | Missing all b_* globals |
| 3 | `scanning.py` | No `execute()` method on NetworkScanner |
| 4 | `oneshot.py` | `b_class="OneShot"` should be `"Companion"`; undefined `args` at line 837 |
| 5 | `sql_connector.py` | `connect()` returns `(False, [])` — always truthy tuple |
| 6 | `steal_files_smb.py` | `not try_anonymous_access()` on tuple always False |
| 7 | `switch_mode.sh` | `__YOUR_PAX_DIR__` placeholder never resolved |
| 8 | `evil_ap.html` | Single-quote breakout XSS in onclick |
| 9 | `wifi.html` | Orphaned dead code breaking JS execution |
| 10 | `utils.py` | Path traversal in `serve_file` |
| 11 | `utils.py` | Self-import `from utils import stop_evil_ap` |
| 12 | `init_shared.py` | Singleton race condition (no lock) |
| 13 | `base_steal.py` | Missing b_* globals |

### New Scripts

- `scripts/setup_services.sh` — Replaces `__YOUR_PAX_DIR__` in systemd service files with actual install path.
- `scripts/update-your-pax-main.sh` — Linux update: git pull + reinstall + restart.
- `scripts/update-your-pax-main.bat` — Windows dev helper: compile check + test hints.

---

## v1.1-alpha (Previous)

### What's New

### your-pax-android (Complete Android App)

A full native Android app built with **Kotlin + Jetpack Compose** for controlling your-pax via Bluetooth SPP (RFCOMM serial).

**19 Screens:**
- `SplashScreen` — Bluetooth device discovery, SPP (RFCOMM) connection, CSRF token fetch
- `HomeScreen` — Live status (BT NAP, WiFi), console log, stats, quick actions
- `NetworkScreen` — Scan results table with colored cells, auto-refresh, Quick Actions
- `NetworkDetailScreen` — Per-host detail: IP, MAC, OS, ports, scan/brute/vuln/manual attacks
- `WiFiScreen` — Network scanner, monitor mode toggle, conflict bar, target list
- `WiFiTargetAttackDialog` — Handshake/Deauth/PMKID/WPS attack controls with live polling
- `EvilAPScreen` — Full Evil AP control: setup, WPA validate, Karma, Clone AP, live monitor
- `WiFiConnectScreen` — WiFi status + scan + manual connect
- `LootScreen` — Credentials tab + Stolen Files browser with download
- `StoreScreen` — 10 stat cards + handshakes/PMKID/creds/WPS/results/vulns/zombies + portal upload/delete
- `NetKBScreen` — Full NetKB table with colored rows, auto-refresh
- `ConfigScreen` — Dynamic config form from JSON, all sections, save/restore defaults
- `BackupRestoreScreen` — Create backup + restore from file picker
- `ManualModeScreen` — Targeted attack (IP/Port/Action) + Execute Command + Quick Commands
- `EPDScreen` — Live e-paper display with zoom/rotation controls
- `LogScreen` — Console log viewer
- `MoreScreen` — Settings, Power actions, About
- `LogScreen` — Full console log viewer

**Key Features:**
- MVVM architecture with Repository pattern
- Dynamic CSRF token management (fetched on BT connect, auto-injected in requests)
- 20+ confirmation dialogs for destructive actions
- Protocol selector for bruteforce (SSH/FTP/Telnet/SMB/SQL/RDP/All)
- Auto-polling for attack status (handshake, PMKID, WPS)
- Copy-to-clipboard for captured passwords
- Font size controls on loot/log screens
- Dark theme with modern Material 3 design (not cyberpunk)
- ~130 Android features vs ~120 web features — Android has MORE

### API Fixes (Android ↔ Firmware Parity)

All critical Android ↔ firmware API mismatches fixed so the app actually communicates correctly:

| Fix | File(s) | What Changed |
|-----|---------|--------------|
| **CSRF Token** | `CsrfTokenManager.kt`, `CsrfResponse.kt`, `RetrofitProvider.kt`, `SplashScreen.kt` | Token fetched dynamically after BT connect; `OkHttp` interceptor adds `Authorization: Bearer <token>` + `X-CSRF-Token: <token>` at request time |
| **Bruteforce JSON Body** | `YourPaxApiService.kt`, `NetworkRepository.kt` | `@FormUrlEncoded @Field("protocol")` → `@Body Map<String, String>` |
| **BluetoothDeviceInfo** | `BluetoothStatusData.kt`, `BluetoothRepository.kt`, `DemoData.kt` | `@SerializedName("address")` → `@SerializedName("mac")`, added `ip` field, `BluetoothDevicesResponse` wrapper |
| **StoreFileItem.size** | `StoreModels.kt`, `DemoData.kt` | `size: String` → `size: Long` |
| **ActionResponse verification** | All trigger/attack endpoints | All 32 POST endpoint response formats verified against actual firmware JSON output |

### Documentation Files Added

- `update2.txt` — Complete firmware reference: API endpoints, actions, config, orchestrator cycle, Android feature map
- `update3.txt` — Detailed Android vs Web Frontend 100% parity check (120+ features compared)

## Project Structure

```
your-pax/
├── your-pax.py              # Main entry point
├── orchestrator.py          # Attack orchestrator
├── webapp.py                # Web server (port 8000)
├── your-pax-android/        # [NEW] Android app (Kotlin + Compose)
│   ├── app/src/main/java/com/yourpax/app/
│   │   ├── data/api/        # Retrofit, API service, models
│   │   ├── data/repository/ # Network, WiFi, Loot, Bluetooth, Config repos
│   │   ├── data/bluetooth/  # BT discovery + SPP connection
│   │   └── ui/              # 19 screens, components, theme, navigation
│   └── build.gradle.kts
├── config/                  # shared_config.json, actions.json
├── actions/                 # 30+ attack modules
├── web/                     # Web UI HTML/CSS/JS
├── data/                    # Network KB, logs, loot, credentials
└── resources/               # Fonts, images, comments
```

## Android App Feature Parity

| Category | Web UI | Android App |
|----------|--------|-------------|
| Bluetooth Connect | N/A | ✓ SplashScreen |
| Dashboard + Stats | ✓ index.html | ✓ HomeScreen |
| Quick Actions | ✓ (in index) | ✓ NetworkScreen + HomeScreen |
| Network Scan | ✓ network.html | ✓ NetworkScreen |
| NetKB | ✓ netkb.html | ✓ NetKBScreen |
| Credentials | ✓ credentials.html | ✓ LootScreen (tab) |
| Stolen Files | ✓ loot.html | ✓ LootScreen (tab) |
| Store | ✓ store.html | ✓ StoreScreen |
| WiFi Scan + Attacks | ✓ wifi.html + wifi_target.html | ✓ WiFiScreen + WiFiTargetAttackDialog |
| Evil AP Full Control | ✓ evil_ap.html | ✓ EvilAPScreen |
| WiFi Connect | ✓ bt_connect.html | ✓ WiFiConnectScreen |
| Config Editor | ✓ config.html | ✓ ConfigScreen |
| Console Logs | ✓ /get_logs | ✓ LogScreen |
| Live EPD | ✓ your-pax.html | ✓ EPDScreen |
| Backup/Restore | ✓ (in index dropdown) | ✓ BackupRestoreScreen |
| Manual Mode | ✗ (no page) | ✓ ManualModeScreen |
| Network Detail | ✗ (no page) | ✓ NetworkDetailScreen |
| Confirmation Dialogs | ✗ (none) | ✓ 20+ dialogs |
| More / Settings | ✓ (in index dropdown) | ✓ MoreScreen |
