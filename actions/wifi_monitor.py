import subprocess
import logging
import time

from logger import Logger
logger = Logger(name="wifi_monitor.py", level=logging.DEBUG)
b_class = "WiFiMonitor"
b_module = "wifi_monitor"
b_status = "wifi_monitor"
b_port = 0
b_parent = None


class WiFiMonitor:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.interface = shared_data.config.get("wifi_interface", "wlan0")
        self.mon_iface = shared_data.config.get("monitor_interface", "wlan0mon")
        self.monitor_active = False

    def start_monitor(self):
        try:
            if self.shared_data.config.get("evil_ap_running", False):
                evil = self.shared_data.evil_ap_instance
                if evil:
                    evil.stop()
                self.shared_data.config["evil_ap_running"] = False
                logger.info("Stopped Evil AP before enabling monitor mode")
            if self.shared_data.oneshot_instance and self.shared_data.oneshot_instance.running:
                logger.error("Cannot enable monitor mode while WPS attack is running")
                return False
            subprocess.run(['sudo', 'ip', 'link', 'set', self.interface, 'down'], check=True)
            time.sleep(0.5)
            subprocess.run(['sudo', 'iw', 'dev', self.interface, 'set', 'type', 'monitor'], check=True)
            time.sleep(0.5)
            subprocess.run(['sudo', 'ip', 'link', 'set', self.interface, 'up'], check=True)
            time.sleep(0.5)
            subprocess.run(['sudo', 'iw', 'dev', self.interface, 'interface', 'add', self.mon_iface, 'type', 'monitor'], capture_output=True)
            self.monitor_active = True
            self.shared_data.config["enable_monitor_mode"] = True
            logger.info(f"Monitor mode enabled on {self.interface} via {self.mon_iface}")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to enable monitor mode: {e}")
            return False

    def stop_monitor(self):
        try:
            subprocess.run(['sudo', 'iw', 'dev', self.mon_iface, 'del'], capture_output=True)
            subprocess.run(['sudo', 'ip', 'link', 'set', self.interface, 'down'], check=True)
            time.sleep(0.5)
            subprocess.run(['sudo', 'iw', 'dev', self.interface, 'set', 'type', 'managed'], check=True)
            time.sleep(0.5)
            subprocess.run(['sudo', 'ip', 'link', 'set', self.interface, 'up'], check=True)
            self.monitor_active = False
            self.shared_data.config["enable_monitor_mode"] = False
            logger.info(f"Monitor mode disabled on {self.interface}")
            return True
        except subprocess.CalledProcessError as e:
            logger.error(f"Failed to disable monitor mode: {e}")
            return False

    def check_monitor(self):
        try:
            result = subprocess.run(['iwconfig', self.interface], capture_output=True, text=True, timeout=5)
            return 'Mode:Monitor' in result.stdout
        except Exception as e:
            logger.error(f"Error checking monitor mode: {e}")
            return False

    def execute(self, ip="", port="", row=None, status_key=""):
        action = self.shared_data.config.get("enable_monitor_mode", False)
        if action and not self.monitor_active:
            return 'success' if self.start_monitor() else 'failed'
        elif not action and self.monitor_active:
            return 'success' if self.stop_monitor() else 'failed'
        return 'success'
