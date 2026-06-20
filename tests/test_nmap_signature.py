import unittest
from unittest.mock import MagicMock, patch
import os
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from actions.nmap_vuln_scanner import NmapVulnScanner

class TestNmapSignature(unittest.TestCase):
    def setUp(self):
        self.mock_shared = MagicMock()
        self.mock_shared.vuln_summary_file = os.devnull
        self.mock_shared.vulnerabilities_dir = os.path.dirname(os.devnull)

    def test_execute_accepts_old_signature(self):
        scanner = NmapVulnScanner(self.mock_shared)
        try:
            result = scanner.execute("192.168.1.1", {"Ports": "80", "Hostnames": "test", "MAC Address": "aa:bb:cc:dd:ee:ff"}, "NmapVulnScanner")
        except TypeError:
            self.fail("execute() should accept (ip, row, status_key) signature")

    def test_execute_accepts_standard_signature(self):
        scanner = NmapVulnScanner(self.mock_shared)
        try:
            result = scanner.execute("192.168.1.1", {"Ports": "80", "Hostnames": "test", "MAC Address": "aa:bb:cc:dd:ee:ff"}, "NmapVulnScanner", port="80")
        except TypeError:
            self.fail("execute() should accept (ip, row, status_key, port=) signature")

if __name__ == '__main__':
    unittest.main()
