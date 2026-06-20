import logging

from logger import Logger

logger = Logger(name="karma.py", level=logging.DEBUG)
b_class = "KarmaConfig"
b_module = "karma"
b_status = "karma"
b_port = 0
b_parent = None


class KarmaConfig:
    def __init__(self, shared_data):
        self.shared_data = shared_data

    @staticmethod
    def get_hostapd_karma_config():
        return """
# Karma attack — accept all probe requests
accept_mac_file=/dev/null
ignore_broadcast_ssid=0
ap_isolate=0
"""

    def is_karma_compatible(self, interface=""):
        return True

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
