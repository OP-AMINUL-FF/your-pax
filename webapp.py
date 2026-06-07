#webapp.py 
import json
import threading
import http.server
import socketserver
import logging
import sys
import signal
import os
import gzip
import io
from urllib.parse import unquote
from logger import Logger
from init_shared import shared_data
from utils import WebUtils

# Initialize the logger
logger = Logger(name="webapp.py", level=logging.DEBUG)

ERROR_404_HTML = b'''<!DOCTYPE html><html><head><title>404 - Not Found</title><style>
body{background:#0d1117;color:#00ff41;font-family:monospace;display:flex;justify-content:center;align-items:center;height:100vh;margin:0}
div{text-align:center}h1{font-size:3em;margin:0}p{color:#888}
</style></head><body><div><h1>404</h1><p>your-pax - Page not found</p></div></body></html>'''

ERROR_500_HTML = b'''<!DOCTYPE html><html><head><title>500 - Server Error</title><style>
body{background:#0d1117;color:#ff4444;font-family:monospace;display:flex;justify-content:center;align-items:center;height:100vh;margin:0}
div{text-align:center}h1{font-size:3em;margin:0}p{color:#888}
</style></head><body><div><h1>500</h1><p>your-pax - Internal server error</p></div></body></html>'''

ERROR_403_HTML = b'''<!DOCTYPE html><html><head><title>403 - Forbidden</title><style>
body{background:#0d1117;color:#ffaa00;font-family:monospace;display:flex;justify-content:center;align-items:center;height:100vh;margin:0}
div{text-align:center}h1{font-size:3em;margin:0}p{color:#888}
</style></head><body><div><h1>403</h1><p>your-pax - Access denied</p></div></body></html>'''

class CustomHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self.shared_data = shared_data
        self.web_utils = WebUtils(shared_data, logger)
        super().__init__(*args, **kwargs)

    def log_message(self, format, *args):
        # Override to suppress logging of GET requests.
        if 'GET' not in format % args:
            logger.info("%s - - [%s] %s\n" %
                        (self.client_address[0],
                         self.log_date_time_string(),
                         format % args))

    def gzip_encode(self, content):
        """Gzip compress the given content."""
        out = io.BytesIO()
        with gzip.GzipFile(fileobj=out, mode="w") as f:
            f.write(content)
        return out.getvalue()

    def send_gzipped_response(self, content, content_type):
        """Send a gzipped HTTP response."""
        gzipped_content = self.gzip_encode(content)
        self.send_response(200)
        self.send_header("Content-type", content_type)
        self.send_header("Content-Encoding", "gzip")
        self.send_header("Content-Length", str(len(gzipped_content)))
        self.end_headers()
        self.wfile.write(gzipped_content)

    CSRF_SCRIPT = (
        b'<script>'
        b'var CSRF_TOKEN="TOKEN";'
        b'(function(){'
        b'var _f=window.fetch;'
        b'window.fetch=function(u,o){o=o||{};if(o.method&&o.method.toUpperCase()!=="GET"){o.headers=o.headers||{};o.headers["X-CSRF-Token"]=CSRF_TOKEN;}return _f.call(window,u,o);};'
        b'var _o=XMLHttpRequest.prototype.open;'
        b'XMLHttpRequest.prototype.open=function(m){this._m=m;return _o.apply(this,arguments);};'
        b'var _s=XMLHttpRequest.prototype.send;'
        b'XMLHttpRequest.prototype.send=function(b){if(this._m&&this._m.toUpperCase()!=="GET"){this.setRequestHeader("X-CSRF-Token",CSRF_TOKEN);}return _s.apply(this,arguments);};'
        b'})();'
        b'</script>'
    )

    def serve_file_gzipped(self, file_path, content_type):
        """Serve a file with gzip compression."""
        with open(file_path, 'rb') as file:
            content = file.read()
        if content_type == 'text/html':
            script = self.CSRF_SCRIPT.replace(b'TOKEN', self.shared_data.csrf_token.encode())
            content = content.replace(b'</head>', script + b'</head>')
        self.send_gzipped_response(content, content_type)

    def do_GET(self):
        if not self._check_auth():
            self.send_response(401)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "error", "message": "Authentication required"}).encode('utf-8'))
            return
        if self.path == '/index.html' or self.path == '/':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'index.html'), 'text/html')
        elif self.path == '/config.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'config.html'), 'text/html')
        elif self.path == '/actions.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'actions.html'), 'text/html')
        elif self.path == '/bt_connect.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'bt_connect.html'), 'text/html')
        elif self.path == '/evil_ap.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'evil_ap.html'), 'text/html')
        elif self.path == '/network.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'network.html'), 'text/html')
        elif self.path == '/netkb.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'netkb.html'), 'text/html')
        elif self.path == '/your-pax.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'your-pax.html'), 'text/html')
        elif self.path == '/loot.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'loot.html'), 'text/html')
        elif self.path == '/credentials.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'credentials.html'), 'text/html')
        elif self.path == '/manual.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'manual.html'), 'text/html')
        elif self.path == '/load_config':
            self.web_utils.serve_current_config(self)
        elif self.path == '/get_web_delay':
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            response = json.dumps({"web_delay": self.shared_data.web_delay})
            self.wfile.write(response.encode('utf-8'))
        elif self.path == '/scan_wifi':
            self.web_utils.scan_wifi(self)
        elif self.path == '/network_data':
            self.web_utils.serve_network_data(self)
        elif self.path == '/netkb_data':
            self.web_utils.serve_netkb_data(self)
        elif self.path == '/netkb_data_json':
            self.web_utils.serve_netkb_data_json(self)
        elif self.path == '/netkb_data_json_full':
            self.web_utils.serve_netkb_data_json_full(self)
        elif self.path.startswith('/screen.png'):
            self.web_utils.serve_image(self)
        elif self.path == '/favicon.ico':
            self.web_utils.serve_favicon(self)
        elif self.path == '/manifest.json':
            self.web_utils.serve_manifest(self)
        elif self.path == '/apple-touch-icon':
            self.web_utils.serve_apple_touch_icon(self)
        elif self.path == '/get_logs':
            self.web_utils.serve_logs(self)
        elif self.path == '/list_credentials':
            self.web_utils.serve_credentials_data(self)
        elif self.path == '/list_credentials_json':
            self.web_utils.serve_credentials_data_json(self)
        elif self.path == '/network_data_json':
            self.web_utils.serve_network_data_json(self)
        elif self.path.startswith('/list_files'):
            self.web_utils.list_files_endpoint(self)
        elif self.path.startswith('/download_file'):
            self.web_utils.download_file(self)
        elif self.path.startswith('/download_backup'):
            self.web_utils.download_backup(self)
        elif self.path == '/wifi_scan_advanced':
            self.web_utils.wifi_scan_advanced(self)
        elif self.path == '/handshake_status':
            self.web_utils.handshake_status(self)
        elif self.path == '/pmkid_status':
            self.web_utils.pmkid_status(self)
        elif self.path == '/oneshot_status':
            self.web_utils.oneshot_status(self)
        elif self.path == '/evil_ap_status':
            self.web_utils.evil_ap_status(self)
        elif self.path == '/conflict_status':
            self.web_utils.conflict_status(self)
        elif self.path == '/store.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'store.html'), 'text/html')
        elif self.path == '/store_data':
            self.web_utils.store_data(self)
        elif self.path == '/wifi.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'wifi.html'), 'text/html')
        elif self.path == '/wifi_target.html':
            self.serve_file_gzipped(os.path.join(self.shared_data.webdir, 'wifi_target.html'), 'text/html')
        elif self.path == '/list_interfaces':
            self.web_utils.list_interfaces(self)
        elif self.path.startswith('/download_store'):
            self.web_utils.download_store(self)
        elif self.path == '/bluetooth_status':
            self.web_utils.bluetooth_status(self)
        elif self.path == '/wifi_status':
            self.web_utils.wifi_status(self)
        elif self.path == '/bluetooth_devices':
            self.web_utils.bluetooth_devices(self)
        elif self.path == '/csrf_token':
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"csrf_token": self.shared_data.csrf_token}).encode('utf-8'))
        elif self.path == '/portal_list':
            self.web_utils.portal_list(self)
        elif self.path == '/evil_clients':
            self.web_utils.evil_clients(self)
        elif self.path == '/loot_monitor_data':
            self.web_utils.loot_monitor_data(self)
        elif self.path.startswith('/scan_targets'):
            self.web_utils.scan_targets(self)
        elif self.path == '/wpa_validate_status':
            self.web_utils.wpa_validate_status(self)
        else:
            self.send_error_page(404, ERROR_404_HTML)

    def send_error_page(self, code, body):
        self.send_response(code)
        self.send_header("Content-type", "text/html")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()
        self.wfile.write(body)

    def _check_auth(self):
        web_token = self.shared_data.config.get('web_token', '')
        if not web_token:
            return True
        query_token = self._get_query_param('token')
        if query_token == web_token:
            return True
        auth_header = self.headers.get('Authorization', '')
        if auth_header.startswith('Bearer ') and auth_header[7:] == web_token:
            return True
        return False

    def _get_query_param(self, name):
        if '?' in self.path:
            query = self.path.split('?', 1)[1]
            for part in query.split('&'):
                if '=' in part:
                    key, val = part.split('=', 1)
                    if key == name:
                        return unquote(val)
        return None

    def _validate_csrf(self):
        token = self.headers.get('X-CSRF-Token', '')
        if token and token == self.shared_data.csrf_token:
            return True
        if self.headers.get('Content-Length'):
            cl = int(self.headers['Content-Length'])
            if 0 < cl < 1048576:
                body = self.rfile.read(cl)
                self.rfile = io.BytesIO(body)
                try:
                    data = json.loads(body.decode('utf-8'))
                    if data.get('csrf_token') == self.shared_data.csrf_token:
                        return True
                except (json.JSONDecodeError, UnicodeDecodeError, AttributeError):
                    pass
        return False

    def do_POST(self):
        if not self._check_auth():
            self.send_response(401)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "error", "message": "Authentication required"}).encode('utf-8'))
            return
        if not self._validate_csrf():
            self.send_response(403)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"status": "error", "message": "CSRF validation failed"}).encode('utf-8'))
            return
        if self.path == '/save_config':
            self.web_utils.save_configuration(self)
        elif self.path == '/connect_wifi':
            self.web_utils.connect_wifi(self)
            self.shared_data.wifichanged = True  # Set the flag when Wi-Fi is connected
        elif self.path == '/disconnect_wifi':  # New route to disconnect Wi-Fi
            self.web_utils.disconnect_and_clear_wifi(self)
        elif self.path == '/clear_files':
            self.web_utils.clear_files(self)
        elif self.path == '/clear_files_light':
            self.web_utils.clear_files_light(self)
        elif self.path == '/initialize_csv':
            self.web_utils.initialize_csv(self)
        elif self.path == '/reboot':
            self.web_utils.reboot_system(self)
        elif self.path == '/shutdown':
            self.web_utils.shutdown_system(self)
        elif self.path == '/restore_default_config':
            self.web_utils.restore_default_config(self)
        elif self.path == '/restart_your_pax_service':
            self.web_utils.restart_your_pax_service(self)
        elif self.path == '/backup':
            self.web_utils.backup(self)
        elif self.path == '/restore':
            self.web_utils.restore(self)
        elif self.path == '/stop_orchestrator':  # New route to stop the orchestrator
            self.web_utils.stop_orchestrator(self)
        elif self.path == '/start_orchestrator':  # New route to start the orchestrator
            self.web_utils.start_orchestrator(self)
        elif self.path == '/execute_manual_attack':  # New route to execute a manual attack
            self.web_utils.execute_manual_attack(self)
        elif self.path == '/handshake_start':
            self.web_utils.handshake_start(self)
        elif self.path == '/handshake_stop':
            self.web_utils.handshake_stop(self)
        elif self.path == '/deauth_attack':
            self.web_utils.deauth_attack(self)
        elif self.path == '/pmkid_start':
            self.web_utils.pmkid_start(self)
        elif self.path == '/pmkid_stop':
            self.web_utils.pmkid_stop(self)
        elif self.path == '/oneshot':
            self.web_utils.oneshot(self)
        elif self.path == '/oneshot_stop':
            self.web_utils.oneshot_stop(self)
        elif self.path == '/start_evil_ap':
            self.web_utils.start_evil_ap(self)
        elif self.path == '/stop_evil_ap':
            self.web_utils.stop_evil_ap(self)
        elif self.path == '/trigger_scan':
            self.web_utils.trigger_scan(self)
        elif self.path == '/trigger_bruteforce':
            self.web_utils.trigger_bruteforce(self)
        elif self.path == '/trigger_vulnscan':
            self.web_utils.trigger_vulnscan(self)
        elif self.path == '/trigger_steal':
            self.web_utils.trigger_steal(self)
        elif self.path == '/stop_all':
            self.web_utils.stop_all(self)
        elif self.path == '/upload_portal':
            self.web_utils.upload_portal(self)
        elif self.path == '/delete_portal':
            self.web_utils.delete_portal(self)
        elif self.path == '/stop_evil_clone':
            self.web_utils.stop_evil_clone(self)
        else:
            self.send_error_page(404, ERROR_404_HTML)

