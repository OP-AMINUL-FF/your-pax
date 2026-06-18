#!/bin/bash
#
# monitor_fd.sh - Monitor file descriptor usage on Raspberry Pi Zero 2W
#
# This script checks /proc/sys/fs/file-nr for open FD count and warns
# when usage exceeds 80% of the system max. Can optionally restart the
# main service if FD pressure is critical.
#
# Used as ExecStartPost in the your-pax systemd service.
#
set -o nounset
set -o errexit
set -o pipefail

readonly SCRIPT_NAME="$(basename "$0")"
readonly SERVICE_NAME="${MONITOR_SERVICE:-your-pax.service}"
readonly CHECK_INTERVAL="${MONITOR_INTERVAL:-30}"
readonly WARN_THRESHOLD="${MONITOR_WARN_THRESHOLD:-80}"
readonly CRIT_THRESHOLD="${MONITOR_CRIT_THRESHOLD:-95}"
readonly LOG_TAG="monitor_fd"
readonly PID_FILE="/var/run/monitor_fd.pid"

# Cleanup on exit
cleanup() {
    rm -f "$PID_FILE"
    logger -t "$LOG_TAG" "monitor_fd.sh exiting cleanly"
}

# Trap SIGTERM (systemd stop), SIGINT, and EXIT
trap 'cleanup; exit 0' SIGTERM SIGINT
trap cleanup EXIT

# Write PID file
echo $$ > "$PID_FILE"

# Main monitoring loop
while true; do
    if [[ ! -r /proc/sys/fs/file-nr ]]; then
        logger -t "$LOG_TAG" "ERROR: Cannot read /proc/sys/fs/file-nr"
        sleep "$CHECK_INTERVAL"
        continue
    fi

    read -r allocated free max_fds <<< "$(</proc/sys/fs/file-nr)"

    if [[ -z "$max_fds" || "$max_fds" -eq 0 ]]; then
        logger -t "$LOG_TAG" "ERROR: Invalid value from /proc/sys/fs/file-nr"
        sleep "$CHECK_INTERVAL"
        continue
    fi

    used=$((allocated - free))
    pct=$((used * 100 / max_fds))

    if (( pct >= CRIT_THRESHOLD )); then
        logger -t "$LOG_TAG" "CRITICAL: FD usage ${pct}% (${used}/${max_fds}) — restarting ${SERVICE_NAME}"
        systemctl restart "$SERVICE_NAME" 2>/dev/null || \
            logger -t "$LOG_TAG" "ERROR: Failed to restart ${SERVICE_NAME}"
    elif (( pct >= WARN_THRESHOLD )); then
        logger -t "$LOG_TAG" "WARNING: FD usage ${pct}% (${used}/${max_fds})"
    else
        logger -t "$LOG_TAG" "OK: FD usage ${pct}% (${used}/${max_fds})"
    fi

    sleep "$CHECK_INTERVAL"
done
