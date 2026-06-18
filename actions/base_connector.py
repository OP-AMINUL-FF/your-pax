import os
import csv
import threading
import logging
from queue import Queue
from shared import SharedData
from logger import Logger

logger = Logger(name="base_connector.py", level=logging.DEBUG)


class BaseConnector:
    PORT = None
    SERVICE_NAME = ""
    CSV_HEADER = ['MAC Address', 'IP Address', 'Hostname', 'User', 'Password', 'Port']

    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.load_scan_file()
        with open(shared_data.usersfile, "r") as f:
            self.users = f.read().splitlines()
        with open(shared_data.passwordsfile, "r") as f:
            self.passwords = f.read().splitlines()
        self.lock = threading.Lock()
        self.results = []
        self.queue = Queue()
        self._init_output_file()

    @property
    def output_file(self):
        raise NotImplementedError

    def connect(self, ip, user, password):
        raise NotImplementedError

    def load_scan_file(self):
        with open(self.shared_data.netkbfile, 'r') as f:
            reader = csv.DictReader(f)
            scan_rows = list(reader)
        self.scan = [row for row in scan_rows if "Ports" in row and str(self.PORT) in row.get("Ports", "")]

    def _init_output_file(self):
        if not os.path.exists(self.output_file):
            logger.info(f"File {self.output_file} does not exist. Creating...")
            with open(self.output_file, "w", newline='') as f:
                writer = csv.writer(f)
                writer.writerow(self.CSV_HEADER)

    def _get_total_tasks(self):
        return len(self.users) * len(self.passwords)

    def _get_queue_items(self, ip, port, mac, hostname):
        for user in self.users:
            for password in self.passwords:
                yield (ip, user, password, mac, hostname, port)

    def worker(self, success_flag):
        attempt_count = 0
        while not self.queue.empty():
            if self.shared_data.orchestrator_should_exit:
                logger.info("Orchestrator exit signal received, stopping worker thread.")
                break
            item = self.queue.get()
            self._process_queue_item(item, success_flag)
            self.queue.task_done()
            attempt_count += 1
            if attempt_count % 10 == 0:
                print(f"[*] {self.SERVICE_NAME}: {attempt_count} attempts completed...")

    def _process_queue_item(self, item, success_flag):
        ip, user, password, mac, hostname, port = item
        if self.connect(ip, user, password):
            with self.lock:
                self.results.append([mac, ip, hostname, user, password, port])
                logger.success(f"Found credentials for IP: {ip} | User: {user}")
                self.save_results()
                self.removeduplicates()
                success_flag[0] = True

    def run_bruteforce(self, ip, port):
        self.load_scan_file()
        try:
            mac = next(row['MAC Address'] for row in self.scan if row['IPs'] == ip)
            hostname = next(row['Hostnames'] for row in self.scan if row['IPs'] == ip)
        except StopIteration:
            mac = ''
            hostname = ''

        for item in self._get_queue_items(ip, port, mac, hostname):
            if self.shared_data.orchestrator_should_exit:
                logger.info("Orchestrator exit signal received, stopping bruteforce task addition.")
                return False, []
            self.queue.put(item)

        success_flag = [False]
        threads = []
        print(f"[*] Brute-forcing {self.SERVICE_NAME} on {ip} ({self._get_total_tasks()} attempts)...")

        for _ in range(8):
            t = threading.Thread(target=self.worker, args=(success_flag,))
            t.start()
            threads.append(t)

        while not self.queue.empty():
            if self.shared_data.orchestrator_should_exit:
                logger.info("Orchestrator exit signal received, stopping bruteforce.")
                while not self.queue.empty():
                    self.queue.get()
                    self.queue.task_done()
                break

        self.queue.join()

        for t in threads:
            t.join()

        self._post_bruteforce(ip, mac, hostname, success_flag)
        return success_flag[0], self.results

    def _post_bruteforce(self, ip, mac, hostname, success_flag):
        pass

    def save_results(self):
        file_exists = os.path.exists(self.output_file)
        with open(self.output_file, 'a', newline='') as f:
            writer = csv.writer(f)
            if not file_exists:
                writer.writerow(self.CSV_HEADER)
            writer.writerows(self.results)
        self.results = []

    def removeduplicates(self):
        if not os.path.exists(self.output_file):
            return
        with open(self.output_file, 'r', newline='') as f:
            reader = csv.reader(f)
            rows = list(reader)
        if not rows:
            return
        header = rows[0]
        seen = set()
        unique_rows = [header]
        for row in rows[1:]:
            key = tuple(row)
            if key not in seen:
                seen.add(key)
                unique_rows.append(row)
        with open(self.output_file, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerows(unique_rows)


class BaseBruteforce:
    def __init__(self, shared_data, connector):
        self.shared_data = shared_data
        self.connector = connector
        logger.info(f"{self.__class__.__name__} initialized.")

    def run(self, ip, port):
        return self.connector.run_bruteforce(ip, port)

    def execute(self, ip, port, row, status_key):
        self.shared_data.orch_status = self.__class__.__name__
        logger.info(f"Executing {self.__class__.__name__} on {ip}:{port}...")
        success, results = self.run(ip, port)
        return 'success' if success else 'failed'
