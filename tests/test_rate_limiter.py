import unittest
import time
import os
import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from utils import RateLimiter

class TestRateLimiter(unittest.TestCase):
    def setUp(self):
        self.limiter = RateLimiter(max_requests=5, window=60)

    def test_allows_within_limit(self):
        for _ in range(5):
            self.assertTrue(self.limiter.is_allowed("1.2.3.4"))

    def test_blocks_over_limit(self):
        for _ in range(5):
            self.limiter.is_allowed("1.2.3.4")
        self.assertFalse(self.limiter.is_allowed("1.2.3.4"))

    def test_different_ips_independent(self):
        for _ in range(5):
            self.limiter.is_allowed("1.2.3.4")
        self.assertTrue(self.limiter.is_allowed("5.6.7.8"))

    def test_reset_on_window_expiry(self):
        limiter = RateLimiter(max_requests=1, window=0.1)
        self.assertTrue(limiter.is_allowed("1.2.3.4"))
        self.assertFalse(limiter.is_allowed("1.2.3.4"))
        time.sleep(0.15)
        self.assertTrue(limiter.is_allowed("1.2.3.4"))

if __name__ == '__main__':
    unittest.main()
