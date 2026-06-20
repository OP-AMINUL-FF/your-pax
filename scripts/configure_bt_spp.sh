#!/bin/bash
# configure_bt_spp.sh — Enable Bluetooth Serial Port Profile (SPP) on your-pax
# Called during install (app_only / web_app modes) and on mode switches.

set -e

echo "[*] Configuring Bluetooth SPP profile..."

# 1. Add Serial Port profile via sdptool
echo "[*] Registering SPP service..."
sdptool add SP || echo "[!] sdptool add SP failed (may already exist)"

# 2. Ensure bluetoothd runs in compatibility mode (-C flag)
BLUEZ_SERVICE="/etc/systemd/system/dbus-org.bluez.service"
if [ -f "$BLUEZ_SERVICE" ]; then
    if ! grep -q -- "-C" "$BLUEZ_SERVICE"; then
        echo "[*] Adding -C flag to bluetoothd..."
        sed -i 's/ExecStart=\/usr\/libexec\/bluetooth\/bluetoothd/\
                ExecStart=\/usr\/libexec\/bluetooth\/bluetoothd -C/' "$BLUEZ_SERVICE"
    fi
else
    # Fallback: edit the original bluez service file
    ORIG="/lib/systemd/system/bluetooth.service"
    if [ -f "$ORIG" ]; then
        echo "[*] Patching $ORIG for compatibility mode..."
        sed -i 's/ExecStart=\/usr\/libexec\/bluetooth\/bluetoothd/\
                ExecStart=\/usr\/libexec\/bluetooth\/bluetoothd -C/' "$ORIG"
        systemctl daemon-reload
    fi
fi

# 3. Make Bluetooth discoverable and pairable
echo "[*] Setting Bluetooth discoverable & pairable..."
bluetoothctl discoverable on
bluetoothctl pairable on

# 4. Configure persistent settings in main.conf
BT_CONF="/etc/bluetooth/main.conf"
if [ -f "$BT_CONF" ]; then
    sed -i 's/^#DiscoverableTimeout=0/DiscoverableTimeout=0/' "$BT_CONF"
    sed -i 's/^#PairableTimeout=0/PairableTimeout=0/' "$BT_CONF"
    sed -i 's/^#Discoverable=true/Discoverable=true/' "$BT_CONF"
fi

# 5. Restart Bluetooth stack
echo "[*] Restarting Bluetooth..."
systemctl daemon-reload
systemctl restart bluetooth
sleep 2

# 6. Verify SPP profile is registered
echo "[*] Verifying SPP profile..."
sdptool browse local | grep -A 10 "Serial Port" || {
    echo "[!] SPP profile not found, retrying..."
    sdptool add SP
    sleep 1
    sdptool browse local | grep -A 10 "Serial Port" || echo "[!] SPP may still be unavailable"
}

echo "[✓] Bluetooth SPP configuration complete"
