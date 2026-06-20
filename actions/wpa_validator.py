import subprocess
import os
import time
import tempfile
import logging

from logger import Logger

logger = Logger(name="wpa_validator.py", level=logging.DEBUG)
b_class = "WPAValidator"
b_module = "wpa_validator"
b_status = "wpa_validator"
b_port = 0
b_parent = None


class WPAValidator:
    def __init__(self, shared_data):
        self.shared_data = shared_data

    def _run_cmd(self, cmd, timeout=5):
        try:
            proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            return proc.returncode, proc.stdout.strip(), proc.stderr.strip()
        except subprocess.TimeoutExpired:
            return -1, "", "timeout"
        except Exception as e:
            return -1, "", str(e)

    def _sanitize(self, value):
        if not isinstance(value, str):
            return str(value)
        import re as _re
        return _re.sub(r'[^\x20-\x7E]', '', value).replace('"', '\\"').replace('\n', '').strip()

    def validate(self, ssid, password, interface="wlan0", timeout=10):
        if not ssid or not password:
            return {"success": False, "error": "Missing SSID or password"}

        safe_ssid = self._sanitize(ssid)
        safe_password = self._sanitize(password)
        tmp_conf = os.path.join(tempfile.gettempdir(), f"wpa_validate_{int(time.time())}.conf")
        try:
            with open(tmp_conf, "w") as f:
                f.write(f'network={{\n\tssid="{safe_ssid}"\n\tpsk="{safe_password}"\n}}\n')

            self._run_cmd(["sudo", "wpa_cli", "-i", interface, "terminate"], timeout=3)
            time.sleep(0.5)

            rc, out, err = self._run_cmd(
                ["sudo", "wpa_supplicant", "-B", "-i", interface, "-c", tmp_conf],
                timeout=5
            )
            if rc != 0:
                return {"success": False, "error": f"wpa_supplicant failed: {err}"}

            time.sleep(2)
            deadline = time.time() + timeout
            while time.time() < deadline:
                rc, out, err = self._run_cmd(
                    ["sudo", "wpa_cli", "-i", interface, "status"], timeout=3
                )
                if "wpa_state=COMPLETED" in out:
                    self._run_cmd(["sudo", "wpa_cli", "-i", interface, "terminate"], timeout=3)
                    os.remove(tmp_conf)
                    return {"success": True, "error": ""}
                time.sleep(1)

            self._run_cmd(["sudo", "wpa_cli", "-i", interface, "terminate"], timeout=3)
            os.remove(tmp_conf)
            return {"success": False, "error": "Connection timeout"}

        except Exception as e:
            try:
                os.remove(tmp_conf)
            except Exception:
                pass
            return {"success": False, "error": str(e)}

    def execute(self, ip="", port="", row=None, status_key=""):
        return 'success'
