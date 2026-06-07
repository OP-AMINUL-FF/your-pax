import os
import logging
import datetime
import threading

from logger import Logger

logger = Logger(name="loot_monitor.py", level=logging.DEBUG)
b_class = "LootMonitor"
b_module = "loot_monitor"
b_status = "loot_monitor"
b_port = 0


class LootMonitor:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.dns_queries = []
        self.http_requests = []
        self.devices = {}
        self._lock = threading.Lock()
        self._max_entries = 100

    def add_dns_query(self, domain, client_ip="", timestamp=""):
        with self._lock:
            self.dns_queries.append({
                "domain": domain,
                "client": client_ip,
                "time": timestamp or str(datetime.datetime.now())
            })
            if len(self.dns_queries) > self._max_entries:
                self.dns_queries = self.dns_queries[-self._max_entries:]

    def add_http_request(self, url, user_agent="", client_ip="", timestamp=""):
        with self._lock:
            os_info = self._detect_os(user_agent)
            self.http_requests.append({
                "url": url,
                "os": os_info,
                "client": client_ip,
                "time": timestamp or str(datetime.datetime.now()),
                "ua": user_agent
            })
            if len(self.http_requests) > self._max_entries:
                self.http_requests = self.http_requests[-self._max_entries:]

    def add_device(self, mac, ip, hostname=""):
        with self._lock:
            self.devices[mac] = {
                "mac": mac,
                "ip": ip,
                "hostname": hostname,
                "first_seen": self.devices.get(mac, {}).get("first_seen", str(datetime.datetime.now())),
                "last_seen": str(datetime.datetime.now())
            }

    def _detect_os(self, user_agent):
        ua = user_agent.lower()
        if "android" in ua:
            return "Android"
        if "iphone" in ua or "ipad" in ua or "ios" in ua:
            return "iOS"
        if "windows" in ua:
            return "Windows"
        if "macintosh" in ua or "mac os" in ua:
            return "macOS"
        if "linux" in ua:
            return "Linux"
        return "Unknown"

    def get_data(self):
        with self._lock:
            return {
                "dns": self.dns_queries[-50:],
                "http": self.http_requests[-50:],
                "devices": list(self.devices.values())
            }

    def clear(self):
        with self._lock:
            self.dns_queries = []
            self.http_requests = []
            self.devices = {}

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
