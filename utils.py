#utils.py

import json
import subprocess
import os
import json
import csv
import zipfile
import uuid
import cgi
import io
import glob
import logging
import time
from html import escape
from datetime import datetime
from logger import Logger
from urllib.parse import unquote
import re



logger = Logger(name="utils.py", level=logging.DEBUG)


class WebUtils:
    def __init__(self, shared_data, logger):
        self.shared_data = shared_data
        self.logger = logger
        self.actions = None  # List that contains all actions
        self.standalone_actions = None  # List that contains all standalone actions

    def load_actions(self):
        """Load all actions from the actions file"""
        if self.actions is None or self.standalone_actions is None:
            self.actions = []
            self.standalone_actions = []
            self.actions_dir = self.shared_data.actions_dir
            from orchestrator import load_actions_from_config
            self.actions, self.standalone_actions, self.network_scanner, self.nmap_vuln_scanner = \
                load_actions_from_config(self.shared_data, self.logger)

    def serve_netkb_data_json(self, handler):
        try:
            netkb_file = self.shared_data.netkbfile
            with open(netkb_file, 'r', encoding='utf-8') as file:
                reader = csv.DictReader(file)
                data = [row for row in reader if row['Alive'] == '1']

            actions = reader.fieldnames[5:]  # Actions are all fields after 'Ports'
            response_data = {
                'ips': [row['IPs'] for row in data],
                'ports': {row['IPs']: row['Ports'].split(';') for row in data},
                'actions': actions
            }

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(response_data).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def execute_manual_attack(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = handler.rfile.read(content_length).decode('utf-8')
            params = json.loads(post_data)
            ip = params.get('ip', '').strip()
            port = params.get('port', '')
            action_class = params.get('action', '').strip()

            import re
            if not re.match(r'^\d{1,3}(\.\d{1,3}){3}$', ip):
                raise ValueError("Invalid IP address format")
            if port != '0' and not (port.isdigit() and 1 <= int(port) <= 65535):
                raise ValueError("Invalid port number")
            if not re.match(r'^[a-zA-Z0-9_-]+$', action_class):
                raise ValueError("Invalid action class")

            self.logger.info(f"Received request to execute {action_class} on {ip}:{port}")

            # Charger les actions si ce n'est pas déjà fait
            self.load_actions()

            action_instance = next((action for action in self.actions if action.action_name == action_class), None)
            if action_instance is None:
                raise Exception(f"Action class {action_class} not found")

            # Charger les données actuelles
            current_data = self.shared_data.read_data()
            row = next((r for r in current_data if r["IPs"] == ip), None)

            if row is None:
                raise Exception(f"No data found for IP: {ip}")

            action_key = action_instance.action_name
            self.logger.info(f"Executing {action_key} on {ip}:{port}")
            result = action_instance.execute(ip, port, row, action_key)

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            if result == 'success':
                row[action_key] = f'success_{timestamp}'
                self.logger.info(f"Action {action_key} executed successfully on {ip}:{port}")
            else:
                row[action_key] = f'failed_{timestamp}'
                self.logger.error(f"Action {action_key} failed on {ip}:{port}")
            self.shared_data.write_data(current_data)

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Manual attack executed"}).encode('utf-8'))
        except Exception as e:
            self.logger.error(f"Error executing manual attack: {e}")
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Attack execution failed"}).encode('utf-8'))


    def serve_logs(self, handler):
        try:
            log_file_path = self.shared_data.webconsolelog
            if not os.path.exists(log_file_path):
                log_files = glob.glob(os.path.join(self.shared_data.currentdir, 'data', 'logs', '*'))
                if log_files:
                    with open(log_file_path, 'w') as lf:
                        subprocess.Popen(['sudo', 'tail', '-f'] + log_files, stdout=lf, stderr=subprocess.DEVNULL)

            with open(log_file_path, 'r') as log_file:
                log_lines = log_file.readlines()

            max_lines = 2000
            if len(log_lines) > max_lines:
                log_lines = log_lines[-max_lines:]
                with open(log_file_path, 'w') as log_file:
                    log_file.writelines(log_lines)

            log_data = ''.join(log_lines)

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"logs": log_data}).encode('utf-8'))
        except BrokenPipeError:
            # Ignore broken pipe errors
            pass
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def start_orchestrator(self, handler):
        try:
            your_pax_instance = self.shared_data.your_pax_instance
            your_pax_instance.start_orchestrator()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Orchestrator starting..."}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def stop_orchestrator(self, handler):
        try:
            your_pax_instance = self.shared_data.your_pax_instance
            your_pax_instance.stop_orchestrator()
            self.shared_data.orchestrator_should_exit = True
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Orchestrator stopping..."}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def backup(self, handler):
        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            backup_filename = f"backup_{timestamp}.zip"
            backup_path = os.path.join(self.shared_data.backupdir, backup_filename)

            with zipfile.ZipFile(backup_path, 'w') as backup_zip:
                for folder in [self.shared_data.configdir, self.shared_data.datadir, self.shared_data.actions_dir, self.shared_data.resourcesdir]:
                    for root, dirs, files in os.walk(folder):
                        for file in files:
                            file_path = os.path.join(root, file)
                            backup_zip.write(file_path, os.path.relpath(file_path, self.shared_data.currentdir))

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "url": f"/download_backup?filename={backup_filename}", "filename": backup_filename, "message": "Backup created successfully"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def restore(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            field_data = handler.rfile.read(content_length)
            field_storage = cgi.FieldStorage(fp=io.BytesIO(field_data), headers=handler.headers, environ={'REQUEST_METHOD': 'POST'})

            file_item = field_storage['file']
            if file_item.filename:
                backup_path = os.path.join(self.shared_data.upload_dir, file_item.filename)
                with open(backup_path, 'wb') as output_file:
                    output_file.write(file_item.file.read())

                with zipfile.ZipFile(backup_path, 'r') as backup_zip:
                    for entry in backup_zip.namelist():
                        entry_path = os.path.abspath(os.path.join(self.shared_data.currentdir, entry))
                        if not entry_path.startswith(os.path.abspath(self.shared_data.currentdir)):
                            raise ValueError(f"Zip slip detected: {entry}")
                    backup_zip.extractall(self.shared_data.currentdir)

                handler.send_response(200)
                handler.send_header("Content-type", "application/json")
                handler.end_headers()
                handler.wfile.write(json.dumps({"status": "success", "message": "Restore completed successfully"}).encode('utf-8'))
            else:
                handler.send_response(400)
                handler.send_header("Content-type", "application/json")
                handler.end_headers()
                handler.wfile.write(json.dumps({"status": "error", "message": "No selected file"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Restore failed"}).encode('utf-8'))

    def download_backup(self, handler):
        query = unquote(handler.path.split('?filename=')[1])
        base_dir = os.path.abspath(self.shared_data.backupdir)
        requested_path = os.path.abspath(os.path.join(base_dir, query))
        if os.path.commonpath([base_dir, requested_path]) != base_dir:
            handler.send_response(403)
            handler.end_headers()
            return
        if os.path.isfile(requested_path):
            handler.send_response(200)
            handler.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(requested_path)}"')
            handler.send_header("Content-type", "application/zip")
            handler.end_headers()
            with open(requested_path, 'rb') as file:
                handler.wfile.write(file.read())
        else:
            handler.send_response(404)
            handler.end_headers()

    def serve_credentials_data(self, handler):
        try:
            directory = self.shared_data.crackedpwddir
            html_content = self.generate_html_for_csv_files(directory)
            handler.send_response(200)
            handler.send_header("Content-type", "text/html")
            handler.end_headers()
            handler.wfile.write(html_content.encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def serve_credentials_data_json(self, handler):
        try:
            directory = self.shared_data.crackedpwddir
            files_data = []
            for filename in sorted(os.listdir(directory)):
                if filename.endswith('.csv'):
                    filepath = os.path.join(directory, filename)
                    with open(filepath, 'r', encoding='utf-8') as f:
                        reader = csv.reader(f)
                        headers = next(reader) if reader else []
                        rows = [row for row in reader]
                    files_data.append({"name": filename, "headers": headers, "rows": rows})
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(files_data).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def serve_network_data_json(self, handler):
        try:
            latest_file = max(
                [os.path.join(self.shared_data.scan_results_dir, f) for f in os.listdir(self.shared_data.scan_results_dir) if f.startswith('result_')],
                key=os.path.getctime
            )
            with open(latest_file, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                headers = next(reader) if reader else []
                rows = [row for row in reader]
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"headers": headers, "rows": rows}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def generate_html_for_csv_files(self, directory):
        html = '<div class="credentials-container">\n'
        for filename in os.listdir(directory):
            if filename.endswith('.csv'):
                filepath = os.path.join(directory, filename)
                html += f'<h2>{escape(filename)}</h2>\n'
                html += '<table class="styled-table">\n<thead>\n<tr>\n'
                with open(filepath, 'r') as file:
                    reader = csv.reader(file)
                    headers = next(reader)
                    for header in headers:
                        html += f'<th>{escape(header)}</th>\n'
                    html += '</tr>\n</thead>\n<tbody>\n'
                    for row in reader:
                        html += '<tr>\n'
                        for cell in row:
                            html += f'<td>{escape(cell)}</td>\n'
                        html += '</tr>\n'
                html += '</tbody>\n</table>\n'
        html += '</div>\n'
        return html

    def list_files(self, directory):
        files = []
        for entry in os.scandir(directory):
            if entry.is_dir():
                files.append({
                    "name": entry.name,
                    "is_directory": True,
                    "children": self.list_files(entry.path)
                })
            else:
                files.append({
                    "name": entry.name,
                    "is_directory": False,
                    "path": entry.path
                })
        return files



    def serve_file(self, handler, filename):
        try:
            with open(os.path.join(self.shared_data.webdir, filename), 'r', encoding='utf-8') as file:
                content = file.read()
                content = content.replace('{{ web_delay }}', str(self.shared_data.web_delay * 1000))
                handler.send_response(200)
                handler.send_header("Content-type", "text/html")
                handler.end_headers()
                handler.wfile.write(content.encode('utf-8'))
        except FileNotFoundError:
            handler.send_response(404)
            handler.end_headers()

    def serve_current_config(self, handler):
        handler.send_response(200)
        handler.send_header("Content-type", "application/json")
        handler.end_headers()
        with open(self.shared_data.shared_config_json, 'r') as f:
            config = json.load(f)
        handler.wfile.write(json.dumps(config).encode('utf-8'))

    def restore_default_config(self, handler):
        handler.send_response(200)
        handler.send_header("Content-type", "application/json")
        handler.end_headers()
        self.shared_data.config = self.shared_data.default_config.copy()
        self.shared_data.save_config()
        handler.wfile.write(json.dumps(self.shared_data.config).encode('utf-8'))

    def serve_image(self, handler):
        image_path = os.path.join(self.shared_data.webdir, 'screen.png')
        try:
            with open(image_path, 'rb') as file:
                handler.send_response(200)
                handler.send_header("Content-type", "image/png")
                handler.send_header("Cache-Control", "max-age=0, must-revalidate")
                handler.end_headers()
                handler.wfile.write(file.read())
        except FileNotFoundError:
            handler.send_response(404)
            handler.end_headers()
        except BrokenPipeError:
            # Ignore broken pipe errors
            pass
        except Exception as e:
            self.logger.error(f"Unexpected error: {e}")


    def serve_favicon(self, handler):
        handler.send_response(200)
        handler.send_header("Content-type", "image/x-icon")
        handler.end_headers()
        favicon_path = self.shared_data.config.get('favicon_path', '') or os.path.join(self.shared_data.webdir, 'images', 'favicon.ico')
        self.logger.info(f"Serving favicon from {favicon_path}")
        try:
            with open(favicon_path, 'rb') as file:
                handler.wfile.write(file.read())
        except FileNotFoundError:
            self.logger.error(f"Favicon not found at {favicon_path}")
            handler.send_response(404)
            handler.end_headers()

    def serve_manifest(self, handler):
        handler.send_response(200)
        handler.send_header("Content-type", "application/json")
        handler.end_headers()
        manifest_path = os.path.join(self.shared_data.webdir, 'manifest.json')
        try:
            with open(manifest_path, 'r') as file:
                handler.wfile.write(file.read().encode('utf-8'))
        except FileNotFoundError:
            handler.send_response(404)
            handler.end_headers()
    
    def serve_apple_touch_icon(self, handler):
        handler.send_response(200)
        handler.send_header("Content-type", "image/png")
        handler.end_headers()
        icon_path = os.path.join(self.shared_data.webdir, 'images', 'apple-touch-icon.png')
        try:
            with open(icon_path, 'rb') as file:
                handler.wfile.write(file.read())
        except FileNotFoundError:
            handler.send_response(404)
            handler.end_headers()

    def scan_wifi(self, handler):
        try:
            result = subprocess.Popen(['sudo', 'iwlist', 'wlan0', 'scan'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            stdout, stderr = result.communicate()
            if result.returncode != 0:
                raise Exception(stderr)
            networks = self.parse_scan_result(stdout)
            self.logger.info(f"Found {len(networks)} networks")
            current_ssid = subprocess.Popen(['iwgetid', '-r'], stdout=subprocess.PIPE, text=True)
            ssid_out, ssid_err = current_ssid.communicate()
            if current_ssid.returncode != 0:
                raise Exception(ssid_err)
            current_ssid = ssid_out.strip()
            self.logger.info(f"Current SSID: {current_ssid}")
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"networks": networks, "current_ssid": current_ssid}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            self.logger.error(f"Error scanning Wi-Fi networks: {e}")
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def parse_scan_result(self, scan_output):
        networks = []
        for line in scan_output.split('\n'):
            if 'ESSID' in line:
                ssid = line.split(':')[1].strip('"')
                if ssid not in networks:
                    networks.append(ssid)
        return networks

    def connect_wifi(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = handler.rfile.read(content_length).decode('utf-8')
            params = json.loads(post_data)
            ssid = params['ssid']
            password = params['password']
            hidden = bool(params.get('hidden', False))

            self.update_nmconnection(ssid, password, hidden)
            connect_result = subprocess.Popen(['sudo', 'nmcli', 'connection', 'up', 'preconfigured'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            stdout, stderr = connect_result.communicate()
            if connect_result.returncode != 0:
                raise Exception(stderr)

            self.shared_data.wifichanged = True

            handler.send_response(200)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Connected to " + ssid}).encode('utf-8'))

        except Exception as e:
            handler.send_response(500)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def disconnect_and_clear_wifi(self, handler):
        try:
            disconnect_result = subprocess.Popen(['sudo', 'nmcli', 'connection', 'down', 'preconfigured'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            stdout, stderr = disconnect_result.communicate()
            if disconnect_result.returncode != 0:
                raise Exception(stderr)

            config_path = '/etc/NetworkManager/system-connections/preconfigured.nmconnection'
            with open(config_path, 'w') as f:
                f.write("")
            subprocess.Popen(['sudo', 'chmod', '600', config_path]).communicate()
            subprocess.Popen(['sudo', 'nmcli', 'connection', 'reload']).communicate()

            self.shared_data.wifichanged = False

            handler.send_response(200)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Disconnected from Wi-Fi and cleared preconfigured settings"}).encode('utf-8'))

        except Exception as e:
            handler.send_response(500)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def clear_files(self, handler):
        try:
            patterns = [
                'config/*.json', 'data/*.csv', 'data/*.log', 'backup/backups/*',
                'backup/uploads/*', 'data/output/data_stolen/*', 'data/output/crackedpwd/*',
                'config/*', 'data/output/scan_results/*', '__pycache__',
                'config/__pycache__', 'data/__pycache__', 'actions/__pycache__',
                'resources/__pycache__', 'web/__pycache__', '*.log',
                'resources/waveshare_epd/__pycache__', 'data/logs/*',
                'data/output/vulnerabilities/*', 'data/logs/*'
            ]
            for pattern in patterns:
                matches = glob.glob(pattern)
                if matches:
                    subprocess.Popen(['sudo', 'rm', '-rf'] + matches, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True).communicate()

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Files cleared successfully"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def clear_files_light(self, handler):
        try:
            patterns = [
                'data/*.log', 'data/output/data_stolen/*', 'data/output/crackedpwd/*',
                'data/output/scan_results/*', '__pycache__', 'config/__pycache__',
                'data/__pycache__', 'actions/__pycache__', 'resources/__pycache__',
                'web/__pycache__', '*.log', 'resources/waveshare_epd/__pycache__',
                'data/logs/*', 'data/output/vulnerabilities/*', 'data/logs/*'
            ]
            for pattern in patterns:
                matches = glob.glob(pattern)
                if matches:
                    subprocess.Popen(['sudo', 'rm', '-rf'] + matches, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True).communicate()

            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Files cleared successfully"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def initialize_csv(self, handler):
        try:
            self.shared_data.generate_actions_json()
            self.shared_data.initialize_csv()
            self.shared_data.create_livestatusfile()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "CSV files initialized successfully"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def reboot_system(self, handler):
        try:
            subprocess.Popen(['sudo', 'reboot'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "System is rebooting"}).encode('utf-8'))
        except subprocess.CalledProcessError as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def shutdown_system(self, handler):
        try:
            subprocess.Popen(['sudo', 'shutdown', 'now'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "System is shutting down"}).encode('utf-8'))
        except subprocess.CalledProcessError as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def restart_your_pax_service(self, handler):
        try:
            subprocess.Popen(['sudo', 'systemctl', 'restart', 'your-pax.service'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "your-pax service restarted successfully"}).encode('utf-8'))
        except subprocess.CalledProcessError as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def serve_network_data(self, handler):
        try:
            latest_file = max(
                [os.path.join(self.shared_data.scan_results_dir, f) for f in os.listdir(self.shared_data.scan_results_dir) if f.startswith('result_')],
                key=os.path.getctime
            )
            table_html = self.generate_html_table(latest_file)
            handler.send_response(200)
            handler.send_header("Content-type", "text/html")
            handler.end_headers()
            handler.wfile.write(table_html.encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def generate_html_table(self, file_path):
        table_html = '<table class="styled-table"><thead><tr>'
        with open(file_path, 'r') as file:
            reader = csv.reader(file)
            headers = next(reader)
            for header in headers:
                table_html += f'<th>{escape(header)}</th>'
            table_html += '</tr></thead><tbody>'
            for row in reader:
                table_html += '<tr>'
                for cell in row:
                    cell_class = "green" if cell.strip() else "red"
                    table_html += f'<td class="{cell_class}">{escape(cell)}</td>'
                table_html += '</tr>'
            table_html += '</tbody></table>'
        return table_html

    def generate_html_table_netkb(self, file_path):
        table_html = '<table class="styled-table"><thead><tr>'
        try:
            with open(file_path, 'r', encoding='utf-8') as file:
                reader = csv.reader(file)
                headers = next(reader)
                for header in headers:
                    table_html += f'<th>{escape(header)}</th>'
                table_html += '</tr></thead><tbody>'
                for row in reader:
                    row_class = "blue-row" if '0' in row[3] else ""
                    table_html += f'<tr class="{row_class}">'
                    for cell in row:
                        cell_class = ""
                        if "success" in cell:
                            cell_class = "green bold"
                        elif "failed" in cell:
                            cell_class = "red bold"
                        elif cell.strip() == "":
                            cell_class = "grey"
                        table_html += f'<td class="{cell_class}">{escape(cell)}</td>'
                    table_html += '</tr>'
                table_html += '</tbody></table>'
        except Exception as e:
            self.logger.error(f"Error in generate_html_table_netkb: {e}")
        return table_html


    def serve_netkb_data(self, handler):
        try:
            latest_file = self.shared_data.netkbfile
            table_html = self.generate_html_table_netkb(latest_file)
            handler.send_response(200)
            handler.send_header("Content-type", "text/html")
            handler.end_headers()
            handler.wfile.write(table_html.encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def serve_netkb_data_json_full(self, handler):
        try:
            latest_file = self.shared_data.netkbfile
            with open(latest_file, 'r', encoding='utf-8') as f:
                reader = csv.reader(f)
                headers = next(reader) if reader else []
                rows = [row for row in reader]
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"headers": headers, "rows": rows}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def _sanitize_config_value(self, value):
        if not isinstance(value, str):
            return str(value)
        return re.sub(r'[^\x20-\x7E]', '', value).replace('"', '\\"').replace('\n', '').strip()

    def update_nmconnection(self, ssid, password, hidden=False):
        config_path = '/etc/NetworkManager/system-connections/preconfigured.nmconnection'
        safe_ssid = self._sanitize_config_value(ssid)
        safe_password = self._sanitize_config_value(password)
        hidden_line = "\nhidden=true" if hidden else ""
        with open(config_path, 'w') as f:
            f.write(f"""
[connection]
id=preconfigured
uuid={uuid.uuid4()}
type=wifi
autoconnect=true

[wifi]
ssid={safe_ssid}
mode=infrastructure{hidden_line}

[wifi-security]
key-mgmt=wpa-psk
psk={safe_password}

[ipv4]
method=auto

[ipv6]
method=auto
""")
        subprocess.Popen(['sudo', 'chmod', '600', config_path]).communicate()
        subprocess.Popen(['sudo', 'nmcli', 'connection', 'reload']).communicate()

    def save_configuration(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = handler.rfile.read(content_length).decode('utf-8')
            params = json.loads(post_data)
            fichier = self.shared_data.shared_config_json
            self.logger.info(f"Received params: {params}")

            with open(fichier, 'r') as f:
                current_config = json.load(f)

            allowed_keys = set(self.shared_data.get_default_config().keys())
            for key, value in params.items():
                if key not in allowed_keys:
                    self.logger.warning(f"Ignoring unknown config key: {key}")
                    continue
                if isinstance(value, bool):
                    current_config[key] = value
                elif isinstance(value, str) and value.lower() in ['true', 'false']:
                    current_config[key] = value.lower() == 'true'
                elif isinstance(value, (int, float)):
                    current_config[key] = value
                elif isinstance(value, list):
                    for val in value[:]:
                        if val == "" :
                            value.remove(val)
                    current_config[key] = value
                elif isinstance(value, str):
                    if value.replace('.', '', 1).isdigit():
                        current_config[key] = float(value) if '.' in value else int(value)
                    else:
                        current_config[key] = value
                else:
                    current_config[key] = value

            with open(fichier, 'w') as f:
                json.dump(current_config, f, indent=4)
            self.logger.info("Configuration saved to file")

            handler.send_response(200)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Configuration saved"}).encode('utf-8'))
            self.logger.info("Configuration saved (web)")

            self.shared_data.load_config()
            self.logger.info("Configuration reloaded (web)")

        except Exception as e:
            handler.send_response(500)
            handler.send_header('Content-type', 'application/json')
            handler.end_headers()
            error_message = {"status": "error", "message": "Internal error"}
            handler.wfile.write(json.dumps(error_message).encode('utf-8'))
            self.logger.error(f"Error saving configuration: {e}")

    def list_files_endpoint(self, handler):
        try:
            files = self.list_files(self.shared_data.datastolendir)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(files).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def download_file(self, handler):
        try:
            query = unquote(handler.path.split('?path=')[1])
            base_dir = os.path.abspath(self.shared_data.datastolendir)
            requested_path = os.path.abspath(os.path.join(base_dir, query))
            if os.path.commonpath([base_dir, requested_path]) != base_dir:
                handler.send_response(403)
                handler.end_headers()
                return
            if os.path.isfile(requested_path):
                handler.send_response(200)
                handler.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(requested_path)}"')
                handler.end_headers()
                with open(requested_path, 'rb') as file:
                    handler.wfile.write(file.read())
            else:
                handler.send_response(404)
                handler.end_headers()
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def wifi_scan_advanced(self, handler):
        try:
            result = subprocess.run(['sudo', 'iwlist', 'wlan0', 'scan'], capture_output=True, text=True, timeout=30)
            if result.returncode != 0:
                raise Exception(result.stderr)
            networks = []
            current = {}
            for line in result.stdout.split('\n'):
                if 'Cell ' in line and current:
                    networks.append(current)
                    current = {}
                if 'Address:' in line:
                    current['bssid'] = line.split('Address:')[1].strip()
                elif 'ESSID:' in line:
                    current['ssid'] = line.split(':')[1].strip().strip('"')
                elif 'Channel:' in line:
                    current['channel'] = line.split('Channel:')[1].strip()
                elif 'Frequency:' in line and 'channel' in line:
                    current['channel'] = line.split('channel ')[1].split(')')[0].strip()
                elif 'Signal level=' in line:
                    sig = line.split('Signal level=')[1].split(' ')[0]
                    current['signal'] = sig
                elif 'Encryption key:on' in line:
                    current['wpa'] = True
                elif 'Encryption key:off' in line:
                    current['wpa'] = False
                elif 'WPS' in line or 'Version:1' in line:
                    current['wps'] = True
                elif 'IE: IEEE 802.11i/WPA2' in line or 'IE: WPA2' in line:
                    current['wpa'] = True
            if current:
                networks.append(current)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(networks).encode('utf-8'))
        except subprocess.TimeoutExpired:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Scan timed out"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(409)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def handshake_start(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = json.loads(handler.rfile.read(content_length))
            from actions.wifi_handshake import WiFiHandshake
            hs = WiFiHandshake(self.shared_data)
            self.shared_data.handshake_instance = hs
            bssid = post_data.get('bssid', '')
            channel = post_data.get('channel', '1')
            prefix = post_data.get('prefix', None)
            import re as _re
            if not _re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
            result = hs.start_capture(bssid, channel, prefix)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success" if result else "error", "message": "Handshake capture started"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def handshake_stop(self, handler):
        try:
            hs = getattr(self.shared_data, 'handshake_instance', None)
            if hs:
                hs.stop_capture()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def handshake_status(self, handler):
        try:
            hs = getattr(self.shared_data, 'handshake_instance', None)
            running = hs is not None and hs.capture_process is not None and hs.capture_process.poll() is None
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"running": running}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def deauth_attack(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = json.loads(handler.rfile.read(content_length))
            from actions.wifi_deauth import WiFiDeauth
            deauth = WiFiDeauth(self.shared_data)
            bssid = post_data.get('bssid', '')
            client = post_data.get('client', 'ff:ff:ff:ff:ff:ff')
            count = int(post_data.get('count', 10))
            channel = post_data.get('channel', None)
            import re as _re
            if not _re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
            if not _re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', client):
                raise ValueError("Invalid client MAC format")
            if count < 1 or count > 10000:
                raise ValueError("Count must be between 1 and 10000")
            result = deauth.deauth(bssid, client, count, channel)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success" if result else "error", "message": f"Deauth sent: {count} packets"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def pmkid_start(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = json.loads(handler.rfile.read(content_length))
            from actions.wifi_pmkid import WiFiPMKID
            pmkid = WiFiPMKID(self.shared_data)
            self.shared_data.pmkid_instance = pmkid
            bssid = post_data.get('bssid', '')
            channel = post_data.get('channel', '1')
            prefix = post_data.get('prefix', None)
            import re as _re
            if not _re.match(r'^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$', bssid):
                raise ValueError("Invalid BSSID format")
            result = pmkid.start_capture(bssid, channel, prefix)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success" if result else "error", "message": "PMKID capture started"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def pmkid_stop(self, handler):
        try:
            pmkid = getattr(self.shared_data, 'pmkid_instance', None)
            if pmkid:
                pmkid.stop_capture()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def pmkid_status(self, handler):
        try:
            pmkid = getattr(self.shared_data, 'pmkid_instance', None)
            running = pmkid is not None and pmkid.running
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"running": running}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def oneshot(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = json.loads(handler.rfile.read(content_length))
            from actions.wifi_oneshot import WiFiOneShot
            oneshot = WiFiOneShot(self.shared_data)
            self.shared_data.oneshot_instance = oneshot
            bssid = post_data.get('bssid', None)
            pixie = post_data.get('pixie', True)
            bruteforce = post_data.get('bruteforce', False)
            pbc = post_data.get('pbc', False)
            pin = post_data.get('pin', None)
            delay = post_data.get('delay', None)
            pixie_force = post_data.get('pixie_force', False)
            show_pixie_cmd = post_data.get('show_pixie_cmd', False)
            verbose = post_data.get('verbose', False)
            iface_down = post_data.get('iface_down', True)
            result = oneshot.run_oneshot(bssid, pixie, bruteforce, pbc, pin, delay, pixie_force, show_pixie_cmd, verbose, iface_down)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success" if result else "error", "message": "WPS attack started"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def oneshot_stop(self, handler):
        try:
            oneshot = getattr(self.shared_data, 'oneshot_instance', None)
            if oneshot:
                oneshot.stop_oneshot()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def oneshot_status(self, handler):
        try:
            oneshot = getattr(self.shared_data, 'oneshot_instance', None)
            if oneshot:
                status = oneshot.get_status()
            else:
                status = {"running": False, "output": []}
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(status).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"running": False, "output": [], "error": "Internal error"}).encode('utf-8'))

    def start_evil_ap(self, handler):
        try:
            content_length = int(handler.headers['Content-Length'])
            post_data = json.loads(handler.rfile.read(content_length))
            from actions.captive_portal import CaptivePortal
            from actions.evil_ap import EvilAP
            from actions.wpa_validator import WPAValidator
            from actions.loot_monitor import LootMonitor
            from actions.evil_clone import EvilClone
            self.shared_data.captive_portal_class = CaptivePortal
            evil = EvilAP(self.shared_data)
            self.shared_data.evil_ap_instance = evil
            ssid = post_data.get('ssid', 'FreeWiFi')
            channel = int(post_data.get('channel', 6))
            password = post_data.get('password', None)
            interface = post_data.get('interface', None)
            if interface:
                import re as _re
                if not _re.match(r'^[a-zA-Z0-9_.-]+$', interface):
                    raise ValueError("Invalid interface name")
                evil.interface = interface
            portal = post_data.get('portal', 'generic.html')
            wpa_validate = post_data.get('wpa_validate', False)
            wpa_interface = post_data.get('wpa_interface', interface or evil.interface)
            karma = post_data.get('karma', False)
            karma_interface = post_data.get('karma_interface', interface or evil.interface)

            wpa_validator = WPAValidator(self.shared_data) if wpa_validate else None
            loot_monitor = LootMonitor(self.shared_data)

            clone_enabled = post_data.get('clone', False)
            if clone_enabled:
                from actions.evil_clone import EvilClone
                clone = EvilClone(self.shared_data)
                self.shared_data.evil_clone_instance = clone
                deauth_if = post_data.get('deauth_interface', 'wlan1')
                clone_if = post_data.get('clone_interface', interface or 'wlan0')
                target_bssid = post_data.get('target_bssid', '')
                target_ssid = post_data.get('target_ssid', ssid)
                target_channel = int(post_data.get('target_channel', channel))
                captive_portal_class = self.shared_data.captive_portal_class
                cp = captive_portal_class(self.shared_data)
                result = clone.start(
                    deauth_if, clone_if, target_bssid, target_ssid,
                    target_channel, password, portal, cp
                )
                if result:
                    self.shared_data.evil_clone_instance = clone
                    handler.send_response(200)
                    handler.send_header("Content-type", "application/json")
                    handler.end_headers()
                    handler.wfile.write(json.dumps({"status": "success", "message": "Clone AP started"}).encode('utf-8'))
                else:
                    handler.send_response(500)
                    handler.send_header("Content-type", "application/json")
                    handler.end_headers()
                    handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))
                return

            result = evil.start(
                ssid=ssid, channel=channel, wpa_passphrase=password,
                portal_template=portal,
                wpa_validate_enabled=wpa_validate,
                wpa_interface=wpa_interface,
                wpa_validator=wpa_validator,
                karma_enabled=karma,
                karma_interface=karma_interface,
                loot_monitor=loot_monitor
            )
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success" if result else "error", "message": "Evil AP started" if result else "Failed to start"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def stop_evil_ap(self, handler):
        try:
            evil = getattr(self.shared_data, 'evil_ap_instance', None)
            if evil:
                evil.stop()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "message": "Evil AP stopped"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def evil_ap_status(self, handler):
        try:
            evil = getattr(self.shared_data, 'evil_ap_instance', None)
            if evil:
                status = evil.get_status()
            else:
                status = {"running": False}
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(status).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"running": False, "error": "Internal error"}).encode('utf-8'))

    def conflict_status(self, handler):
        try:
            evil = getattr(self.shared_data, 'evil_ap_instance', None)
            oneshot = getattr(self.shared_data, 'oneshot_instance', None)
            handshake = getattr(self.shared_data, 'handshake_instance', None)
            pmkid = getattr(self.shared_data, 'pmkid_instance', None)
            monitor = getattr(self.shared_data, 'monitor_instance', None)
            status = {
                "evil_ap_running": evil is not None and evil.running if hasattr(evil, 'running') else False,
                "monitor_mode": self.shared_data.config.get("enable_monitor_mode", False),
                "handshake_running": handshake is not None and handshake.capture_process is not None and handshake.capture_process.poll() is None if hasattr(handshake, 'capture_process') else False,
                "pmkid_running": pmkid is not None and pmkid.running if hasattr(pmkid, 'running') else False,
                "oneshot_running": oneshot is not None and oneshot.running if hasattr(oneshot, 'running') else False,
            }
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(status).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def list_interfaces(self, handler):
        try:
            result = subprocess.run(['iwconfig'], capture_output=True, text=True, timeout=5)
            interfaces = []
            for line in result.stdout.split('\n'):
                if 'IEEE 802.11' in line:
                    iface = line.split()[0]
                    interfaces.append(iface)
            if not interfaces:
                result2 = subprocess.run(['ip', 'link'], capture_output=True, text=True, timeout=5)
                for line in result2.stdout.split('\n'):
                    line = line.strip()
                    if line.startswith(('wlan', 'wl')) and ':' in line:
                        iface = line.split(':')[1].strip() if ':' in line else line.split()[1].rstrip(':')
                        interfaces.append(iface)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(interfaces).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps([self.shared_data.config.get("wifi_interface", "wlan0")]).encode('utf-8'))

    def _load_captured_creds(self):
        creds_file = os.path.join(self.shared_data.datadir, "captured_creds.txt")
        creds = []
        if os.path.exists(creds_file):
            try:
                with open(creds_file, "r") as f:
                    for line in f:
                        line = line.strip()
                        if not line:
                            continue
                        parts = line.split(" | ", 1)
                        if len(parts) == 2:
                            creds.append({"time": parts[0], "password": parts[1]})
                        else:
                            creds.append({"time": "", "password": line})
            except Exception as e:
                logger.error(f"Failed to read captured creds: {e}")
                creds = []
        return creds

    def store_data(self, handler):
        try:
            data = {
                "handshakes": [],
                "pmkid": [],
                "creds_evil": self._load_captured_creds(),
                "wps_reports": [],
                "creds_cracked": {"count": 0, "services": []},
                "stolen_files": [],
                "scan_results": [],
                "vuln_scans": [],
                "zombies": [],
                "netkb_count": 0
            }
            handshake_dir = os.path.join(self.shared_data.datadir, "handshakes")
            if os.path.exists(handshake_dir):
                for f in os.listdir(handshake_dir):
                    fpath = os.path.join(handshake_dir, f)
                    if os.path.isfile(fpath):
                        data["handshakes"].append({
                            "name": f, "size": os.path.getsize(fpath),
                            "modified": datetime.fromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%d %H:%M'),
                            "path": fpath
                        })
            pmkid_dir = os.path.join(self.shared_data.datadir, "pmkid")
            if os.path.exists(pmkid_dir):
                for f in os.listdir(pmkid_dir):
                    fpath = os.path.join(pmkid_dir, f)
                    if os.path.isfile(fpath):
                        data["pmkid"].append({
                            "name": f, "size": os.path.getsize(fpath),
                            "modified": datetime.fromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%d %H:%M'),
                            "path": fpath
                        })
            if os.path.exists(self.shared_data.scan_results_dir):
                for f in os.listdir(self.shared_data.scan_results_dir):
                    fpath = os.path.join(self.shared_data.scan_results_dir, f)
                    if os.path.isfile(fpath):
                        data["scan_results"].append({
                            "name": f, "size": os.path.getsize(fpath),
                            "modified": datetime.fromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%d %H:%M'),
                            "path": fpath
                        })
            if os.path.exists(self.shared_data.vulnerabilities_dir):
                for f in os.listdir(self.shared_data.vulnerabilities_dir):
                    fpath = os.path.join(self.shared_data.vulnerabilities_dir, f)
                    if os.path.isfile(fpath):
                        data["vuln_scans"].append({
                            "name": f, "size": os.path.getsize(fpath),
                            "modified": datetime.fromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%d %H:%M'),
                            "path": fpath
                        })
            if os.path.exists(self.shared_data.zombiesdir):
                for f in os.listdir(self.shared_data.zombiesdir):
                    fpath = os.path.join(self.shared_data.zombiesdir, f)
                    if os.path.isfile(fpath):
                        data["zombies"].append({
                            "name": f, "size": os.path.getsize(fpath),
                            "modified": datetime.fromtimestamp(os.path.getmtime(fpath)).strftime('%Y-%m-%d %H:%M'),
                            "path": fpath
                        })
            if os.path.exists(self.shared_data.datastolendir):
                stolen = []
                for root, dirs, files in os.walk(self.shared_data.datastolendir):
                    for f in files:
                        fpath = os.path.join(root, f)
                        rel = os.path.relpath(fpath, self.shared_data.datastolendir)
                        stolen.append({
                            "name": f, "size": os.path.getsize(fpath),
                            "path": fpath, "rel": rel
                        })
                data["stolen_files"] = stolen
            if os.path.exists(self.shared_data.crackedpwddir):
                count = 0
                services = []
                for f in os.listdir(self.shared_data.crackedpwddir):
                    if f.endswith('.csv'):
                        fpath = os.path.join(self.shared_data.crackedpwddir, f)
                        with open(fpath, 'r') as cf:
                            line_count = sum(1 for _ in cf) - 1
                            if line_count > 0:
                                count += line_count
                                services.append({"name": f.replace('.csv', '').upper(), "count": line_count})
                data["creds_cracked"] = {"count": count, "services": services}
            if os.path.exists(self.shared_data.netkbfile):
                with open(self.shared_data.netkbfile, 'r') as f:
                    data["netkb_count"] = sum(1 for _ in f) - 1
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(data).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"error": "Internal error"}).encode('utf-8'))

    def download_store(self, handler):
        try:
            query = unquote(handler.path.split('?path=')[1]) if '?path=' in handler.path else ''
            if query:
                base_dir = os.path.abspath(self.shared_data.datastolendir)
                requested_path = os.path.abspath(os.path.join(base_dir, query))
                if os.path.commonpath([base_dir, requested_path]) != base_dir:
                    handler.send_response(403)
                    handler.end_headers()
                    return
                handler.send_response(200)
                handler.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(requested_path)}"')
                handler.end_headers()
                with open(requested_path, 'rb') as f:
                    handler.wfile.write(f.read())
            else:
                handler.send_response(404)
                handler.end_headers()
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def bluetooth_status(self, handler):
        try:
            bnap = self.shared_data
            status = {
                "active": bnap.bluetooth_nap_active,
                "bridge_ip": bnap.bluetooth_nap_ip,
                "port": 8000,
                "ssid": "your-pax",
                "connected_clients": 1 if bnap.pan_connected else 0
            }
            try:
                result = subprocess.run(['ip', 'neigh', 'show', 'dev', 'pan0'],
                                      capture_output=True, text=True, timeout=5)
                if result.returncode == 0:
                    clients = [l.split()[0] for l in result.stdout.strip().split('\n') if l.strip()]
                    status["clients"] = clients
                    status["connected_clients"] = len(clients)
            except Exception:
                pass
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(status).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def wifi_status(self, handler):
        try:
            result = subprocess.run(['iwgetid', '-r'], capture_output=True, text=True, timeout=5)
            ssid = result.stdout.strip() if result.returncode == 0 else ""
            connected = bool(ssid)
            signal = ""
            if connected:
                sig_result = subprocess.run(['iwconfig', 'wlan0'], capture_output=True, text=True, timeout=5)
                for line in sig_result.stdout.split('\n'):
                    if 'Signal level' in line:
                        signal = line.split('Signal level=')[1].split()[0] if 'Signal level=' in line else ""
                        break
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({
                "connected": connected,
                "ssid": ssid,
                "signal": signal,
                "interface": "wlan0"
            }).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def bluetooth_devices(self, handler):
        try:
            result = subprocess.run(['ip', 'neigh', 'show', 'dev', 'pan0'],
                                   capture_output=True, text=True, timeout=5)
            devices = []
            if result.returncode == 0 and result.stdout.strip():
                for line in result.stdout.strip().split('\n'):
                    parts = line.split()
                    if len(parts) >= 1:
                        devices.append({"ip": parts[0], "mac": parts[4] if len(parts) > 4 else "", "name": ""})
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"devices": devices}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def trigger_scan(self, handler):
        try:
            self.shared_data.manual_scan_trigger.set()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "triggered", "action": "network_scan"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def trigger_bruteforce(self, handler):
        try:
            protocol = "all"
            content_length = int(handler.headers.get('Content-Length', 0))
            if content_length > 0:
                post_data = json.loads(handler.rfile.read(content_length))
                protocol = post_data.get('protocol', 'all')
            self.shared_data.manual_bruteforce_protocol = protocol
            self.shared_data.manual_bruteforce_trigger.set()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "triggered", "action": "bruteforce", "protocol": protocol}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def trigger_vulnscan(self, handler):
        try:
            self.shared_data.manual_vulnscan_trigger.set()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "triggered", "action": "vuln_scan"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def trigger_steal(self, handler):
        try:
            self.shared_data.manual_steal_trigger.set()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "triggered", "action": "steal_files"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def stop_all(self, handler):
        try:
            self.shared_data.manual_scan_trigger.clear()
            self.shared_data.manual_bruteforce_trigger.clear()
            self.shared_data.manual_vulnscan_trigger.clear()
            self.shared_data.manual_steal_trigger.clear()
            self.shared_data.orchestrator_should_exit = True
            time.sleep(0.5)
            self.shared_data.orchestrator_should_exit = False
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "stopped", "message": "All operations stopped."}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def portal_list(self, handler):
        try:
            portals = []
            base = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'web', 'portals')
            for d in [base, os.path.join(base, 'custom')]:
                source = 'builtin' if d == base else 'custom'
                if os.path.isdir(d):
                    for f in sorted(os.listdir(d)):
                        if f.endswith('.html'):
                            portals.append({"name": f, "source": source})
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"portals": portals}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"portals": [{"name":"generic.html","source":"builtin"}], "error": "Internal error"}).encode('utf-8'))

    def upload_portal(self, handler):
        try:
            content_length = int(handler.headers.get('Content-Length', 0))
            body = handler.rfile.read(content_length)
            import cgi
            import io
            content_type = handler.headers.get('Content-Type', '')
            _, pdict = cgi.parse_header(content_type)
            pdict['boundary'] = pdict.get('boundary', b'')
            parsed = cgi.parse_multipart(io.BytesIO(body), pdict)
            file_data = parsed.get('file', [None])[0]
            filename = parsed.get('filename', [None])[0]
            if not filename or not file_data:
                raise ValueError("No file provided")
            if not filename.endswith('.html'):
                raise ValueError("Only .html files allowed")
            safe_name = os.path.basename(filename)
            dest = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'web', 'portals', 'custom', safe_name)
            with open(dest, 'wb') as f:
                f.write(file_data)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success", "filename": safe_name}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def delete_portal(self, handler):
        try:
            content_length = int(handler.headers.get('Content-Length', 0))
            post_data = json.loads(handler.rfile.read(content_length))
            filename = post_data.get('filename', '')
            if not filename or '..' in filename:
                raise ValueError("Invalid filename")
            dest = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'web', 'portals', 'custom', filename)
            if os.path.isfile(dest):
                os.remove(dest)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))

    def evil_clients(self, handler):
        try:
            clients = []
            leases = "/var/lib/misc/dnsmasq.leases"
            if os.path.exists(leases):
                with open(leases, 'r') as f:
                    for line in f:
                        parts = line.strip().split()
                        if len(parts) >= 4:
                            clients.append({
                                "timestamp": parts[0],
                                "mac": parts[1],
                                "ip": parts[2],
                                "hostname": parts[3] if parts[3] != '*' else 'Unknown'
                            })
            if not clients:
                arp_result = subprocess.run(['arp', '-a'], capture_output=True, text=True, timeout=5)
                for line in arp_result.stdout.split('\n'):
                    parts = line.split()
                    if len(parts) >= 2:
                        clients.append({"ip": parts[0].strip('()'), "mac": parts[1] if '(' not in parts[1] else parts[1], "hostname": ""})
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"clients": clients}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"clients": [], "error": "Internal error"}).encode('utf-8'))

    def loot_monitor_data(self, handler):
        try:
            loot = getattr(self.shared_data, 'loot_monitor_instance', None)
            if loot and hasattr(loot, 'get_data'):
                data = loot.get_data()
            else:
                evil = getattr(self.shared_data, 'evil_ap_instance', None)
                if evil and hasattr(evil, 'loot_monitor') and evil.loot_monitor:
                    data = evil.loot_monitor.get_data()
                else:
                    data = {"dns": [], "http": [], "devices": []}
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps(data).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"dns": [], "http": [], "devices": []}).encode('utf-8'))

    def scan_targets(self, handler):
        try:
            interface = handler.path.split('?iface=')[1] if '?iface=' in handler.path else 'wlan0'
            import re as _re
            if not _re.match(r'^[a-zA-Z0-9_.-]+$', interface):
                raise ValueError("Invalid interface")
            result = subprocess.run(['sudo', 'iw', 'dev', interface, 'scan'], capture_output=True, text=True, timeout=15)
            networks = []
            current = {}
            for line in result.stdout.split('\n'):
                if 'BSS ' in line and '(' in line:
                    if current.get('ssid'):
                        networks.append(current)
                    current = {'bssid': line.split()[1]}
                if 'freq:' in line:
                    pass
                if 'signal:' in line:
                    parts = line.split()
                    if len(parts) >= 2:
                        current['signal'] = parts[1]
                if 'SSID:' in line:
                    current['ssid'] = line.split('SSID:')[1].strip()
                if 'DS Parameter set: channel' in line:
                    parts = line.split()
                    if len(parts) >= 4:
                        try:
                            current['channel'] = int(parts[3])
                        except ValueError:
                            pass
            if current.get('ssid'):
                networks.append(current)
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"networks": networks}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"networks": [], "error": "Internal error"}).encode('utf-8'))

    def wpa_validate_status(self, handler):
        try:
            status = "idle"
            evil = getattr(self.shared_data, 'evil_ap_instance', None)
            if evil and getattr(evil, 'running', False) and getattr(evil, 'wpa_validate_enabled', False):
                status = "running"
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": status}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "error": "Internal error"}).encode('utf-8'))

    def stop_evil_clone(self, handler):
        try:
            clone = getattr(self.shared_data, 'evil_clone_instance', None)
            if clone:
                clone.stop()
            handler.send_response(200)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "success"}).encode('utf-8'))
        except Exception as e:
            handler.send_response(500)
            handler.send_header("Content-type", "application/json")
            handler.end_headers()
            handler.wfile.write(json.dumps({"status": "error", "message": "Internal error"}).encode('utf-8'))



