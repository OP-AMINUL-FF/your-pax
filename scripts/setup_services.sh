#!/bin/bash
# setup_services.sh — Replace __YOUR_PAX_DIR__ placeholder in systemd service files
# Usage: sudo ./scripts/setup_services.sh [/path/to/install/dir]
#   If no path given, uses parent directory of this script's location.

set -e

if [ -n "$1" ]; then
    PAX_DIR="$1"
else
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    PAX_DIR="$(dirname "$SCRIPT_DIR")"
fi

if [ ! -d "$PAX_DIR" ]; then
    echo "Error: Directory $PAX_DIR does not exist."
    exit 1
fi

echo "[*] Installing systemd services with PAX_DIR=$PAX_DIR"

SYSTEMD_DIR="$PAX_DIR/systemd"
for svc in "$SYSTEMD_DIR"/*.service; do
    [ -f "$svc" ] || continue
    NAME=$(basename "$svc")
    TARGET="/etc/systemd/system/$NAME"
    echo "[*] Installing $NAME"
    sed "s|__YOUR_PAX_DIR__|$PAX_DIR|g" "$svc" | sudo tee "$TARGET" > /dev/null
done

echo "[*] Reloading systemd daemon..."
sudo systemctl daemon-reload

echo "[*] Enabling services..."
for svc in "$SYSTEMD_DIR"/*.service; do
    [ -f "$svc" ] || continue
    NAME=$(basename "$svc" .service)
    sudo systemctl enable "$NAME" 2>/dev/null || true
done

echo "[✓] Services installed. Use: sudo systemctl start your-pax-*"