class WebThread(threading.Thread):
    """
    Thread to run the web server serving the EPD display interface.
    """
    def __init__(self, handler_class=CustomHandler, port=8000):
        super().__init__()
        self.shared_data = shared_data
        self.port = port
        self.handler_class = handler_class
        self.httpd = None

    def run(self):
        """
        Run the web server in a separate thread with concurrent request handling.
        """
        while not self.shared_data.webapp_should_exit:
            try:
                self.httpd = socketserver.ThreadingTCPServer(("", self.port), self.handler_class)
                self.httpd.daemon_threads = True
                self.httpd.allow_reuse_address = True
                self.httpd.timeout = 1
                logger.info(f"Serving at port {self.port} (multi-threaded)")
                while not self.shared_data.webapp_should_exit:
                    self.httpd.handle_request()
            except OSError as e:
                if e.errno == 98:  # Address already in use error
                    logger.warning(f"Port {self.port} is in use, trying the next port...")
                    self.port += 1
                else:
                    logger.error(f"Error in web server: {e}")
                    break
            finally:
                if self.httpd:
                    self.httpd.server_close()
                    logger.info("Web server closed.")

    def shutdown(self):
        """
        Shutdown the web server gracefully.
        """
        if self.httpd:
            self.httpd.shutdown()
            self.httpd.server_close()
            logger.info("Web server shutdown initiated.")

def handle_exit_web(signum, frame):
    """
    Handle exit signals to shutdown the web server cleanly.
    """
    shared_data.webapp_should_exit = True
    if web_thread.is_alive():
        web_thread.shutdown()
        web_thread.join()  # Wait until the web_thread is finished
    logger.info("Server shutting down...")
    sys.exit(0)

# Initialize the web thread
web_thread = WebThread(port=8000)

# Set up signal handling for graceful shutdown
signal.signal(signal.SIGINT, handle_exit_web)
signal.signal(signal.SIGTERM, handle_exit_web)

if __name__ == "__main__":
    try:
        # Start the web server thread
        web_thread.start()
        logger.info("Web server thread started.")
    except Exception as e:
        logger.error(f"An exception occurred during web server start: {e}")
        handle_exit_web(signal.SIGINT, None)
        sys.exit(1)
