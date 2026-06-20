import logging
from logger import Logger

logger = Logger(name="wifi_scanner.py", level=logging.DEBUG)
b_class = "WiFiScanner"
b_module = "wifi_scanner"
b_status = "wifi_scanner"
b_port = 0
b_parent = None


class WiFiScanner:
    def __init__(self, shared_data):
        self.shared_data = shared_data

    def scan(self, iface=None):
        try:
            from actions.scanning import NetworkScanner
            scanner = NetworkScanner(self.shared_data)
            network = scanner.get_network()
            if not network:
                return []
            scan_result = scanner.ScanPorts(
                scanner, network,
                self.shared_data.config.get("portstart", 1),
                self.shared_data.config.get("portend", 1000),
                self.shared_data.config.get("portlist", [])
            )
            ip_data, open_ports, all_ports, csv_file, netkb, alive_ips = scan_result.start()
            networks = []
            for ip in ip_data.ip_list:
                networks.append({
                    "ip": ip,
                    "ports": open_ports.get(ip, []),
                    "alive": ip in alive_ips
                })
            return networks
        except Exception as e:
            logger.error(f"WiFiScanner scan failed: {e}")
            return []

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
