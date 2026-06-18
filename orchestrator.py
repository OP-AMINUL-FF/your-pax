# orchestrator.py
# Description:
# This file, orchestrator.py, is the heuristic your-pax brain, and it is responsible for coordinating and executing various network scanning and offensive security actions 
# It manages the loading and execution of actions, handles retries for failed and successful actions, 
# and updates the status of the orchestrator.
#
# Key functionalities include:
# - Initializing and loading actions from a configuration file, including network and vulnerability scanners.
# - Managing the execution of actions on network targets, checking for open ports and handling retries based on success or failure.
# - Coordinating the execution of parent and child actions, ensuring actions are executed in a logical order.
# - Running the orchestrator cycle to continuously check for and execute actions on available network targets.
# - Handling and updating the status of the orchestrator, including scanning for new targets and performing vulnerability scans.
# - Implementing threading to manage concurrent execution of actions with a semaphore to limit active threads.
# - Logging events and errors to ensure maintainability and ease of debugging.
# - Handling graceful degradation by managing retries and idle states when no new targets are found.

import json
import importlib
import time
import logging
import sys
import threading
from datetime import datetime, timedelta
from actions.nmap_vuln_scanner import NmapVulnScanner
from init_shared import shared_data
from logger import Logger

logger = Logger(name="orchestrator.py", level=logging.DEBUG)


def load_actions_from_config(shared_data, _logger=None):
    """Load all actions from configuration. Returns (actions, standalone_actions, network_scanner, nmap_vuln_scanner)."""
    actions = []
    standalone_actions = []
    network_scanner = None
    nmap_vuln_scanner = None
    log = _logger or logger

    with open(shared_data.actions_file, 'r') as file:
        actions_config = json.load(file)

    for action_cfg in actions_config:
        module_name = action_cfg["b_module"]
        if module_name == 'scanning':
            module = importlib.import_module(f'actions.{module_name}')
            b_class = getattr(module, 'b_class')
            network_scanner = getattr(module, b_class)(shared_data)
        elif module_name == 'nmap_vuln_scanner':
            nmap_vuln_scanner = NmapVulnScanner(shared_data)
        else:
            module = importlib.import_module(f'actions.{module_name}')
            try:
                b_class = action_cfg["b_class"]
                action_instance = getattr(module, b_class)(shared_data)
                action_instance.action_name = b_class
                action_instance.port = action_cfg.get("b_port")
                action_instance.b_parent_action = action_cfg.get("b_parent")
                if action_instance.port == 0:
                    standalone_actions.append(action_instance)
                else:
                    actions.append(action_instance)
            except AttributeError as e:
                log.error(f"Module {module_name} is missing required attributes: {e}")

    return actions, standalone_actions, network_scanner, nmap_vuln_scanner


