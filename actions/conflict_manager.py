import logging

from logger import Logger
logger = Logger(name="conflict_manager.py", level=logging.DEBUG)

b_class = "ConflictManager"
b_module = "conflict_manager"
b_status = "conflict_manager"
b_port = None
b_parent = None
b_action = None
b_target = None

class ConflictManager:
    def __init__(self, shared_data):
        self.shared_data = shared_data

    def prepare_for_monitor(self):
        """Auto-resolve conflicts before enabling monitor mode. Returns (ok, msg)."""
        if self._is_running("evil_ap_running"):
            return False, "Evil AP is running. Stop it first."
        if self._is_module_running("oneshot"):
            return False, "WPS attack is running. Stop it first."
        return True, ""

    def prepare_for_evil_ap(self):
        """Auto-resolve conflicts before starting Evil AP. Stops monitor mode. Returns (ok, msg, actions_taken)."""
        actions = []
        if self.shared_data.config.get("enable_monitor_mode", False):
            mon = self.shared_data.monitor_instance
            if mon:
                mon.stop_monitor()
            self.shared_data.config["enable_monitor_mode"] = False
            actions.append("Monitor mode disabled")
        if self._is_module_running("oneshot"):
            return False, "WPS attack is running. Stop it first.", actions
        return True, "", actions

    def prepare_for_wps(self):
        """Auto-resolve conflicts before starting WPS. Returns (ok, msg, actions_taken)."""
        actions = []
        if self.shared_data.config.get("evil_ap_running", False):
            evil = self.shared_data.evil_ap_instance
            if evil:
                evil.stop()
            portal = self.shared_data.captive_portal_instance
            if portal:
                portal.stop()
            self.shared_data.config["evil_ap_running"] = False
            actions.append("Evil AP stopped")
        if self.shared_data.config.get("enable_monitor_mode", False):
            mon = self.shared_data.monitor_instance
            if mon:
                mon.stop_monitor()
            self.shared_data.config["enable_monitor_mode"] = False
            actions.append("Monitor mode disabled")
        return True, "", actions

    def prepare_for_monitor_attack(self, attack_name):
        """Check if a monitor-mode attack (handshake/pmkid/deauth) can start."""
        if not self.shared_data.config.get("enable_monitor_mode", False):
            return False, "Monitor mode is not enabled. Enable it first."
        if attack_name == "handshake" and self._is_module_running("pmkid"):
            return False, "PMKID capture is running on the monitor interface."
        if attack_name == "pmkid" and self._is_module_running("handshake"):
            return False, "Handshake capture is running on the monitor interface."
        return True, ""

    def check_all(self):
        """Return status of all conflict-aware modules."""
        return {
            "monitor_mode": self.shared_data.config.get("enable_monitor_mode", False),
            "evil_ap_running": self.shared_data.config.get("evil_ap_running", False),
            "handshake_running": self._is_module_running("handshake"),
            "pmkid_running": self._is_module_running("pmkid"),
            "oneshot_running": self._is_module_running("oneshot"),
        }

    def _is_running(self, config_key):
        return self.shared_data.config.get(config_key, False)

    def _is_module_running(self, module):
        inst = getattr(self.shared_data, f"{module}_instance", None)
        if inst and hasattr(inst, 'running'):
            return inst.running
        if inst and hasattr(inst, 'capture_process'):
            return inst.capture_process is not None and inst.capture_process.poll() is None
        return False
