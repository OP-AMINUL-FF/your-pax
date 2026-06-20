import socket
import json
import threading
import sys
import os
import traceback
import base64
import time
try:
    import psutil
except ImportError:
    psutil = None

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from logger import Logger
from init_shared import shared_data
from event_bus import event_queue, broadcast_event

logger = Logger("BtSerialServer")

RFCOMM_CHANNEL = 1
MAX_MSG_SIZE = 16384
SOCKET_TIMEOUT = 30.0
CHUNK_SIZE = 10000  # 10KB per chunk for large file transfers

# In-memory chunk buffers for chunked uploads
_upload_buffers = {}

CMD_ROUTER = {}

def register(cmd):
    def wrapper(func):
        CMD_ROUTER[cmd] = func
        return func
    return wrapper


def send_json(conn, req_id, status, data=None, error=None):
    resp = {"id": req_id, "status": status}
    if data is not None:
        resp["data"] = data
    if error is not None:
        resp["error"] = error
    try:
        conn.sendall((json.dumps(resp) + "\n").encode("utf-8"))
    except Exception as e:
        logger.error(f"Send failed: {e}")


@register("ping")
def cmd_ping(params, conn, req_id):
    send_json(conn, req_id, "ok", data="pong")


@register("get_store_data")
def cmd_get_store_data(params, conn, req_id):
    try:
        from utils import load_captured_creds
        data = load_captured_creds(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        logger.error(f"get_store_data failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("get_wifi_status")
def cmd_get_wifi_status(params, conn, req_id):
    try:
        from utils import get_wifi_status
        data = get_wifi_status(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_bluetooth_status")
def cmd_get_bluetooth_status(params, conn, req_id):
    data = {
        "bluetooth_nap_active": shared_data.bluetooth_nap_active,
        "bluetooth_nap_ip": shared_data.bluetooth_nap_ip,
        "pan_connected": shared_data.pan_connected,
    }
    send_json(conn, req_id, "ok", data=data)


@register("wifi_scan_advanced")
def cmd_wifi_scan_advanced(params, conn, req_id):
    try:
        from actions.wifi_scanner import WiFiScanner
        scanner = WiFiScanner(shared_data)
        iface = params.get("iface", shared_data.config.get("wifi_interface", "wlan0"))
        networks = scanner.scan(iface)
        send_json(conn, req_id, "ok", data=networks)
    except Exception as e:
        logger.error(f"wifi_scan_advanced failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("connect_wifi")
def cmd_connect_wifi(params, conn, req_id):
    try:
        ssid = params.get("ssid", "")
        password = params.get("password", "")
        if not ssid:
            send_json(conn, req_id, "error", error="ssid required")
            return
        from utils import connect_to_wifi
        result = connect_to_wifi(shared_data, ssid, password)
        send_json(conn, req_id, "ok", data={"connected": result})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("disconnect_wifi")
def cmd_disconnect_wifi(params, conn, req_id):
    try:
        from utils import disconnect_wifi
        disconnect_wifi(shared_data)
        send_json(conn, req_id, "ok", data={"disconnected": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("handshake_start")
def cmd_handshake_start(params, conn, req_id):
    try:
        bssid = params.get("bssid", "")
        channel = params.get("channel", "1")
        prefix = params.get("prefix", "handshake")
        from actions.wifi_handshake import WiFiHandshake
        hs = WiFiHandshake(shared_data)
        hs.start(bssid, channel, prefix)
        shared_data.handshake_instance = hs
        send_json(conn, req_id, "ok", data={"running": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("handshake_stop")
def cmd_handshake_stop(params, conn, req_id):
    try:
        if shared_data.handshake_instance:
            shared_data.handshake_instance.stop()
            shared_data.handshake_instance = None
        send_json(conn, req_id, "ok", data={"stopped": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("handshake_status")
def cmd_handshake_status(params, conn, req_id):
    running = shared_data.handshake_instance is not None
    output = getattr(shared_data.handshake_instance, "output", "") if running else ""
    send_json(conn, req_id, "ok", data={"running": running, "output": output})


@register("pmkid_start")
def cmd_pmkid_start(params, conn, req_id):
    try:
        bssid = params.get("bssid", "")
        channel = params.get("channel", "1")
        prefix = params.get("prefix", "pmkid")
        from actions.wifi_pmkid import WiFiPMKID
        pmkid = WiFiPMKID(shared_data)
        pmkid.start(bssid, channel, prefix)
        shared_data.pmkid_instance = pmkid
        send_json(conn, req_id, "ok", data={"running": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("pmkid_stop")
def cmd_pmkid_stop(params, conn, req_id):
    try:
        if shared_data.pmkid_instance:
            shared_data.pmkid_instance.stop()
            shared_data.pmkid_instance = None
        send_json(conn, req_id, "ok", data={"stopped": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("pmkid_status")
def cmd_pmkid_status(params, conn, req_id):
    running = shared_data.pmkid_instance is not None
    send_json(conn, req_id, "ok", data={"running": running})


@register("deauth_attack")
def cmd_deauth(params, conn, req_id):
    try:
        bssid = params.get("bssid", "")
        client = params.get("client", "ff:ff:ff:ff:ff:ff")
        count = params.get("count", 10)
        channel = params.get("channel", "1")
        from actions.wifi_deauth import WiFiDeauth
        deauth = WiFiDeauth(shared_data)
        deauth.deauth(bssid, client, count, channel)
        send_json(conn, req_id, "ok", data={"sent": count})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("oneshot_start")
def cmd_oneshot_start(params, conn, req_id):
    try:
        from actions.wifi_oneshot import WiFiOneShot
        oneshot = WiFiOneShot(shared_data)
        oneshot.start(params)
        shared_data.oneshot_instance = oneshot
        send_json(conn, req_id, "ok", data={"running": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("oneshot_stop")
def cmd_oneshot_stop(params, conn, req_id):
    try:
        if shared_data.oneshot_instance:
            shared_data.oneshot_instance.stop()
            shared_data.oneshot_instance = None
        send_json(conn, req_id, "ok", data={"stopped": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("oneshot_status")
def cmd_oneshot_status(params, conn, req_id):
    running = shared_data.oneshot_instance is not None
    output = getattr(shared_data.oneshot_instance, "output_lines", []) if running else []
    send_json(conn, req_id, "ok", data={"running": running, "output": output})


@register("start_evil_ap")
def cmd_start_evil_ap(params, conn, req_id):
    try:
        from utils import start_evil_ap as _start_ap
        result = _start_ap(shared_data, params)
        send_json(conn, req_id, "ok", data={"running": result})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("stop_evil_ap")
def cmd_stop_evil_ap(params, conn, req_id):
    try:
        from utils import stop_evil_ap as _stop_ap
        _stop_ap(shared_data)
        send_json(conn, req_id, "ok", data={"stopped": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("evil_ap_status")
def cmd_evil_ap_status(params, conn, req_id):
    running = shared_data.evil_ap_running if hasattr(shared_data, "evil_ap_running") else False
    data = {"running": running, "ssid": shared_data.config.get("evil_ap_ssid", "")}
    send_json(conn, req_id, "ok", data=data)


@register("evil_clients")
def cmd_evil_clients(params, conn, req_id):
    clients = getattr(shared_data, "evil_clients", [])
    send_json(conn, req_id, "ok", data=clients)


@register("loot_monitor_data")
def cmd_loot_monitor_data(params, conn, req_id):
    try:
        from actions.loot_monitor import LootMonitor
        monitor = LootMonitor(shared_data)
        data = monitor.get_data()
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("trigger_scan")
def cmd_trigger_scan(params, conn, req_id):
    shared_data.manual_scan_trigger.set()
    send_json(conn, req_id, "ok", data={"triggered": True})


@register("trigger_bruteforce")
def cmd_trigger_bruteforce(params, conn, req_id):
    protocol = params.get("protocol", "all")
    shared_data.manual_bruteforce_protocol = protocol
    shared_data.manual_bruteforce_trigger.set()
    send_json(conn, req_id, "ok", data={"triggered": True})


@register("trigger_vulnscan")
def cmd_trigger_vulnscan(params, conn, req_id):
    shared_data.manual_vulnscan_trigger.set()
    send_json(conn, req_id, "ok", data={"triggered": True})


@register("trigger_steal")
def cmd_trigger_steal(params, conn, req_id):
    shared_data.manual_steal_trigger.set()
    send_json(conn, req_id, "ok", data={"triggered": True})


@register("stop_all")
def cmd_stop_all(params, conn, req_id):
    try:
        from utils import stop_all_attacks
        stop_all_attacks(shared_data)
        send_json(conn, req_id, "ok", data={"stopped": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("execute_manual_attack")
def cmd_execute_manual_attack(params, conn, req_id):
    try:
        ip = params.get("ip", "")
        port = params.get("port", "")
        action = params.get("action", "")
        from utils import execute_manual_attack as _exec
        result = _exec(shared_data, ip, port, action)
        send_json(conn, req_id, "ok", data=result)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("load_config")
def cmd_load_config(params, conn, req_id):
    send_json(conn, req_id, "ok", data=shared_data.config)


@register("save_config")
def cmd_save_config(params, conn, req_id):
    try:
        updates = params.get("config", params)
        shared_data.config.update(updates)
        shared_data.save_config()
        send_json(conn, req_id, "ok", data={"saved": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("restore_default_config")
def cmd_restore_default_config(params, conn, req_id):
    try:
        shared_data.restore_default_config()
        send_json(conn, req_id, "ok", data={"restored": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_network_data")
def cmd_get_network_data(params, conn, req_id):
    try:
        from utils import get_network_data
        data = get_network_data(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_netkb_data")
def cmd_get_netkb_data(params, conn, req_id):
    try:
        from utils import get_netkb_data
        data = get_netkb_data(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_netkb_meta")
def cmd_get_netkb_meta(params, conn, req_id):
    try:
        from utils import get_netkb_meta
        data = get_netkb_meta(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("list_credentials")
def cmd_list_credentials(params, conn, req_id):
    try:
        from utils import list_credentials
        data = list_credentials(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("list_files")
def cmd_list_files(params, conn, req_id):
    try:
        from utils import list_loot_files
        data = list_loot_files(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_logs")
def cmd_get_logs(params, conn, req_id):
    try:
        lines = params.get("lines", 500)
        from utils import get_logs
        data = get_logs(shared_data, lines)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("list_interfaces")
def cmd_list_interfaces(params, conn, req_id):
    try:
        from utils import list_interfaces
        data = list_interfaces(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("conflict_status")
def cmd_conflict_status(params, conn, req_id):
    try:
        from actions.conflict_manager import ConflictManager
        cm = ConflictManager(shared_data)
        data = cm.check_all()
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("scan_targets")
def cmd_scan_targets(params, conn, req_id):
    try:
        iface = params.get("iface", shared_data.config.get("wifi_interface", "wlan0"))
        from utils import scan_targets as _scan
        data = _scan(shared_data, iface)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("portal_list")
def cmd_portal_list(params, conn, req_id):
    try:
        from utils import list_portals
        data = list_portals(shared_data)
        send_json(conn, req_id, "ok", data=data)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("delete_portal")
def cmd_delete_portal(params, conn, req_id):
    try:
        filename = params.get("filename", "")
        if not filename:
            send_json(conn, req_id, "error", error="filename required")
            return
        path = os.path.join(shared_data.resourcedir, "portals", filename)
        if os.path.exists(path):
            os.remove(path)
        send_json(conn, req_id, "ok", data={"deleted": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("clear_files")
def cmd_clear_files(params, conn, req_id):
    try:
        from utils import clear_all_files
        clear_all_files(shared_data)
        send_json(conn, req_id, "ok", data={"cleared": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("clear_files_light")
def cmd_clear_files_light(params, conn, req_id):
    try:
        from utils import clear_light_files
        clear_light_files(shared_data)
        send_json(conn, req_id, "ok", data={"cleared": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("initialize_csv")
def cmd_initialize_csv(params, conn, req_id):
    try:
        shared_data.initialize_csv()
        send_json(conn, req_id, "ok", data={"initialized": True})
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("reboot")
def cmd_reboot(params, conn, req_id):
    send_json(conn, req_id, "ok", data={"rebooting": True})
    import subprocess
    subprocess.Popen(["/sbin/reboot"])


@register("shutdown")
def cmd_shutdown(params, conn, req_id):
    send_json(conn, req_id, "ok", data={"shutting_down": True})
    import subprocess
    subprocess.Popen(["/sbin/shutdown", "-h", "now"])


@register("restart_service")
def cmd_restart_service(params, conn, req_id):
    send_json(conn, req_id, "ok", data={"restarting": True})
    import subprocess
    subprocess.Popen(["systemctl", "restart", "your-pax"])


@register("start_orchestrator")
def cmd_start_orchestrator(params, conn, req_id):
    shared_data.orchestrator_should_exit = False
    from orchestrator import start_orchestrator
    start_orchestrator(shared_data)
    send_json(conn, req_id, "ok", data={"started": True})


@register("stop_orchestrator")
def cmd_stop_orchestrator(params, conn, req_id):
    shared_data.orchestrator_should_exit = True
    send_json(conn, req_id, "ok", data={"stopped": True})


@register("backup")
def cmd_backup(params, conn, req_id):
    try:
        from utils import create_backup
        result = create_backup(shared_data)
        send_json(conn, req_id, "ok", data=result)
    except Exception as e:
        send_json(conn, req_id, "error", error=str(e))


@register("get_web_delay")
def cmd_get_web_delay(params, conn, req_id):
    delay = shared_data.config.get("web_delay", 2)
    send_json(conn, req_id, "ok", data={"delay_ms": delay * 1000})


@register("get_csrf_token")
def cmd_get_csrf_token(params, conn, req_id):
    send_json(conn, req_id, "ok", data={"csrf_token": shared_data.csrf_token})


@register("download_file")
def cmd_download_file(params, conn, req_id):
    try:
        path = params.get("path", "")
        if not path:
            send_json(conn, req_id, "error", error="path required")
            return
        full_path = os.path.join(shared_data.lootdir, path)
        if not os.path.exists(full_path):
            send_json(conn, req_id, "error", error="file_not_found")
            return
        size = os.path.getsize(full_path)
        if size <= CHUNK_SIZE:
            with open(full_path, "rb") as f:
                raw = f.read()
            b64 = base64.b64encode(raw).decode("utf-8")
            send_json(conn, req_id, "ok", data={"data": b64, "size": size, "inline": True})
        else:
            chunks = (size + CHUNK_SIZE - 1) // CHUNK_SIZE
            send_json(conn, req_id, "ok", data={"size": size, "chunks": chunks, "chunk_size": CHUNK_SIZE, "inline": False})
    except Exception as e:
        logger.error(f"download_file failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("download_chunk")
def cmd_download_chunk(params, conn, req_id):
    try:
        path = params.get("path", "")
        index = int(params.get("index", 0))
        full_path = os.path.join(shared_data.lootdir, path)
        if not os.path.exists(full_path):
            send_json(conn, req_id, "error", error="file_not_found")
            return
        with open(full_path, "rb") as f:
            f.seek(index * CHUNK_SIZE)
            raw = f.read(CHUNK_SIZE)
        b64 = base64.b64encode(raw).decode("utf-8")
        is_last = len(raw) < CHUNK_SIZE
        send_json(conn, req_id, "ok", data={"index": index, "data": b64, "last": is_last})
    except Exception as e:
        logger.error(f"download_chunk failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("download_store")
def cmd_download_store(params, conn, req_id):
    try:
        path = params.get("path", "")
        if not path:
            send_json(conn, req_id, "error", error="path required")
            return
        full_path = os.path.join(shared_data.storedir, path)
        if not os.path.exists(full_path):
            send_json(conn, req_id, "error", error="file_not_found")
            return
        size = os.path.getsize(full_path)
        if size <= CHUNK_SIZE:
            with open(full_path, "rb") as f:
                raw = f.read()
            b64 = base64.b64encode(raw).decode("utf-8")
            send_json(conn, req_id, "ok", data={"data": b64, "size": size, "inline": True})
        else:
            chunks = (size + CHUNK_SIZE - 1) // CHUNK_SIZE
            send_json(conn, req_id, "ok", data={"size": size, "chunks": chunks, "chunk_size": CHUNK_SIZE, "inline": False})
    except Exception as e:
        logger.error(f"download_store failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("upload_chunk")
def cmd_upload_chunk(params, conn, req_id):
    try:
        cmd = params.get("cmd", "restore")
        filename = params.get("filename", "backup.zip")
        index = int(params.get("index", 0))
        total = int(params.get("total", 0))
        data_b64 = params.get("data", "")
        is_last = params.get("last", False)

        if not data_b64:
            send_json(conn, req_id, "error", error="data required")
            return

        chunk = base64.b64decode(data_b64)
        buf_key = f"{cmd}:{filename}"

        if buf_key not in _upload_buffers:
            _upload_buffers[buf_key] = bytearray(total) if total > 0 else bytearray()

        buf = _upload_buffers[buf_key]
        start = index * CHUNK_SIZE
        buf[start:start + len(chunk)] = chunk

        send_json(conn, req_id, "ok", data={"index": index, "received": len(chunk)})

        if is_last:
            final_data = bytes(buf)
            del _upload_buffers[buf_key]

            if cmd == "restore":
                temp_path = os.path.join(shared_data.tempdir, filename)
                with open(temp_path, "wb") as f:
                    f.write(final_data)
                from utils import restore_backup
                result = restore_backup(shared_data, temp_path)
                # Defer the response — upload_finish sends it
            elif cmd == "upload_portal":
                portals_dir = os.path.join(shared_data.resourcedir, "portals")
                if not os.path.exists(portals_dir):
                    os.makedirs(portals_dir, exist_ok=True)
                path = os.path.join(portals_dir, filename)
                with open(path, "wb") as f:
                    f.write(final_data)
                result = {"uploaded": True}
            else:
                send_json(conn, req_id, "error", error=f"unknown_cmd:{cmd}")
                return

            # Store result for upload_finish
            _upload_buffers[f"_result:{buf_key}"] = result
    except Exception as e:
        logger.error(f"upload_chunk failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


@register("upload_finish")
def cmd_upload_finish(params, conn, req_id):
    try:
        cmd = params.get("cmd", "restore")
        filename = params.get("filename", "backup.zip")
        buf_key = f"{cmd}:{filename}"
        result_key = f"_result:{buf_key}"

        result = _upload_buffers.pop(result_key, None)
        _upload_buffers.pop(buf_key, None)

        if result is None:
            send_json(conn, req_id, "error", error="no_upload_in_progress")
            return

        send_json(conn, req_id, "ok", data=result)
    except Exception as e:
        logger.error(f"upload_finish failed: {e}")
        send_json(conn, req_id, "error", error=str(e))


SYSTEM_STATUS_INTERVAL = 5  # seconds


def _system_status_heartbeat():
    """Background thread that broadcasts system status every N seconds."""
    while True:
        try:
            cpu_usage = 0.0
            cpu_temp = "0°C"
            ram_free_mb = 0
            ram_total_mb = 0
            if psutil:
                cpu_usage = psutil.cpu_percent(interval=0.5)
                cpu_temp = _read_cpu_temp()
                mem = psutil.virtual_memory()
                ram_free_mb = mem.available // (1024 * 1024)
                ram_total_mb = mem.total // (1024 * 1024)
            else:
                # Fallback: read from /proc
                try:
                    with open("/proc/stat") as f:
                        cpu_line = f.readline()
                        parts = cpu_line.split()
                        if len(parts) > 4:
                            total = sum(int(x) for x in parts[1:] if x.isdigit())
                            idle = int(parts[4])
                            cpu_usage = 100.0 * (1.0 - idle / max(total, 1))
                except Exception:
                    pass
                try:
                    with open("/proc/meminfo") as f:
                        for line in f:
                            if line.startswith("MemTotal:"):
                                ram_total_mb = int(line.split()[1]) // 1024
                            elif line.startswith("MemAvailable:"):
                                ram_free_mb = int(line.split()[1]) // 1024
                except Exception:
                    pass
                cpu_temp = _read_cpu_temp()

            broadcast_event("system_status", {
                "cpu_usage": round(cpu_usage, 1),
                "cpu_temp": cpu_temp,
                "ram_free_mb": ram_free_mb,
                "ram_total_mb": ram_total_mb,
            })
        except Exception:
            pass
        time.sleep(SYSTEM_STATUS_INTERVAL)


def _read_cpu_temp():
    try:
        with open("/sys/class/thermal/thermal_zone0/temp") as f:
            raw = f.read().strip()
            temp_c = int(raw) / 1000.0
            return f"{temp_c:.0f}°C"
    except Exception:
        return "N/A"


def handle_client(conn, addr):
    logger.info(f"Client connected: {addr}")
    conn.settimeout(SOCKET_TIMEOUT)
    buffer = ""
    try:
        while True:
            try:
                raw = conn.recv(MAX_MSG_SIZE).decode("utf-8")
            except socket.timeout:
                continue
            except OSError:
                break
            if not raw:
                break
            buffer += raw
            while "\n" in buffer:
                line, buffer = buffer.split("\n", 1)
                line = line.strip()
                if not line:
                    continue
                try:
                    req = json.loads(line)
                except json.JSONDecodeError as e:
                    logger.warning(f"Invalid JSON from {addr}: {e}")
                    send_json(conn, 0, "error", error="invalid_json")
                    continue
                cmd = req.get("cmd", "")
                params = req.get("params", {})
                req_id = req.get("id", 0)
                logger.info(f"CMD {cmd} id={req_id} from {addr}")
                handler = CMD_ROUTER.get(cmd)
                if handler:
                    try:
                        handler(params, conn, req_id)
                    except Exception as e:
                        logger.error(f"Handler {cmd} failed: {e}\n{traceback.format_exc()}")
                        send_json(conn, req_id, "error", error=str(e))
                else:
                    send_json(conn, req_id, "error", error=f"unknown_cmd:{cmd}")
                # Drain pending events after each command
                while not event_queue.empty():
                    try:
                        evt = event_queue.get_nowait()
                        send_json(conn, -1, "event", data=evt)
                    except queue.Empty:
                        break
    except Exception as e:
        logger.error(f"Client error: {e}")
    finally:
        try:
            conn.close()
        except Exception:
            pass
        logger.info(f"Client disconnected: {addr}")


def main():
    # Force shared_data init before starting
    _ = shared_data.config

    server_sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
    server_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server_sock.bind(("", RFCOMM_CHANNEL))
        server_sock.listen(1)
    except OSError as e:
        logger.error(f"Failed to bind RFCOMM channel {RFCOMM_CHANNEL}: {e}")
        logger.error("Make sure Bluetooth is enabled and SPP profile is registered.")
        logger.error("Run: sudo sdptool add SP")
        sys.exit(1)

    logger.info(f"Bluetooth Serial Server listening on RFCOMM channel {RFCOMM_CHANNEL}")
    logger.info(f"Registered commands: {', '.join(sorted(CMD_ROUTER.keys()))}")

    # Start system status heartbeat
    heartbeat = threading.Thread(target=_system_status_heartbeat, daemon=True)
    heartbeat.start()
    logger.info("System status heartbeat started")

    while True:
        try:
            conn, addr = server_sock.accept()
            thread = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            thread.start()
        except Exception as e:
            logger.error(f"Accept failed: {e}")
            break

    server_sock.close()


if __name__ == "__main__":
    main()
