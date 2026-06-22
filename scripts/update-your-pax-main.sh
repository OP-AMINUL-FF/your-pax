#!/bin/bash
# update-your-pax-main.sh — Pull latest changes and reinstall services
# Usage: sudo ./scripts/update-your-pax-main.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAX_DIR="$(dirname "$SCRIPT_DIR")"

echo "[*] Updating your-pax-main in $PAX_DIR"

if [ -d "$PAX_DIR/.git" ]; then
    echo "[*] Pulling latest changes..."
    cd "$PAX_DIR"
    git pull
else
    echo "[!] Not a git repository. Skipping pull."
fi

echo "[*] Reinstalling systemd services..."
sudo bash "$SCRIPT_DIR/setup_services.sh" "$PAX_DIR"

echo "[*] Restarting services..."
sudo systemctl restart your-pax-orchestrator 2>/dev/null || true
sudo systemctl restart your-pax-web 2>/dev/null || true
sudo systemctl restart your-pax-nap 2>/dev/null || true
sudo systemctl restart your-pax-spp 2>/dev/null || true

echo "[✓] Update complete. Run 'systemctl status your-pax-*' to verify."