class Orchestrator:
    def __init__(self):
        """Initialise the orchestrator"""
        self.shared_data = shared_data
        self.actions = []  # List of actions to be executed
        self.standalone_actions = []  # List of standalone actions to be executed
        self.failed_scans_count = 0  # Count the number of failed scans
        self.network_scanner = None
        self.last_vuln_scan_time = datetime.min  # Set the last vulnerability scan time to the minimum datetime value
        self.load_actions()  # Load all actions from the actions file
        actions_loaded = [action.__class__.__name__ for action in self.actions + self.standalone_actions]  # Get the names of the loaded actions
        logger.info(f"Actions loaded: {actions_loaded}")
        self.semaphore = threading.Semaphore(10)  # Limit the number of active threads to 10

    def load_actions(self):
        """Load all actions from the actions file"""
        self.actions_dir = self.shared_data.actions_dir
        self.actions, self.standalone_actions, self.network_scanner, self.nmap_vuln_scanner = \
            load_actions_from_config(self.shared_data, logger)

    def process_alive_ips(self, current_data):
        """Process all IPs with alive status set to 1"""
        any_action_executed = False
        action_executed_status = None

        for action in self.actions:
            for row in current_data:
                if row["Alive"] != '1':
                    continue
                ip, ports = row["IPs"], row["Ports"].split(';')
                action_key = action.action_name

                if action.b_parent_action is None:
                    with self.semaphore:
                        if self.execute_action(action, ip, ports, row, action_key, current_data):
                            action_executed_status = action_key
                            any_action_executed = True
                            self.shared_data.orch_status = action_executed_status

                            for child_action in self.actions:
                                if child_action.b_parent_action == action_key:
                                    with self.semaphore:
                                        if self.execute_action(child_action, ip, ports, row, child_action.action_name, current_data):
                                            action_executed_status = child_action.action_name
                                            self.shared_data.orch_status = action_executed_status
                                            break
                            break

        return any_action_executed


    def execute_action(self, action, ip, ports, row, action_key, current_data):
        """Execute an action on a target"""
        if hasattr(action, 'port') and str(action.port) not in ports:
            return False

        # Check parent action status
        if action.b_parent_action:
            parent_status = row.get(action.b_parent_action, "")
            if 'success' not in parent_status:
                return False  # Skip child action if parent action has not succeeded

        # Check if the action is already successful and if retries are disabled for successful actions
        if 'success' in row[action_key]:
            if not self.shared_data.retry_success_actions:
                return False
            else:
                try:
                    last_success_time = datetime.strptime(row[action_key].split('_')[1] + "_" + row[action_key].split('_')[2], "%Y%m%d_%H%M%S")
                    if datetime.now() < last_success_time + timedelta(seconds=self.shared_data.success_retry_delay):
                        retry_in_seconds = (last_success_time + timedelta(seconds=self.shared_data.success_retry_delay) - datetime.now()).total_seconds()
                        formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                        logger.warning(f"Skipping action {action.action_name} for {ip}:{action.port} due to success retry delay, retry possible in: {formatted_retry_in}")
                        return False  # Skip if the success retry delay has not passed
                except (ValueError, IndexError) as ve:
                    logger.error(f"Error parsing last success time for {action.action_name}: {ve}")

        last_failed_time_str = row.get(action_key, "")
        if 'failed' in last_failed_time_str:
            try:
                last_failed_time = datetime.strptime(last_failed_time_str.split('_')[1] + "_" + last_failed_time_str.split('_')[2], "%Y%m%d_%H%M%S")
                if datetime.now() < last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay):
                    retry_in_seconds = (last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay) - datetime.now()).total_seconds()
                    formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                    logger.warning(f"Skipping action {action.action_name} for {ip}:{action.port} due to failed retry delay, retry possible in: {formatted_retry_in}")
                    return False  # Skip if the retry delay has not passed
            except (ValueError, IndexError) as ve:
                logger.error(f"Error parsing last failed time for {action.action_name}: {ve}")

        try:
            logger.info(f"Executing action {action.action_name} for {ip}:{action.port}")
            self.shared_data.status_text2 = ip
            result = action.execute(ip, str(action.port), row, action_key)
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            if result == 'success':
                row[action_key] = f'success_{timestamp}'
            else:
                row[action_key] = f'failed_{timestamp}'
            self.shared_data.write_data(current_data)
            return result == 'success'
        except Exception as e:
            logger.error(f"Action {action.action_name} failed: {e}")
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            row[action_key] = f'failed_{timestamp}'
            self.shared_data.write_data(current_data)
            return False

    def execute_standalone_action(self, action, current_data):
        """Execute a standalone action"""
        row = next((r for r in current_data if r["MAC Address"] == "STANDALONE"), None)
        if not row:
            row = {
                "MAC Address": "STANDALONE",
                "IPs": "STANDALONE",
                "Hostnames": "STANDALONE",
                "Ports": "0",
                "Alive": "0"
            }
            current_data.append(row)

        action_key = action.action_name
        if action_key not in row:
            row[action_key] = ""

        # Check if the action is already successful and if retries are disabled for successful actions
        if 'success' in row[action_key]:
            if not self.shared_data.retry_success_actions:
                return False
            else:
                try:
                    last_success_time = datetime.strptime(row[action_key].split('_')[1] + "_" + row[action_key].split('_')[2], "%Y%m%d_%H%M%S")
                    if datetime.now() < last_success_time + timedelta(seconds=self.shared_data.success_retry_delay):
                        retry_in_seconds = (last_success_time + timedelta(seconds=self.shared_data.success_retry_delay) - datetime.now()).total_seconds()
                        formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                        logger.warning(f"Skipping standalone action {action.action_name} due to success retry delay, retry possible in: {formatted_retry_in}")
                        return False  # Skip if the success retry delay has not passed
                except (ValueError, IndexError) as ve:
                    logger.error(f"Error parsing last success time for {action.action_name}: {ve}")

        last_failed_time_str = row.get(action_key, "")
        if 'failed' in last_failed_time_str:
            try:
                last_failed_time = datetime.strptime(last_failed_time_str.split('_')[1] + "_" + last_failed_time_str.split('_')[2], "%Y%m%d_%H%M%S")
                if datetime.now() < last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay):
                    retry_in_seconds = (last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay) - datetime.now()).total_seconds()
                    formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                    logger.warning(f"Skipping standalone action {action.action_name} due to failed retry delay, retry possible in: {formatted_retry_in}")
                    return False  # Skip if the retry delay has not passed
            except (ValueError, IndexError) as ve:
                logger.error(f"Error parsing last failed time for {action.action_name}: {ve}")

        try:
            logger.info(f"Executing standalone action {action.action_name}")
            result = action.execute()
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            if result == 'success':
                row[action_key] = f'success_{timestamp}'
                logger.info(f"Standalone action {action.action_name} executed successfully")
            else:
                row[action_key] = f'failed_{timestamp}'
                logger.error(f"Standalone action {action.action_name} failed")
            self.shared_data.write_data(current_data)
            return result == 'success'
        except Exception as e:
            logger.error(f"Standalone action {action.action_name} failed: {e}")
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            row[action_key] = f'failed_{timestamp}'
            self.shared_data.write_data(current_data)
            return False

    def _run_scan_interruptible(self):
        """Run network scan in a way that can be interrupted."""
        scan_thread = threading.Thread(target=self.network_scanner.scan, daemon=True)
        scan_thread.start()
        while scan_thread.is_alive():
            if self.shared_data.orchestrator_should_exit:
                return False
            scan_thread.join(timeout=1)
        return True

    def run(self):
        """Run the orchestrator cycle to execute actions"""
        #Run the scanner a first time to get the initial data
        self.shared_data.orch_status = "NetworkScanner"
        self.shared_data.status_text2 = "First scan..."
        if not self._run_scan_interruptible():
            return
        self.shared_data.status_text2 = ""
        while not self.shared_data.orchestrator_should_exit:
            current_data = self.shared_data.read_data()
            any_action_executed = False
            action_executed_status = None
            action_retry_pending = False
            any_action_executed = self.process_alive_ips(current_data)
            self.shared_data.write_data(current_data)

            if not any_action_executed:
                self.shared_data.orch_status = "IDLE"
                self.shared_data.status_text2 = ""
                logger.info("No available targets. Running network scan...")
                if self.network_scanner:
                    self.shared_data.orch_status = "NetworkScanner"
                    if not self._run_scan_interruptible():
                        return
                     # Relire les données mises à jour après le scan
                    current_data = self.shared_data.read_data()
                    any_action_executed = self.process_alive_ips(current_data)
                    if self.shared_data.scan_vuln_running:
                        current_time = datetime.now()
                        if current_time >= self.last_vuln_scan_time + timedelta(seconds=self.shared_data.scan_vuln_interval):
                            try:
                                logger.info("Starting vulnerability scans...")
                                for row in current_data:
                                    if row["Alive"] == '1':
                                        ip = row["IPs"]
                                        scan_status = row.get("NmapVulnScanner", "")

                                        # Check success retry delay
                                        if 'success' in scan_status:
                                            try:
                                                last_success_time = datetime.strptime(scan_status.split('_')[1] + "_" + scan_status.split('_')[2], "%Y%m%d_%H%M%S")
                                            except (ValueError, IndexError):
                                                continue
                                            if not self.shared_data.retry_success_actions:
                                                logger.warning(f"Skipping vulnerability scan for {ip} because retry on success is disabled.")
                                                continue
                                            if datetime.now() < last_success_time + timedelta(seconds=self.shared_data.success_retry_delay):
                                                retry_in_seconds = (last_success_time + timedelta(seconds=self.shared_data.success_retry_delay) - datetime.now()).total_seconds()
                                                formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                                                logger.warning(f"Skipping vulnerability scan for {ip} due to success retry delay, retry possible in: {formatted_retry_in}")
                                                continue

                                        # Check failed retry delay
                                        if 'failed' in scan_status:
                                            try:
                                                last_failed_time = datetime.strptime(scan_status.split('_')[1] + "_" + scan_status.split('_')[2], "%Y%m%d_%H%M%S")
                                            except (ValueError, IndexError):
                                                continue
                                            if datetime.now() < last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay):
                                                retry_in_seconds = (last_failed_time + timedelta(seconds=self.shared_data.failed_retry_delay) - datetime.now()).total_seconds()
                                                formatted_retry_in = str(timedelta(seconds=retry_in_seconds))
                                                logger.warning(f"Skipping vulnerability scan for {ip} due to failed retry delay, retry possible in: {formatted_retry_in}")
                                                continue

                                        with self.semaphore:
                                            result = self.nmap_vuln_scanner.execute(ip, row, "NmapVulnScanner")
                                            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                                            if result == 'success':
                                                row["NmapVulnScanner"] = f'success_{timestamp}'
                                            else:
                                                row["NmapVulnScanner"] = f'failed_{timestamp}'
                                            self.shared_data.write_data(current_data)
                                self.last_vuln_scan_time = current_time
                            except Exception as e:
                                logger.error(f"Error during vulnerability scan: {e}")


                else:
                    logger.warning("No network scanner available.")
                self.failed_scans_count += 1
                if self.failed_scans_count >= 1:
                    for action in self.standalone_actions:
                        with self.semaphore:
                            if self.execute_standalone_action(action, current_data):
                                self.failed_scans_count = 0
                                break
                    idle_start_time = datetime.now()
                    idle_end_time = idle_start_time + timedelta(seconds=self.shared_data.scan_interval)
                    while datetime.now() < idle_end_time:
                        if self.shared_data.orchestrator_should_exit:
                            break
                        remaining_time = (idle_end_time - datetime.now()).total_seconds()
                        self.shared_data.orch_status = "IDLE"
                        self.shared_data.status_text2 = ""
                        sys.stdout.write('\x1b[1A\x1b[2K')
                        logger.warning(f"Scanner did not find any new targets. Next scan in: {remaining_time} seconds")
                        time.sleep(1)
                    self.failed_scans_count = 0
                    continue
            else:
                self.failed_scans_count = 0
                action_retry_pending = True

            if action_retry_pending:
                self.failed_scans_count = 0

    def run_triggered(self):
        """Trigger-based orchestrator loop for manual mode."""
        self.shared_data.orch_status = "MANUAL"
        self.shared_data.status_text2 = "Waiting for trigger..."
        while not self.shared_data.orchestrator_should_exit:
            try:
                if self.shared_data.manual_scan_trigger.is_set():
                    self.shared_data.manual_scan_trigger.clear()
                    self.shared_data.orch_status = "NetworkScanner"
                    self.shared_data.status_text2 = "Scan triggered..."
                    logger.info("Trigger: Network scan starting...")
                    if self.network_scanner:
                        self.network_scanner.scan()
                    self.shared_data.status_text2 = "Scan complete"
                    self.shared_data.orch_status = "MANUAL"

                if self.shared_data.manual_bruteforce_trigger.is_set():
                    self.shared_data.manual_bruteforce_trigger.clear()
                    protocol = self.shared_data.manual_bruteforce_protocol
                    self.shared_data.orch_status = "Bruteforce"
                    self.shared_data.status_text2 = f"Bruteforce: {protocol}..."
                    logger.info(f"Trigger: Brute force starting ({protocol})...")
                    current_data = self.shared_data.read_data()
                    for action in self.actions + self.standalone_actions:
                        action_name = action.action_name.lower()
                        if protocol == "all" or protocol in action_name:
                            for row in current_data:
                                if row["Alive"] == '1' or row["MAC Address"] == "STANDALONE":
                                    ip = row["IPs"]
                                    port = action.port if hasattr(action, 'port') and action.port else "0"
                                    action_key = action.action_name
                                    if row["MAC Address"] == "STANDALONE":
                                        result = self.execute_standalone_action(action, current_data)
                                    else:
                                        ports = row["Ports"].split(';')
                                        with self.semaphore:
                                            result = self.execute_action(action, ip, ports, row, action_key, current_data)
                                    if result:
                                        break
                            else:
                                continue
                            break
                    self.shared_data.status_text2 = "Bruteforce complete"
                    self.shared_data.orch_status = "MANUAL"

                if self.shared_data.manual_vulnscan_trigger.is_set():
                    self.shared_data.manual_vulnscan_trigger.clear()
                    self.shared_data.orch_status = "VulnScan"
                    self.shared_data.status_text2 = "Vuln scan triggered..."
                    logger.info("Trigger: Vulnerability scan starting...")
                    current_data = self.shared_data.read_data()
                    if self.nmap_vuln_scanner:
                        for row in current_data:
                            if row["Alive"] == '1':
                                ip = row["IPs"]
                                with self.semaphore:
                                    result = self.nmap_vuln_scanner.execute(ip, row, "NmapVulnScanner")
                                    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                                    row["NmapVulnScanner"] = f'success_{timestamp}' if result == 'success' else f'failed_{timestamp}'
                                self.shared_data.write_data(current_data)
                    self.shared_data.status_text2 = "Vuln scan complete"
                    self.shared_data.orch_status = "MANUAL"

                if self.shared_data.manual_steal_trigger.is_set():
                    self.shared_data.manual_steal_trigger.clear()
                    self.shared_data.orch_status = "Steal"
                    self.shared_data.status_text2 = "Steal triggered..."
                    logger.info("Trigger: Steal files starting...")
                    current_data = self.shared_data.read_data()
                    for action in self.standalone_actions:
                        action_name = action.action_name.lower()
                        if 'steal' in action_name:
                            self.execute_standalone_action(action, current_data)
                            break
                    self.shared_data.status_text2 = "Steal complete"
                    self.shared_data.orch_status = "MANUAL"

            except Exception as e:
                logger.error(f"Error in triggered orchestrator: {e}")
            time.sleep(1)

if __name__ == "__main__":
    orchestrator = Orchestrator()
    orchestrator.run()
