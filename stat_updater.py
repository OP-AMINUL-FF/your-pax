# stat_updater.py
# Lightweight stat updater for non-display mode.
# Replaces display.py when no e-paper is connected.
# No PIL, no fonts, no images, no EPD driver — saves ~15-25MB RAM.

import threading
import time
import os
import csv
import glob
import json
import logging
from logger import Logger
from init_shared import shared_data

logger = Logger(name="stat_updater.py", level=logging.DEBUG)

class StatUpdater:
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.semaphore = threading.Semaphore(10)

    def run(self):
        while not self.shared_data.display_should_exit:
            self.update_shared_data()
            time.sleep(25)
            self.update_vuln_count()
            time.sleep(275)

    def update_shared_data(self):
        with self.semaphore:
            try:
                with open(self.shared_data.livestatusfile, 'r', newline='') as file:
                    reader = csv.DictReader(file)
                    for row in reader:
                        self.shared_data.portnbr = int(row['Total Open Ports'])
                        self.shared_data.targetnbr = int(row['Alive Hosts Count'])
                        self.shared_data.networkkbnbr = int(row['All Known Hosts Count'])
                        self.shared_data.vulnnbr = int(row['Vulnerabilities Count'])
                        break

                crackedpw_files = glob.glob(f"{self.shared_data.crackedpwddir}/*.csv")
                total_passwords = 0
                for file in crackedpw_files:
                    with open(file, 'r') as f:
                        total_passwords += sum(1 for _ in f) - 1
                self.shared_data.crednbr = total_passwords

                total_data = sum([len(files) for r, d, files in os.walk(self.shared_data.datastolendir)])
                self.shared_data.datanbr = total_data

                total_zombies = sum([len(files) for r, d, files in os.walk(self.shared_data.zombiesdir)])
                self.shared_data.zombiesnbr = total_zombies

                try:
                    with open(self.shared_data.actions_file, 'r') as af:
                        actions_data = json.load(af)
                        total_attacks = len(actions_data)
                except Exception:
                    total_attacks = len([f for f in os.listdir(self.shared_data.actions_dir) if f.endswith('.py') and f != '__init__.py'])
                self.shared_data.attacksnbr = total_attacks

                self.shared_data.update_stats()

            except (FileNotFoundError, Exception) as e:
                logger.error(f"Error updating shared data: {e}")

    def update_vuln_count(self):
        with self.semaphore:
            try:
                if not os.path.exists(self.shared_data.vuln_summary_file):
                    with open(self.shared_data.vuln_summary_file, 'w', newline='') as f:
                        writer = csv.writer(f)
                        writer.writerow(["IP", "Hostname", "MAC Address", "Port", "Vulnerabilities"])
                    self.shared_data.vulnnbr = 0
                else:
                    if os.path.exists(self.shared_data.netkbfile):
                        with open(self.shared_data.netkbfile, 'r', newline='') as file:
                            reader = csv.DictReader(file)
                            alive_macs = set(row["MAC Address"] for row in reader if int(row["Alive"]) == 1 and row["MAC Address"] != "STANDALONE")
                    else:
                        alive_macs = set()

                    with open(self.shared_data.vuln_summary_file, 'r', newline='') as file:
                        reader = csv.DictReader(file)
                        all_vulnerabilities = set()
                        for row in reader:
                            mac_address = row["MAC Address"]
                            if mac_address in alive_macs and mac_address != "STANDALONE":
                                vulnerabilities = row["Vulnerabilities"]
                                if vulnerabilities and isinstance(vulnerabilities, str):
                                    all_vulnerabilities.update(vulnerabilities.split("; "))
                        self.shared_data.vulnnbr = len(all_vulnerabilities)

                    if os.path.exists(self.shared_data.livestatusfile):
                        with open(self.shared_data.livestatusfile, 'r', newline='') as livestatus_file:
                            reader = csv.DictReader(livestatus_file)
                            fieldnames = reader.fieldnames
                            rows = list(reader)
                        if rows:
                            rows[0]['Vulnerabilities Count'] = str(self.shared_data.vulnnbr)
                            with open(self.shared_data.livestatusfile, 'w', newline='') as livestatus_file:
                                writer = csv.DictWriter(livestatus_file, fieldnames=fieldnames)
                                writer.writeheader()
                                writer.writerows(rows)
            except Exception as e:
                logger.error(f"Error updating vuln count: {e}")
