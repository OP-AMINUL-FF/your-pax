#!/bin/bash
# switch_mode.sh — Switch your-pax connection mode at runtime
# Usage: sudo ./scripts/switch_mode.sh [web_only|app_only|web_app]

set -e

MODE=$1
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAX_DIR="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PAX_DIR/config/shared_config.json"

if [ -z "$MODE" ]; then
    echo "Usage: $0 {web_only|app_only|web_app}"
    echo ""
    echo "  web_only  — Web Server + NAP  (Android uses HTTP, no SPP)"
    echo "  app_only  — SPP only          (no web, no NAP, app uses Bluetooth Serial)"
    echo "  web_app   — Web + NAP + SPP   (Android→SPP, Browser→HTTP)"
    exit 1
fi

case "$MODE" in
    web_only|app_only|web_app)
        ;;
    *)
        echo "Invalid mode: $MODE"
        echo "Valid: web_only, app_only, web_app"
        exit 1
        ;;
esac

echo "[*] Switching to mode: $MODE"

# Update config.json
if [ -f "$CONFIG_FILE" ]; then
    python3 -c "
import json
with open('$CONFIG_FILE', 'r') as f:
    cfg = json.load(f)
cfg['connection_mode'] = '$MODE'
with open('$CONFIG_FILE', 'w') as f:
    json.dump(cfg, f, indent=4)
"
fi

# Apply service changes per mode
case "$MODE" in
    web_only)
        echo "[*] Enabling: your-pax-web, your-pax-nap"
        echo "[*] Disabling: your-pax-spp"
        systemctl enable  your-pax-web
        systemctl enable  your-pax-nap
        systemctl disable your-pax-spp
        systemctl start   your-pax-web  2>/dev/null || true
        systemctl start   your-pax-nap  2>/dev/null || true
        systemctl stop    your-pax-spp  2>/dev/null || true
        ;;
    app_only)
        echo "[*] Enabling: your-pax-spp"
        echo "[*] Disabling: your-pax-web, your-pax-nap"
        systemctl enable  your-pax-spp
        systemctl disable your-pax-web
        systemctl disable your-pax-nap
        systemctl start   your-pax-spp  2>/dev/null || true
        systemctl stop    your-pax-web  2>/dev/null || true
        systemctl stop    your-pax-nap  2>/dev/null || true
        echo "[*] Note: Web UI files can be removed to save space:"
        echo "    rm -rf $PAX_DIR/web/*"
        ;;
    web_app)
        echo "[*] Enabling: your-pax-web, your-pax-nap, your-pax-spp"
        systemctl enable your-pax-web
        systemctl enable your-pax-nap
        systemctl enable your-pax-spp
        systemctl start  your-pax-web  2>/dev/null || true
        systemctl start  your-pax-nap  2>/dev/null || true
        systemctl start  your-pax-spp  2>/dev/null || true
        ;;
esac

echo "[✓] Mode switched to: $MODE"
systemctl status your-pax-web --no-pager 2>/dev/null | head -3
systemctl status your-pax-nap --no-pager 2>/dev/null | head -3
systemctl status your-pax-spp --no-pager 2>/dev/null | head -3
