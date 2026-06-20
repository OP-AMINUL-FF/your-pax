import unittest
from unittest.mock import MagicMock, patch
import os
import sys
import tempfile
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from actions.steal_files_ftp import StealFilesFTP
from actions.steal_files_telnet import StealFilesTelnet

class TestPathTraversal(unittest.TestCase):
    def setUp(self):
        self.mock_shared = MagicMock()
        self.mock_shared.datastolendir = tempfile.mkdtemp()
        self.mock_shared.steal_file_extensions = ['.txt']
        self.mock_shared.steal_file_names = []
        self.mock_shared.orchestrator_should_exit = False

    def test_ftp_traversal_blocked(self):
        ftp = StealFilesFTP.__new__(StealFilesFTP)
        ftp.shared_data = self.mock_shared
        mock_conn = MagicMock()
        with tempfile.TemporaryDirectory() as tmpdir:
            ftp.steal_item(mock_conn, '../../etc/passwd', tmpdir)
            mock_conn.retrbinary.assert_not_called()

    def test_ftp_safe_path(self):
        ftp = StealFilesFTP.__new__(StealFilesFTP)
        ftp.shared_data = self.mock_shared
        mock_conn = MagicMock()
        with tempfile.TemporaryDirectory() as tmpdir:
            ftp.steal_item(mock_conn, '/home/user/file.txt', tmpdir)
            mock_conn.retrbinary.assert_called_once()

    def test_telnet_traversal_blocked(self):
        tn = StealFilesTelnet.__new__(StealFilesTelnet)
        tn.shared_data = self.mock_shared
        mock_conn = MagicMock()
        with tempfile.TemporaryDirectory() as tmpdir:
            tn.steal_item(mock_conn, '../../etc/shadow', tmpdir)
            mock_conn.write.assert_not_called()

    def test_telnet_safe_path(self):
        tn = StealFilesTelnet.__new__(StealFilesTelnet)
        tn.shared_data = self.mock_shared
        mock_conn = MagicMock()
        with tempfile.TemporaryDirectory() as tmpdir:
            tn.steal_item(mock_conn, '/var/www/file.txt', tmpdir)
            mock_conn.write.assert_called_once()

if __name__ == '__main__':
    unittest.main()
