import subprocess
import os
import logging
import threading
import json
import signal

from logger import Logger
logger = Logger(name="wifi_oneshot.py", level=logging.DEBUG)
b_class = "WiFiOneShot"
b_module = "wifi_oneshot"
b_status = "wifi_oneshot"
b_port = 0
b_parent = None
YOUR_PAX_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ONESHOT_LOCAL = os.path.join(YOUR_PAX_ROOT, 'actions', 'oneshot.py')
ONESHOT_ALT = os.path.join(YOUR_PAX_ROOT, '..', 'OneShot-master', 'oneshot.py')
VULN_LIST_LOCAL = os.path.join(YOUR_PAX_ROOT, 'actions', 'vulnwsc.txt')
VULN_LIST_ALT = os.path.join(YOUR_PAX_ROOT, '..', 'OneShot-master', 'vulnwsc.txt')

class WiFiOneShot:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.interface = shared_data.config.get("wifi_interface", "wlan0")
        self.process = None
        self.output = []
        self._output_lock = threading.Lock()
        self.running = False
        self.status_file = os.path.join(shared_data.datadir, 'oneshot_status.json')
        cfg_path = shared_data.config.get("oneshot_path", "")
        cfg_vuln = shared_data.config.get("oneshot_vuln_list", "")
        if cfg_path and os.path.exists(cfg_path):
            self.oneshot_path = cfg_path
            self.vuln_list = cfg_vuln if (cfg_vuln and os.path.exists(cfg_vuln)) else VULN_LIST_LOCAL
        elif os.path.exists(ONESHOT_LOCAL):
            self.oneshot_path = ONESHOT_LOCAL
            self.vuln_list = VULN_LIST_LOCAL
        elif os.path.exists(ONESHOT_ALT):
            self.oneshot_path = ONESHOT_ALT
            self.vuln_list = VULN_LIST_ALT
        else:
            self.oneshot_path = ONESHOT_LOCAL
            self.vuln_list = VULN_LIST_LOCAL

    def build_cmd(self, bssid=None, pixie=False, bruteforce=False, pbc=False,
                  pin=None, delay=None, pixie_force=False, show_pixie_cmd=False,
                  verbose=False, iface_down=True, vuln_list=None):
        if bssid is not None:
            import re
            if not re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
        if pin is not None:
            if not (len(pin) == 8 and pin.isdigit()):
                raise ValueError("PIN must be 8 digits")
        if delay is not None:
            delay_int = int(delay)
            if delay_int < 0 or delay_int > 60:
                raise ValueError("Delay must be between 0 and 60")
        cmd = ['sudo', 'python3', self.oneshot_path, '-i', self.interface]
        if bssid:
            cmd.extend(['-b', bssid])
        if pixie:
            cmd.append('-K')
        if bruteforce:
            cmd.append('-B')
        if pbc:
            cmd.append('--pbc')
        if pin:
            cmd.extend(['-p', pin])
        if delay is not None:
            cmd.extend(['-d', str(delay)])
        if pixie_force:
            cmd.append('-F')
        if show_pixie_cmd:
            cmd.append('-X')
        if verbose:
            cmd.append('-v')
        if iface_down:
            cmd.append('--iface-down')
        vuln_path = vuln_list or self.vuln_list
        if os.path.exists(vuln_path):
            cmd.extend(['--vuln-list', vuln_path])
        return cmd

    def start(self, params=None):
        if params is None:
            params = {}
        return self.run_oneshot(
            bssid=params.get("bssid"),
            pixie=params.get("pixie", True),
            bruteforce=params.get("bruteforce", False),
            pbc=params.get("pbc", False),
            pin=params.get("pin"),
            delay=params.get("delay"),
            pixie_force=params.get("pixie_force", False),
            show_pixie_cmd=params.get("show_pixie_cmd", False),
            verbose=params.get("verbose", False),
            iface_down=params.get("iface_down", True),
        )

    def run_oneshot(self, bssid=None, pixie=True, bruteforce=False, pbc=False,
                    pin=None, delay=None, pixie_force=False, show_pixie_cmd=False,
                    verbose=False, iface_down=True):
        try:
            if self.shared_data.config.get("enable_monitor_mode", False):
                mon = self.shared_data.monitor_instance
                if mon:
                    mon.stop_monitor()
                self.shared_data.config["enable_monitor_mode"] = False
                self.output.append("[!] Monitor mode disabled for WPS attack")
                logger.info("Disabled monitor mode before WPS")
            if self.shared_data.config.get("evil_ap_running", False):
                evil = self.shared_data.evil_ap_instance
                if evil:
                    evil.stop()
                portal = self.shared_data.captive_portal_instance
                if portal:
                    portal.stop()
                self.shared_data.config["evil_ap_running"] = False
                self.output.append("[!] Evil AP stopped for WPS attack")
                logger.info("Stopped Evil AP before WPS")
            cmd = self.build_cmd(bssid, pixie, bruteforce, pbc, pin, delay,
                                 pixie_force, show_pixie_cmd, verbose, iface_down)
            self.running = True
            self.output = []
            self.process = subprocess.Popen(
                cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True
            )
            logger.info(f"OneShot started: {' '.join(cmd)}")
            self.shared_data.oneshot_instance = self
            thread = threading.Thread(target=self._read_output, daemon=True)
            thread.start()
            return True
        except Exception as e:
            logger.error(f"Failed to start OneShot: {e}")
            return False

    def _read_output(self):
        try:
            for line in iter(self.process.stdout.readline, ''):
                if not line:
                    break
                with self._output_lock:
                    self.output.append(line.rstrip())
                    if len(self.output) > 500:
                        self.output = self.output[-500:]
        except Exception:
            pass
        finally:
            self.running = False

    def stop(self):
        return self.stop_oneshot()

    def stop_oneshot(self):
        if self.process:
            self.process.send_signal(signal.SIGINT)
            try:
                self.process.wait(timeout=5)
            except Exception:
                self.process.kill()
            self.process = None
            self.running = False
            logger.info("OneShot stopped")
            return True
        return False

    def get_output(self):
        with self._output_lock:
            return list(self.output)

    def get_status(self):
        status = {"running": self.running, "output": self.get_output()[-50:]}
        if self.process and self.process.poll() is not None:
            self.running = False
            status["running"] = False
            status["exit_code"] = self.process.returncode
        return status

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
