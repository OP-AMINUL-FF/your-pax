#!/bin/bash
# generate_hashes.sh — Generate SHA256 hash manifest for release artifacts
# Usage: ./scripts/generate_hashes.sh [output_file]
#   If no output file given, prints to stdout.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAX_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PAX_DIR"

OUTPUT="${1:-/dev/stdout}"

echo "============================================" > "$OUTPUT"
echo " your-pax v1.1.1-alpha — SHA256 Hash Manifest" >> "$OUTPUT"
echo " Generated: $(date -u '+%Y-%m-%dT%H:%M:%SZ')" >> "$OUTPUT"
echo "============================================" >> "$OUTPUT"
echo "" >> "$OUTPUT"

# Core Python files
echo "--- Core Python ---" >> "$OUTPUT"
for f in your-pax.py webapp.py orchestrator.py shared.py init_shared.py utils.py logger.py event_bus.py; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        echo "  $hash  $f  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"

# Action modules
echo "--- Action Modules ---" >> "$OUTPUT"
for f in actions/*.py; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        name=$(basename "$f")
        echo "  $hash  actions/$name  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"

# Web UI
echo "--- Web UI ---" >> "$OUTPUT"
for f in web/*.html web/scripts/*.js; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        name=$(echo "$f" | sed 's|^\./||')
        echo "  $hash  $name  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"

# Scripts
echo "--- Scripts ---" >> "$OUTPUT"
for f in scripts/*.sh; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        name=$(basename "$f")
        echo "  $hash  scripts/$name  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"

# Systemd
echo "--- Systemd Services ---" >> "$OUTPUT"
for f in systemd/*.service; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        name=$(basename "$f")
        echo "  $hash  systemd/$name  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"

# Android app files (Kotlin source + build config)
echo "--- Android App Source ---" >> "$OUTPUT"
find your-pax-android -name "*.kt" -o -name "*.kts" -o -name "*.xml" 2>/dev/null | sort | while read -r f; do
    if [ -f "$f" ]; then
        hash=$(sha256sum "$f" | cut -d' ' -f1)
        size=$(wc -c < "$f")
        echo "  $hash  $f  (${size} bytes)" >> "$OUTPUT"
    fi
done

echo "" >> "$OUTPUT"
echo "============================================" >> "$OUTPUT"
echo " End of Manifest" >> "$OUTPUT"
echo "============================================" >> "$OUTPUT"

chmod +x "$OUTPUT" 2>/dev/null || true
echo "[*] Hash manifest written to: $OUTPUT"

