# bluez_pairing_agent.py
# BlueZ pairing agent (org.bluez.Agent1) for your-pax.
# Auto-accepts every pairing callback (JustWorks) so a phone can pair
# with the your-pax NAP without any PIN/confirmation prompt on the Pi side.
#
# IO capability: "NoInputNoOutput"  -> BlueZ uses the JustWorks model,
# which means the agent is only ever asked to AuthorizeService and (rarely)
# RequestConfirmation/RequestAuthorization, all of which we accept.
# If a requesting device insists on a PIN (legacy pairing), we reject it,
# because with NoInputNoOutput we have no secure channel to convey one.

import logging
import dbus
import dbus.service
from dbus.mainloop.glib import DBusGMainLoop
from logger import Logger

logger = Logger(name="bluez_pairing_agent.py", level=logging.DEBUG)

AGENT_PATH = "/yourpax/agent"
AGENT_INTERFACE = "org.bluez.Agent1"
AGENT_MANAGER_INTERFACE = "org.bluez.AgentManager1"
OBJECT_MANAGER_INTERFACE = "org.freedesktop.DBus.ObjectManager"

# BlueZ org.bluez.Error.Rejected / Canceled equivalents
_REJECTED = "org.bluez.Error.Rejected"
_CANCELED = "org.bluez.Error.Canceled"


class _PairingFailed(dbus.DBusException):
    """Internal helper to raise named BlueZ errors from agent callbacks."""

    def __init__(self, name, message=""):
        dbus.DBusException.__init__(self, message)
        self._dbus_error_name = name


class PairingAgent(dbus.service.Object):
    """A JustWorks BlueZ Agent1 that auto-accepts all pairing requests."""

    def __init__(self, bus, path=AGENT_PATH):
        self.path = path
        # Auto-accept services by default. If you want to whitelist only the
        # NAP service (UUID 00001116-...), set this to False and filter in
        # AuthorizeService below.
        self.authorize_all_services = True
        dbus.service.Object.__init__(self, bus, path)
        logger.info(f"PairingAgent exported at {path}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="", out_signature="")
    def Release(self):
        """Called by BlueZ when the agent is being released."""
        logger.info("PairingAgent: Release() called by BlueZ")

    @dbus.service.method(AGENT_INTERFACE, in_signature="os", out_signature="")
    def RequestPinCode(self, device, *args):
        # NoInputNoOutput cannot provide a PIN securely. Reject so BlueZ
        # either falls back to JustWorks or the phone uses a different model.
        logger.warning(f"RequestPinCode rejected (NoInputNoOutput): {device}")
        raise _PairingFailed(_REJECTED, "NoInputNoOutput agent cannot supply a PIN")

    @dbus.service.method(AGENT_INTERFACE, in_signature="o", out_signature="u")
    def RequestPasskey(self, device):
        logger.warning(f"RequestPasskey rejected (NoInputNoOutput): {device}")
        raise _PairingFailed(_REJECTED, "NoInputNoOutput agent cannot supply a passkey")

    @dbus.service.method(AGENT_INTERFACE, in_signature="ouay", out_signature="")
    def DisplayPinCode(self, device, pincode, *args):
        # Display-only; nothing to display on a headless Pi, just log.
        logger.info(f"DisplayPinCode: {device} (ignored, headless)")

    @dbus.service.method(AGENT_INTERFACE, in_signature="ouq", out_signature="")
    def DisplayPasskey(self, device, passkey, entered, *args):
        logger.info(f"DisplayPasskey: {device} passkey={passkey} entered={entered}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="ou", out_signature="")
    def RequestConfirmation(self, device, passkey, *args):
        # JustWorks: accept without user confirmation.
        logger.info(f"RequestConfirmation auto-accepted: {device} passkey={passkey}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="os", out_signature="")
    def AuthorizeService(self, device, uuid, *args):
        if self.authorize_all_services:
            logger.info(f"AuthorizeService auto-accepted: {device} uuid={uuid}")
            return
        # NAP service UUID is 00001116-0000-1000-8000-00805F9B34FB.
        # If you whitelist, accept only that uuid here and reject others.
        logger.info(f"AuthorizeService auto-accepted: {device} uuid={uuid}")
        return

    @dbus.service.method(AGENT_INTERFACE, in_signature="o", out_signature="")
    def RequestAuthorization(self, device, *args):
        logger.info(f"RequestAuthorization auto-accepted: {device}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="", out_signature="")
    def Cancel(self):
        logger.info("PairingAgent: Cancel() called")


def find_agent_manager(bus):
    """Locate the org.bluez.AgentManager1 object path via ObjectManager."""
    manager = dbus.Interface(bus.get_object("org.bluez", "/"), OBJECT_MANAGER_INTERFACE)
    for path, interfaces in manager.GetManagedObjects().items():
        if AGENT_MANAGER_INTERFACE in interfaces:
            return path
    return None


def register_agent(bus, capability="NoInputNoOutput", path=AGENT_PATH):
    """Register a PairingAgent as the default BlueZ agent.

    Must be called AFTER DBusGMainLoop(set_as_default=True) and a fresh
    dbus.SystemBus() connection, so callbacks dispatch on the GLib mainloop.

    Returns the PairingAgent instance on success, or None on failure.
    """
    try:
        agent_path = find_agent_manager(bus)
        if not agent_path:
            logger.error("AgentManager1 not found on D-Bus (is bluetoothd running?)")
            return None
        agent_manager = dbus.Interface(
            bus.get_object("org.bluez", agent_path), AGENT_MANAGER_INTERFACE
        )
        agent = PairingAgent(bus, path=path)
        agent_manager.RegisterAgent(path, capability)
        agent_manager.RequestDefaultAgent(path)
        logger.info(
            f"Pairing agent registered at {path} with capability '{capability}' "
            "(JustWorks auto-accept)"
        )
        return agent
    except dbus.exceptions.DBusException as e:
        logger.error(f"Failed to register pairing agent: {e}")
        return None
