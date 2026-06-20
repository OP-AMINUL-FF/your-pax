#your-pax.py
# This script defines the main execution flow for the your-pax application. It initializes and starts
# various components such as network scanning, display, and web server functionalities. The YourPax 
# class manages the primary operations, including initiating network scans and orchestrating tasks.
# The script handles startup delays, checks for Wi-Fi connectivity, and coordinates the execution of
# scanning and orchestrator tasks using semaphores to limit concurrent threads. It also sets up 
# signal handlers to ensure a clean exit when the application is terminated.

# Functions:
# - handle_exit:  handles the termination of the main, display, and web threads.
# - handle_exit_webserver:  handles the termination of the web server thread.
# - is_wifi_connected: Checks for Wi-Fi connectivity using the nmcli command.

# The script starts by loading shared data configurations, then initializes and sta
# your-pax.py


import threading
import signal
import logging
import time
import sys
import subprocess
from init_shared import shared_data
from display import Display, handle_exit_display
from comment import Commentaireia
from webapp import web_thread, handle_exit_web
from orchestrator import Orchestrator
from logger import Logger

logger = Logger(name="your-pax.py", level=logging.DEBUG)

class YourPax:
    """Main class for YourPax. Manages the primary operations of the application."""
    def __init__(self, shared_data):
        self.shared_data = shared_data
        self.commentaire_ia = Commentaireia(shared_data)
        self.orchestrator_thread = None
        self.orchestrator = None

    def run(self):
        """Main loop for YourPax. Runs in manual mode by default, processes triggers."""
        if hasattr(self.shared_data, 'startup_delay') and self.shared_data.startup_delay > 0:
            logger.info(f"Waiting for startup delay: {self.shared_data.startup_delay} seconds")
            time.sleep(self.shared_data.startup_delay)

        self.shared_data.manual_mode = True
        if self.orchestrator_thread is None or not self.orchestrator_thread.is_alive():
            self.orchestrator = Orchestrator()
            self.orchestrator_thread = threading.Thread(target=self.orchestrator.run_triggered)
            self.orchestrator_thread.start()
            logger.info("Orchestrator thread started in manual/triggered mode.")

        while not self.shared_data.should_exit:
            time.sleep(10)

    def start_orchestrator(self):
        """Start orchestrator in automatic mode."""
        if self.orchestrator_thread is not None and self.orchestrator_thread.is_alive():
            self.shared_data.orchestrator_should_exit = True
            self.orchestrator_thread.join()
            self.shared_data.orchestrator_should_exit = False
        self.shared_data.manual_mode = False
        self.orchestrator = Orchestrator()
        self.orchestrator_thread = threading.Thread(target=self.orchestrator.run)
        self.orchestrator_thread.start()
        logger.info("Orchestrator started in automatic mode.")

    def stop_orchestrator(self):
        """Stop the orchestrator and switch to manual/triggered mode."""
        if self.orchestrator_thread is not None and self.orchestrator_thread.is_alive():
            self.shared_data.orchestrator_should_exit = True
            self.orchestrator_thread.join()
            logger.info("Orchestrator thread stopped.")
        self.shared_data.manual_mode = True
        self.shared_data.orchestrator_should_exit = False
        self.orchestrator = Orchestrator()
        self.orchestrator_thread = threading.Thread(target=self.orchestrator.run_triggered)
        self.orchestrator_thread.start()
        logger.info("Orchestrator restarted in manual/triggered mode.")

    def is_wifi_connected(self):
        """Checks for Wi-Fi connectivity using the nmcli command."""
        result = subprocess.Popen(['nmcli', '-t', '-f', 'active', 'dev', 'wifi'], stdout=subprocess.PIPE, text=True).communicate()[0]
        self.wifi_connected = 'yes' in result
        return self.wifi_connected

    
    @staticmethod
    def start_display():
        """Start the display thread"""
        display = Display(shared_data)
        display_thread = threading.Thread(target=display.run)
        display_thread.start()
        return display_thread

def handle_exit(sig, frame, display_thread, your_pax_thread, web_thread, orchestrator_thread):
    """Handles the termination of the main, display, and web threads."""
    shared_data.should_exit = True
    shared_data.orchestrator_should_exit = True
    shared_data.display_should_exit = True
    shared_data.webapp_should_exit = True
    if display_thread is not None:
        if shared_data.config.get("has_display", True):
            handle_exit_display(sig, frame, display_thread)
        if display_thread.is_alive():
            display_thread.join()
    if orchestrator_thread is not None and orchestrator_thread.is_alive():
        orchestrator_thread.join()
    if your_pax_thread is not None and your_pax_thread.is_alive():
        your_pax_thread.join()
    if web_thread is not None and web_thread.is_alive():
        web_thread.join()
    logger.info("Main loop finished. Clean exit.")
    sys.exit(0)



if __name__ == "__main__":
    logger.info("Starting threads")

    try:
        logger.info("Loading shared data config...")
        shared_data.load_config()

        display_thread = None
        your_pax_thread = None

        # Register signal handlers BEFORE starting threads to close race window
        # Use mutable container so handlers see latest values
        def handle_sig(sig, frame):
            ot = getattr(your_pax, 'orchestrator_thread', None) if 'your_pax' in dir() else None
            handle_exit(sig, frame, display_thread, your_pax_thread, web_thread, ot)
        signal.signal(signal.SIGINT, handle_sig)
        signal.signal(signal.SIGTERM, handle_sig)

        if shared_data.config.get("has_display", True):
            logger.info("Starting display thread...")
            shared_data.display_should_exit = False
            display_thread = YourPax.start_display()
        else:
            logger.info("Display disabled. Starting stat updater (headless mode)...")
            shared_data.display_should_exit = False
            from stat_updater import StatUpdater
            updater = StatUpdater(shared_data)
            display_thread = threading.Thread(target=updater.run)
            display_thread.start()

        logger.info("Starting YourPax thread...")
        your_pax = YourPax(shared_data)
        shared_data.your_pax_instance = your_pax
        your_pax_thread = threading.Thread(target=your_pax.run)
        your_pax_thread.start()

        mode = shared_data.config.get("connection_mode", "web_app")
        if mode in ("web_only", "web_app"):
            logger.info(f"Starting the web server (mode: {mode})...")
            web_thread.start()

    except Exception as e:
        logger.error(f"An exception occurred during thread start: {e}")
        if shared_data.config.get("has_display", True):
            handle_exit_display(signal.SIGINT, None)
        exit(1)
