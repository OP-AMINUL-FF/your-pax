import subprocess
import os
import logging
import time
import signal

from logger import Logger
from event_bus import broadcast_event
logger = Logger(name="wifi_handshake.py", level=logging.DEBUG)
b_class = "WiFiHandshake"
b_module = "wifi_handshake"
b_status = "wifi_handshake"
b_port = 0
b_parent = None


class WiFiHandshake:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.interface = shared_data.config.get("monitor_interface", "wlan0mon")
        self.capture_process = None
        self.handshake_dir = os.path.join(shared_data.datadir, "handshakes")

    def start(self, bssid, channel, prefix=None):
        return self.start_capture(bssid, channel, prefix)

    def start_capture(self, bssid, channel, output_file=None):
        try:
            import re
            if not re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
            channel_int = int(channel)
            if channel_int < 1 or channel_int > 165:
                raise ValueError("Channel must be between 1 and 165")
            os.makedirs(self.handshake_dir, exist_ok=True)
            if not output_file:
                output_file = os.path.join(self.handshake_dir, f"capture_{bssid.replace(':', '')}_{int(time.time())}")
            subprocess.run(['sudo', 'iwconfig', self.interface, 'channel', str(channel)],
                           capture_output=True, timeout=5, check=False)
            cmd = [
                'sudo', 'airodump-ng', '-c', str(channel),
                '--bssid', bssid,
                '-w', output_file,
                self.interface
            ]
            self.capture_process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            logger.info(f"Handshake capture started for {bssid} on channel {channel}")
            broadcast_event("wifi_handshake_captured", {
                "bssid": bssid,
                "channel": channel,
                "output_file": output_file,
                "status": "capturing"
            })
            return output_file
        except Exception as e:
            logger.error(f"Failed to start capture: {e}")
            return None

    def stop(self):
        return self.stop_capture()

    def stop_capture(self):
        if self.capture_process:
            self.capture_process.send_signal(signal.SIGINT)
            try:
                self.capture_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.capture_process.kill()
            self.capture_process = None
            logger.info("Handshake capture stopped")
            broadcast_event("wifi_handshake_captured", {
                "status": "stopped",
                "message": "Handshake capture stopped"
            })
            return True
        return False

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
