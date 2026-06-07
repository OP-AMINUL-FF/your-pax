# bluetooth_nap.py
# Bluetooth NAP (Network Access Point) daemon for your-pax
# Creates a PAN bridge (pan0) so phones can connect via Bluetooth
# and access the Web UI at http://192.168.4.1:8000

import subprocess
import time
import logging
import signal
import sys
import os
import threading
from logger import Logger

logger = Logger(name="bluetooth_nap.py", level=logging.DEBUG)

BRIDGE_IP = "192.168.4.1"
BRIDGE_NETMASK = "24"
BRIDGE_IFACE = "pan0"
DHCP_RANGE_START = "192.168.4.2"
DHCP_RANGE_END = "192.168.4.250"
NAP_ALIAS = "your-pax"
NAP_UUID = "00001116-0000-1000-8000-00805F9B34FB"  # NAP Profile UUID

class BluetoothNAP:
    def __init__(self):
        self.running = False
        self.dnsmasq_pid = None
        self.bnep_interfaces = set()
        self._bnep_lock = threading.Lock()
        self.mainloop = None
        self.bridge_up = False
        self.dnsmasq_running = False

    def _run_cmd(self, cmd, check=True, timeout=10):
        """Run a command and return result."""
        try:
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
            if check and result.returncode != 0:
                logger.error(f"Command failed: {' '.join(cmd)}: {result.stderr}")
                return None
            return result.stdout.strip()
        except subprocess.TimeoutExpired:
            logger.error(f"Command timed out: {' '.join(cmd)}")
            return None
        except Exception as e:
            logger.error(f"Error running {' '.join(cmd)}: {e}")
            return None

    def setup_bluetooth(self):
        """Configure Bluetooth controller via D-Bus."""
        try:
            import dbus
        except ImportError:
            logger.error("dbus module not available. Install python3-dbus.")
            return False
        logger.info("Setting up Bluetooth controller...")
        try:
            bus = dbus.SystemBus()
            adapter = dbus.Interface(bus.get_object('org.bluez', '/org/bluez/hci0'),
                                     'org.freedesktop.DBus.Properties')
            adapter.Set('org.bluez.Adapter1', 'Powered', dbus.Boolean(1))
            adapter.Set('org.bluez.Adapter1', 'Discoverable', dbus.Boolean(1))
            adapter.Set('org.bluez.Adapter1', 'Pairable', dbus.Boolean(1))
            adapter.Set('org.bluez.Adapter1', 'Alias', NAP_ALIAS)
            logger.info(f"Bluetooth alias set to '{NAP_ALIAS}'")
            return True
        except Exception as e:
            logger.error(f"Failed to configure Bluetooth: {e}")
            return False

    def setup_bridge(self):
        """Create pan0 bridge interface."""
        logger.info("Setting up bridge interface...")
        self._run_cmd(['brctl', 'addbr', BRIDGE_IFACE], check=False)
        self._run_cmd(['ip', 'addr', 'add', f'{BRIDGE_IP}/{BRIDGE_NETMASK}', 'dev', BRIDGE_IFACE])
        self._run_cmd(['ip', 'link', 'set', BRIDGE_IFACE, 'up'])
        self.bridge_up = True
        logger.info(f"Bridge {BRIDGE_IFACE} created with IP {BRIDGE_IP}")
        return True

    def start_dhcp(self):
        """Start dnsmasq DHCP server on bridge."""
        logger.info("Starting dnsmasq on bridge...")
        if self.dnsmasq_running:
            self.stop_dhcp()
        self._run_cmd(['dnsmasq', '--strict-order',
                       f'--interface={BRIDGE_IFACE}',
                       f'--dhcp-range={DHCP_RANGE_START},{DHCP_RANGE_END},12h',
                       f'--dhcp-option=3,{BRIDGE_IP}',
                       '--dhcp-option=6,0.0.0.0',
                       '--port=0',
                       '--no-resolv',
                       '--bind-interfaces',
                       '--dhcp-leasefile=/tmp/bt_leases.log',
                       '--pid-file=/var/run/bt-nap-dnsmasq.pid'])
        self.dnsmasq_running = True
        logger.info("dnsmasq started on bridge")
        return True

    def stop_dhcp(self):
        """Stop dnsmasq DHCP server."""
        self._run_cmd(['pkill', '-f', 'bt-nap-dnsmasq'], check=False)
        pid_file = '/var/run/bt-nap-dnsmasq.pid'
        if os.path.exists(pid_file):
            self._run_cmd(['kill', f"$(cat {pid_file})"], check=False)
            try:
                os.remove(pid_file)
            except OSError:
                pass
        self.dnsmasq_running = False

    def register_nap_service(self):
        """Register NAP service via BlueZ D-Bus API."""
        try:
            import dbus
        except ImportError:
            logger.warning("dbus not available; skipping NAP service registration (bridge-only mode).")
            return False
        logger.info("Registering NAP service...")
        try:
            bus = dbus.SystemBus()
            manager = dbus.Interface(bus.get_object('org.bluez', '/'),
                                     'org.freedesktop.DBus.ObjectManager')
            managed_objects = manager.GetManagedObjects()
            profile_manager_path = None
            for path, interfaces in managed_objects.items():
                if 'org.bluez.ProfileManager1' in interfaces:
                    profile_manager_path = path
                    break
            if not profile_manager_path:
                logger.error("ProfileManager1 not found on D-Bus")
                return False
            profile_manager = dbus.Interface(
                bus.get_object('org.bluez', profile_manager_path),
                'org.bluez.ProfileManager1')
            service_record = (
                f"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                f"<record>"
                f"  <attribute id=\"0x0001\">"
                f"    <uuid value=\"{NAP_UUID}\"/>"
                f"  </attribute>"
                f"  <attribute id=\"0x0004\">"
                f"    <sequence>"
                f"      <uuid value=\"0x0100\"/>"
                f"    </sequence>"
                f"  </attribute>"
                f"  <attribute id=\"0x0005\">"
                f"    <sequence>"
                f"      <uuid value=\"{NAP_UUID}\"/>"
                f"    </sequence>"
                f"  </attribute>"
                f"  <attribute id=\"0x0006\">"
                f"    <sequence>"
                f"      <uuid value=\"{NAP_UUID}\"/>"
                f"    </sequence>"
                f"  </attribute>"
                f"  <attribute id=\"0x0009\">"
                f"    <uint16 value=\"0x0100\"/>"
                f"  </attribute>"
                f"  <attribute id=\"0x0100\">"
                f"    <text value=\"{NAP_ALIAS}\"/>"
                f"  </attribute>"
                f"</record>"
            )
            profile_manager.RegisterProfile(NAP_UUID, service_record, {})
            logger.info("NAP service registered via D-Bus")
            return True
        except Exception as e:
            logger.error(f"Failed to register NAP service: {e}")
            return False

    def watcher_thread(self):
        """Background thread that monitors for bnep interfaces and attaches them to bridge."""
        while self.running:
            try:
                bnep_found = set()
                for iface in os.listdir('/sys/class/net/'):
                    if iface.startswith('bnep'):
                        bnep_found.add(iface)
                with self._bnep_lock:
                    for iface in bnep_found:
                        if iface not in self.bnep_interfaces:
                            logger.info(f"New client connected via {iface}, attaching to bridge")
                            self._run_cmd(['brctl', 'addif', BRIDGE_IFACE, iface], check=False)
                            self._run_cmd(['ip', 'link', 'set', iface, 'up'], check=False)
                            self.bnep_interfaces.add(iface)
                    gone = self.bnep_interfaces - bnep_found
                    for iface in gone:
                        logger.info(f"Client disconnected from {iface}")
                        self._run_cmd(['brctl', 'delif', BRIDGE_IFACE, iface], check=False)
                    self.bnep_interfaces = bnep_found
            except Exception as e:
                logger.error(f"Watcher error: {e}")
            time.sleep(2)

    def cleanup(self):
        """Clean shutdown of all resources."""
        logger.info("Cleaning up Bluetooth NAP...")
        self.running = False
        self.stop_dhcp()
        with self._bnep_lock:
            for iface in list(self.bnep_interfaces):
                self._run_cmd(['brctl', 'delif', BRIDGE_IFACE, iface], check=False)
                self._run_cmd(['ip', 'link', 'set', iface, 'down'], check=False)
        if self.bridge_up:
            self._run_cmd(['ip', 'link', 'set', BRIDGE_IFACE, 'down'], check=False)
            self._run_cmd(['brctl', 'delbr', BRIDGE_IFACE], check=False)
            self.bridge_up = False
        logger.info("Cleanup complete")

    def get_status(self):
        """Return current NAP status as dict."""
        with self._bnep_lock:
            clients = len(self.bnep_interfaces)
        return {
            "active": self.running,
            "bridge_ip": BRIDGE_IP,
            "dhcp_range": f"{DHCP_RANGE_START} - {DHCP_RANGE_END}",
            "connected_clients": clients,
            "bridge_interface": BRIDGE_IFACE,
            "alias": NAP_ALIAS
        }

    def run(self):
        """Main entry point - setup and start Bluetooth NAP."""
        self.running = True
        logger.info("Starting Bluetooth NAP...")
        try:
            self.setup_bluetooth()
            self.setup_bridge()
            self.start_dhcp()
            self.register_nap_service()
        except Exception as e:
            logger.error(f"Failed to initialize Bluetooth NAP: {e}")
            self.cleanup()
            sys.exit(1)
        watcher = threading.Thread(target=self.watcher_thread, daemon=True)
        watcher.start()
        logger.info(f"Bluetooth NAP running at {BRIDGE_IP}:8000")
        try:
            from dbus.mainloop.glib import DBusGMainLoop
            from gi.repository import GLib
            DBusGMainLoop(set_as_default=True)
            self.mainloop = GLib.MainLoop()
        except ImportError:
            logger.warning("GLib/D-Bus mainloop not available, running without event loop.")
            self.mainloop = None
        if self.mainloop:
            try:
                self.mainloop.run()
            except KeyboardInterrupt:
                pass
        else:
            while self.running:
                time.sleep(10)
        self.cleanup()

def main():
    nap = BluetoothNAP()
    def handle_signal(sig, frame):
        logger.info("Signal received, shutting down...")
        nap.cleanup()
        sys.exit(0)
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)
    nap.run()

if __name__ == "__main__":
    main()
