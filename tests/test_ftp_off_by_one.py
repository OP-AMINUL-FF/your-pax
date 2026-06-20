import unittest
from unittest.mock import MagicMock, patch
import os
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from actions.ftp_connector import FTPConnector

class TestFTPOffByOne(unittest.TestCase):
    def setUp(self):
        self.mock_shared = MagicMock()
        self.mock_shared.usersfile = os.path.join(os.path.dirname(__file__), '..', 'data', 'users.txt')
        self.mock_shared.passwordsfile = os.path.join(os.path.dirname(__file__), '..', 'data', 'passwords.txt')

    @patch('actions.base_connector.BaseConnector.__init__', return_value=None)
    def test_get_total_tasks_no_off_by_one(self, mock_init):
        ftp = FTPConnector.__new__(FTPConnector)
        ftp.shared_data = self.mock_shared
        ftp.users = ['a', 'b', 'c']
        ftp.passwords = ['x', 'y']
        result = ftp._get_total_tasks()
        self.assertEqual(result, 6, "Should be 3*2=6, not 7")

    @patch('actions.base_connector.BaseConnector.__init__', return_value=None)
    def test_get_total_tasks_empty(self, mock_init):
        ftp = FTPConnector.__new__(FTPConnector)
        ftp.shared_data = self.mock_shared
        ftp.users = []
        ftp.passwords = []
        result = ftp._get_total_tasks()
        self.assertEqual(result, 0, "Should be 0 for empty lists")

if __name__ == '__main__':
    unittest.main()
