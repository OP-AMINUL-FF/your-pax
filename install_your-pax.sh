#!/bin/bash

# your-pax Installation Script
# This script handles the complete installation of your-pax
# Author: your-pax
# Developer: OP AMINUL FF
# Company: OPX
# Version: 1.0 - 071124 - 0954

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Logging configuration
LOG_DIR="/var/log/your-pax_install"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/your-pax_install_$(date +%Y%m%d_%H%M%S).log"
VERBOSE=false

# Global variables
YOUR_PAX_USER="your-pax"
YOUR_PAX_PATH="/home/${YOUR_PAX_USER}/your-pax"
CURRENT_STEP=0
TOTAL_STEPS=7

# Parse command-line flags
SKIP_USB=false
for arg in "$@"; do
    case $arg in
        --help)
            echo "Usage: sudo ./install_your-pax.sh [--skip-usb]"
            echo ""
            echo "Options:"
            echo "  --skip-usb    Skip USB gadget configuration (useful on non-RPi systems)"
            echo "  --help        Show this help message"
            exit 0
            ;;
        --skip-usb)
            SKIP_USB=true
            shift
            ;;
    esac
done

# Function to display progress
show_progress() {
    echo -e "${BLUE}Step $CURRENT_STEP of $TOTAL_STEPS: $1${NC}"
}

# Logging function
log() {
    local level=$1
    shift
    local message="[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $*"
    echo -e "$message" >> "$LOG_FILE"
    if [ "$VERBOSE" = true ] || [ "$level" != "DEBUG" ]; then
        case $level in
            "ERROR") echo -e "${RED}$message${NC}" ;;
            "SUCCESS") echo -e "${GREEN}$message${NC}" ;;
            "WARNING") echo -e "${YELLOW}$message${NC}" ;;
            "INFO") echo -e "${BLUE}$message${NC}" ;;
            *) echo -e "$message" ;;
        esac
    fi
}

# Error handling function
handle_error() {
    local error_code=$?
    local error_message=$1
    local retry_count=${2:-0}
    log "ERROR" "An error occurred during: $error_message (Error code: $error_code)"
    log "ERROR" "Check the log file for details: $LOG_FILE"

    if [ "$retry_count" -ge 3 ]; then
        log "ERROR" "Too many retries. Exiting."
        clean_exit 1
    fi

    echo -e "\n${RED}Would you like to:"
    echo "1. Retry this step"
    echo "2. Skip this step (not recommended)"
    echo "3. Exit installation${NC}"
    read -r choice

    case $choice in
        1) return 1 ;; # Retry
        2) return 0 ;; # Skip
        3) clean_exit 1 ;; # Exit
        *) handle_error "$error_message" $((retry_count + 1)) ;; # Invalid choice
    esac
}

# Function to check command success
check_success() {
    if [ $? -eq 0 ]; then
        log "SUCCESS" "$1"
        return 0
    else
        handle_error "$1"
        return $?
    fi
}

# # Check system compatibility
# check_system_compatibility() {
#     log "INFO" "Checking system compatibility..."
    
#     # Check if running on Raspberry Pi
#     if ! grep -q "Raspberry Pi" /proc/cpuinfo; then
#         log "WARNING" "This system might not be a Raspberry Pi. Continue anyway? (y/n)"
#         read -r response
#         if [[ ! "$response" =~ ^[Yy]$ ]]; then
#             clean_exit 1
#         fi
#     fi
    
#     check_success "System compatibility check completed"
# }
# Check system compatibility
check_system_compatibility() {
    log "INFO" "Checking system compatibility..."
    local should_ask_confirmation=false
    
    # Check if running on Raspberry Pi
    if ! grep -q "Raspberry Pi" /proc/cpuinfo; then
        log "WARNING" "This system might not be a Raspberry Pi"
        should_ask_confirmation=true
    fi

    # Check RAM (Raspberry Pi Zero has 512MB RAM)
    total_ram=$(free -m | awk '/^Mem:/{print $2}')
    if [ "$total_ram" -lt 410 ]; then
        log "WARNING" "Low RAM detected. Required: 512MB (410 With OS Running), Found: ${total_ram}MB"
        echo -e "${YELLOW}Your system has less RAM than recommended. This might affect performance, but you can continue.${NC}"
        should_ask_confirmation=true
    else
        log "SUCCESS" "RAM check passed: ${total_ram}MB available"
    fi

    # Check available disk space
    available_space=$(df -m /home | awk 'NR==2 {print $4}')
    if [ "$available_space" -lt 2048 ]; then
        log "WARNING" "Low disk space. Recommended: 1GB, Found: ${available_space}MB"
        echo -e "${YELLOW}Your system has less free space than recommended. This might affect installation.${NC}"
        should_ask_confirmation=true
    else
        log "SUCCESS" "Disk space check passed: ${available_space}MB available"
    fi

    # Check OS version
    if [ -f "/etc/os-release" ]; then
        source /etc/os-release
        
        # Verify if it's Raspbian
        if [ "$NAME" != "Raspbian GNU/Linux" ]; then
            log "WARNING" "Different OS detected. Recommended: Raspbian GNU/Linux, Found: ${NAME}"
            echo -e "${YELLOW}Your system is not running Raspbian GNU/Linux.${NC}"
            should_ask_confirmation=true
        fi
        
        # Compare versions (supporting Bookworm = 12 and Trixie = 13)
        if [ "$VERSION_ID" != "12" ] && [ "$VERSION_ID" != "13" ]; then
            log "WARNING" "Different OS version detected"
            echo -e "${YELLOW}This script was tested with Raspbian GNU/Linux 12 (bookworm) and 13 (trixie)${NC}"
            echo -e "${YELLOW}Current system: ${PRETTY_NAME}${NC}"
            should_ask_confirmation=true
        else
            log "SUCCESS" "OS version check passed: ${PRETTY_NAME}"
        fi
    else
        log "WARNING" "Could not determine OS version (/etc/os-release not found)"
        should_ask_confirmation=true
    fi

    # Check if system is ARM (armhf for Pi Zero, arm64 for Pi Zero 2W)
    architecture=$(dpkg --print-architecture)
    if [ "$architecture" != "armhf" ] && [ "$architecture" != "arm64" ]; then
        log "WARNING" "Different architecture detected. Expected: armhf or arm64, Found: ${architecture}"
        echo -e "${YELLOW}This script was designed for Raspberry Pi Zero (armhf) and Zero 2W (arm64)${NC}"
        should_ask_confirmation=true
    else
        log "SUCCESS" "Architecture check passed: ${architecture}"
    fi
    
    # Additional Pi Zero specific checks if possible
    if ! (grep -q "Pi Zero" /proc/cpuinfo || grep -q "BCM2835" /proc/cpuinfo); then
        log "WARNING" "Could not confirm if this is a Raspberry Pi Zero"
        echo -e "${YELLOW}This script was designed for Raspberry Pi Zero${NC}"
        should_ask_confirmation=true
    else
        log "SUCCESS" "Raspberry Pi Zero detected"
    fi

    if [ "$should_ask_confirmation" = true ]; then
        echo -e "\n${YELLOW}Some system compatibility warnings were detected (see above).${NC}"
        echo -e "${YELLOW}The installation might not work as expected.${NC}"
        echo -e "${YELLOW}Do you want to continue anyway? (y/n)${NC}"
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            log "INFO" "Installation aborted by user after compatibility warnings"
            clean_exit 1
        fi
    else
        log "SUCCESS" "All compatibility checks passed"
    fi

    log "INFO" "System compatibility check completed"
    return 0
}



# Install system dependencies
install_dependencies() {
    log "INFO" "Installing system dependencies..."
    
    # Update package list
    DEBIAN_FRONTEND=noninteractive apt-get update
    
    # Detect architecture for conditional packages
    ARCH=$(dpkg --print-architecture 2>/dev/null || uname -m)

    # List of required packages
    packages=(
        "python3-pip"
        "wget"
        "lsof"
        "git"
        "nmap"
        "libopenblas-dev"
        "bluez-tools"
        "bluez"
        "dhcpcd5"
        "bridge-utils"
        "python3-dbus"
        "python3-dev"
        "libffi-dev"
        "libssl-dev"
        "build-essential"
        "aircrack-ng"
        "hostapd"
        "xvfb"
        "dnsmasq"
        "iw"
        "rfkill"
        "pixiewps"
        "net-tools"
        "iptables"
    )
    
    # Install common packages
    for package in "${packages[@]}"; do
        log "INFO" "Installing $package..."
        DEBIAN_FRONTEND=noninteractive apt-get install -y "$package"
        check_success "Installed $package"
    done
    
    # Install ARM-specific packages (only on Raspberry Pi / ARM systems)
    if [[ "$ARCH" == "armhf" || "$ARCH" == "arm64" || "$ARCH" == aarch* ]]; then
        log "INFO" "Installing ARM-specific packages..."
        arm_packages=("libatlas-base-dev")
        for pkg in "${arm_packages[@]}"; do
            DEBIAN_FRONTEND=noninteractive apt-get install -y "$pkg" 2>/dev/null || log "WARNING" "Could not install $pkg (non-critical)"
        done
    fi
    
    # Update nmap scripts
    nmap --script-updatedb
    check_success "Dependencies installation completed"
}

# Configure system limits
configure_system_limits() {
    log "INFO" "Configuring system limits..."

    # Configure /etc/security/limits.conf
    cat >> /etc/security/limits.conf << EOF
* soft nofile 65535
* hard nofile 65535
root soft nofile 65535
root hard nofile 65535
EOF

    # Configure systemd limits
    sed -i '/^#DefaultLimitNOFILE=/d' /etc/systemd/system.conf
    echo "DefaultLimitNOFILE=65535" >> /etc/systemd/system.conf
    sed -i '/^#DefaultLimitNOFILE=/d' /etc/systemd/user.conf
    echo "DefaultLimitNOFILE=65535" >> /etc/systemd/user.conf

    # Create /etc/security/limits.d/90-nofile.conf
    cat > /etc/security/limits.d/90-nofile.conf << EOF
root soft nofile 65535
root hard nofile 65535
EOF

    # Configure sysctl
    echo "fs.file-max = 2097152" >> /etc/sysctl.conf
    sysctl -p

    check_success "System limits configuration completed"
}

# Setup your-pax
setup_your_pax() {
    log "INFO" "Setting up your-pax..."
    
    # Create system user if it doesn't exist
    if ! id -u $YOUR_PAX_USER >/dev/null 2>&1; then
        adduser --disabled-password --gecos "" $YOUR_PAX_USER
        check_success "Created system user"
    fi

    # Check for existing your-pax directory
    cd "/home/$YOUR_PAX_USER"
    if [ -d "your-pax" ]; then
        log "INFO" "Using existing your-pax directory"
        echo -e "${GREEN}Using existing your-pax directory${NC}"
    else
        # No existing directory, proceed with clone
        log "INFO" "Cloning your-pax repository"
        git clone https://github.com/OP-AMINUL-FF/your-pax.git
        check_success "Cloned your-pax repository"
    fi

    cd your-pax

    # Install Python requirements
    log "INFO" "Installing Python requirements..."
    
    # Try multiple approaches for PEP 668 compatibility
    if pip3 install -r requirements.txt --break-system-packages; then
        log "SUCCESS" "Python requirements installed (system-wide)"
    elif pip3 install --user -r requirements.txt 2>/dev/null; then
        log "SUCCESS" "Python requirements installed (user site)"
    elif python3 -m venv $YOUR_PAX_PATH/venv && \
         source $YOUR_PAX_PATH/venv/bin/activate && \
         pip install -r $YOUR_PAX_PATH/requirements.txt; then
        log "INFO" "Remember to activate the venv: source $YOUR_PAX_PATH/venv/bin/activate"
    else
        handle_error "Installed Python requirements"
        return $?
    fi

    # Set correct permissions
    chown -R $YOUR_PAX_USER:$YOUR_PAX_USER $YOUR_PAX_PATH
    chmod -R 755 $YOUR_PAX_PATH

    # OneShot WPS attack tool setup (bundled in firmware)
    if [ -f "actions/oneshot.py" ]; then
        log "SUCCESS" "OneShot found in firmware bundle (actions/oneshot.py)"
    else
        log "WARNING" "OneShot not found in bundle. WPS attacks will be disabled."
    fi


}


# Configure services
setup_services() {
    log "INFO" "Setting up system services..."
    
    # Create kill_port_8000.sh script
    cat > $YOUR_PAX_PATH/kill_port_8000.sh << 'EOF'
#!/bin/bash
PORT=8000
PIDS=$(lsof -t -i:$PORT)
if [ -n "$PIDS" ]; then
    echo "Killing PIDs using port $PORT: $PIDS"
    kill -9 $PIDS
fi
EOF
    chmod +x $YOUR_PAX_PATH/kill_port_8000.sh

    # Create your-pax service
    cat > /etc/systemd/system/your-pax.service << EOF
[Unit]
Description=your-pax Service
DefaultDependencies=no
Before=basic.target
After=local-fs.target

[Service]
ExecStartPre=$YOUR_PAX_PATH/kill_port_8000.sh
ExecStart=/usr/bin/python3 $YOUR_PAX_PATH/your-pax.py
WorkingDirectory=$YOUR_PAX_PATH
StandardOutput=inherit
StandardError=inherit
Restart=always
User=root

# Check open files and restart if it reached the limit (ulimit -n buffer of 1000)
ExecStartPost=$YOUR_PAX_PATH/monitor_fd.sh

[Install]
WantedBy=multi-user.target
EOF

    # Configure PAM (check for duplicates first)
    if ! grep -q "^session required pam_limits.so" /etc/pam.d/common-session 2>/dev/null; then
        echo "session required pam_limits.so" >> /etc/pam.d/common-session
    fi
    if ! grep -q "^session required pam_limits.so" /etc/pam.d/common-session-noninteractive 2>/dev/null; then
        echo "session required pam_limits.so" >> /etc/pam.d/common-session-noninteractive
    fi

    # Configure BlueZ so the adapter stays pairable/discoverable indefinitely
    # and the NAP can auto-pair. Without this, DiscoverableTimeout/PairableTimeout
    # expire after a reboot and the phone can no longer find your-pax.
    log "INFO" "Configuring /etc/bluetooth/main.conf..."
    MAIN_CONF="/etc/bluetooth/main.conf"
    mkdir -p /etc/bluetooth
    if [ -f "$MAIN_CONF" ] && ! grep -q "your-pax managed" "$MAIN_CONF" 2>/dev/null; then
        cp -n "$MAIN_CONF" "${MAIN_CONF}.yourpax.bak" 2>/dev/null || true
    fi
    cat > "$MAIN_CONF" << EOF
# your-pax managed configuration
[General]
# Stay pairable + discoverable forever (no timeout) so the phone can connect
# at any time without re-enabling Bluetooth visibility on the Pi.
AlwaysPairable = true
PairableTimeout = 0
DiscoverableTimeout = 0

[Policy]
# Do not auto-suspend the adapter; the NAP must stay reachable.
AutoEnable=true
EOF
    # Apply the new config so bt-nap.service sees a ready adapter when it starts.
    systemctl restart bluetooth 2>/dev/null || log "WARNING" "Could not restart bluetooth service"
    sleep 2

    # Create Bluetooth NAP service
    cat > /etc/systemd/system/bt-nap.service << EOF
[Unit]
Description=Bluetooth NAP Service for your-pax
After=bluetooth.service
Requires=bluetooth.service

[Service]
ExecStart=/usr/bin/python3 $YOUR_PAX_PATH/actions/bluetooth_nap.py
Restart=always
RestartSec=3
User=root

[Install]
WantedBy=multi-user.target
EOF

    # Enable and start services
    systemctl daemon-reload
    systemctl enable bt-nap.service
    systemctl enable your-pax.service
    systemctl start bt-nap.service 2>/dev/null || log "WARNING" "Could not start bt-nap service"
    systemctl start your-pax.service 2>/dev/null || log "WARNING" "Could not start your-pax service"

    check_success "Services setup completed"
}

# Configure USB Gadget
configure_usb_gadget() {
    # USB gadget is Raspberry Pi-specific; skip on non-ARM systems
    ARCH=$(dpkg --print-architecture 2>/dev/null || uname -m)
    if [[ "$ARCH" != "armhf" && "$ARCH" != "arm64" && "$ARCH" != aarch* ]]; then
        log "INFO" "Skipping USB gadget configuration (not an ARM/Raspberry Pi system)"
        return 0
    fi

    log "INFO" "Configuring USB Gadget..."

    # Modify cmdline.txt
    if [ -f /boot/firmware/cmdline.txt ]; then
        sed -i 's/rootwait/rootwait modules-load=dwc2,g_ether/' /boot/firmware/cmdline.txt
    elif [ -f /boot/cmdline.txt ]; then
        sed -i 's/rootwait/rootwait modules-load=dwc2,g_ether/' /boot/cmdline.txt
    else
        log "WARNING" "Could not find /boot/firmware/cmdline.txt or /boot/cmdline.txt"
    fi

    # Modify config.txt
    if [ -f /boot/firmware/config.txt ]; then
        echo "dtoverlay=dwc2" >> /boot/firmware/config.txt
    elif [ -f /boot/config.txt ]; then
        echo "dtoverlay=dwc2" >> /boot/config.txt
    else
        log "WARNING" "Could not find /boot/firmware/config.txt or /boot/config.txt"
    fi

    # Create USB gadget script
    cat > /usr/local/bin/usb-gadget.sh << 'EOF'
#!/bin/bash
set -e

modprobe libcomposite
cd /sys/kernel/config/usb_gadget/
mkdir -p g1
cd g1

echo 0x1d6b > idVendor
echo 0x0104 > idProduct
echo 0x0100 > bcdDevice
echo 0x0200 > bcdUSB

mkdir -p strings/0x409
echo "fedcba9876543210" > strings/0x409/serialnumber
echo "Raspberry Pi" > strings/0x409/manufacturer
echo "Pi Zero USB" > strings/0x409/product

mkdir -p configs/c.1/strings/0x409
echo "Config 1: ECM network" > configs/c.1/strings/0x409/configuration
echo 250 > configs/c.1/MaxPower

mkdir -p functions/ecm.usb0

if [ -L configs/c.1/ecm.usb0 ]; then
    rm configs/c.1/ecm.usb0
fi
ln -s functions/ecm.usb0 configs/c.1/

max_retries=10
retry_count=0

while ! ls /sys/class/udc > UDC 2>/dev/null; do
    if [ $retry_count -ge $max_retries ]; then
        echo "Error: Device or resource busy after $max_retries attempts."
        exit 1
    fi
    retry_count=$((retry_count + 1))
    sleep 1
done

if ! ip addr show usb0 | grep -q "172.20.2.1"; then
    ifconfig usb0 172.20.2.1 netmask 255.255.255.0
else
    echo "Interface usb0 already configured."
fi
EOF

    chmod +x /usr/local/bin/usb-gadget.sh

    # Create USB gadget service
    cat > /etc/systemd/system/usb-gadget.service << EOF
[Unit]
Description=USB Gadget Service
After=network.target

[Service]
ExecStartPre=/sbin/modprobe libcomposite
ExecStart=/usr/local/bin/usb-gadget.sh
Type=simple
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

    # Configure network interface
    cat >> /etc/network/interfaces << EOF

allow-hotplug usb0
iface usb0 inet static
    address 172.20.2.1
    netmask 255.255.255.0
EOF

    # Enable and start services
    systemctl daemon-reload
    systemctl enable usb-gadget 2>/dev/null || log "WARNING" "Could not enable usb-gadget service"
    systemctl start usb-gadget 2>/dev/null || log "WARNING" "Could not start usb-gadget service"

    # systemd-networkd is optional; skip if masked/unavailable
    if systemctl is-enabled systemd-networkd &>/dev/null; then
        systemctl enable systemd-networkd 2>/dev/null
        systemctl start systemd-networkd 2>/dev/null || log "WARNING" "Could not start systemd-networkd"
    else
        log "INFO" "systemd-networkd not available, skipping (not critical for USB gadget)"
    fi
    systemctl start usb-gadget 2>/dev/null || log "WARNING" "Could not start usb-gadget (will start on boot)"

    check_success "USB Gadget configuration completed"
}

# Configure display mode
configure_display_mode() {
    log "INFO" "Configuring display mode..."
    echo ""
    echo -e "${BLUE}┌─────────────────────────────────────────────┐${NC}"
    echo -e "${BLUE}│         Display Configuration               │${NC}"
    echo -e "${BLUE}├─────────────────────────────────────────────┤${NC}"
    echo -e "${BLUE}│ Do you have an e-paper display connected?   │${NC}"
    echo -e "${BLUE}│                                             │${NC}"
    echo -e "${BLUE}│  1) Yes — with display (default)            │${NC}"
    echo -e "${BLUE}│     ~25MB RAM used for display drivers      │${NC}"
    echo -e "${BLUE}│                                             │${NC}"
    echo -e "${BLUE}│  2) No — headless mode (saves RAM)          │${NC}"
    echo -e "${BLUE}│     ~15-25MB RAM saved                      │${NC}"
    echo -e "${BLUE}│     Control via Web UI or SSH only          │${NC}"
    echo -e "${BLUE}└─────────────────────────────────────────────┘${NC}"
    echo ""
    read -p "Select display mode [1/2] (default: 1): " display_choice

    CONFIG_FILE="$YOUR_PAX_PATH/config/shared_config.json"

    if [ "$display_choice" = "2" ]; then
        log "INFO" "Non-display (headless) mode selected."
        if [ -f "$CONFIG_FILE" ]; then
            sed -i 's/"has_display": true/"has_display": false/' "$CONFIG_FILE"
            log "SUCCESS" "has_display set to false in config/shared_config.json"
        else
            log "WARNING" "Config file not found at $CONFIG_FILE"
            log "INFO" "Creating config with has_display=false..."
            mkdir -p "$YOUR_PAX_PATH/config"
            cat > "$CONFIG_FILE" << EOF
{
    "has_display": false
}
EOF
        fi
        echo -e "${GREEN}✓ Headless mode enabled (~15-25MB RAM saved)${NC}"
    else
        log "INFO" "Display mode selected (default)."
        if [ -f "$CONFIG_FILE" ]; then
            sed -i 's/"has_display": false/"has_display": true/' "$CONFIG_FILE"
            log "SUCCESS" "has_display set to true in config/shared_config.json"
        fi
        echo -e "${GREEN}✓ Display mode enabled${NC}"
    fi

    log "INFO" "Display mode configuration completed"
}

# Verify installation
verify_installation() {
    log "INFO" "Verifying installation..."
    
    # Check if services are running
    if ! systemctl is-active --quiet your-pax.service; then
        log "WARNING" "your-pax service is not running"
    else
        log "SUCCESS" "your-pax service is running"
    fi
    
    # Check web interface
    sleep 5
    if curl -s http://localhost:8000 > /dev/null; then
        log "SUCCESS" "Web interface is accessible"
    else
        log "WARNING" "Web interface is not responding"
    fi
}

# Clean exit function
clean_exit() {
    local exit_code=$1
    if [ $exit_code -eq 0 ]; then
        log "SUCCESS" "your-pax installation completed successfully!"
        log "INFO" "Log file available at: $LOG_FILE"
    else
        log "ERROR" "your-pax installation failed!"
        log "ERROR" "Check the log file for details: $LOG_FILE"
    fi
    exit $exit_code
}

# Main installation process
main() {
    log "INFO" "Starting your-pax installation..."

    # Check if script is run as root
    if [ "$(id -u)" -ne 0 ]; then
        echo "This script must be run as root. Please use 'sudo'."
        exit 1
    fi

    echo -e "${BLUE}your-pax Installation Options:${NC}"
    echo "1. Full installation (recommended)"
    echo "2. Custom installation"
    read -p "Choose an option (1/2): " install_option

    case $install_option in
        1)
            CURRENT_STEP=1; show_progress "Checking system compatibility"
            check_system_compatibility

            CURRENT_STEP=2; show_progress "Installing system dependencies"
            install_dependencies

            CURRENT_STEP=3; show_progress "Configuring system limits"
            configure_system_limits

            CURRENT_STEP=4; show_progress "Setting up your-pax"
            setup_your_pax

            configure_display_mode

            if [ "$SKIP_USB" = false ]; then
                CURRENT_STEP=5; show_progress "Configuring USB Gadget"
                configure_usb_gadget
            else
                log "INFO" "Skipping USB Gadget configuration (--skip-usb)"
            fi

            CURRENT_STEP=6; show_progress "Setting up services"
            setup_services

            CURRENT_STEP=7; show_progress "Verifying installation"
            verify_installation
            ;;
        2)
            echo "Custom installation - select components to install:"
            read -p "Install dependencies? (y/n): " deps
            read -p "Configure system limits? (y/n): " limits
            read -p "Setup your-pax? (y/n): " setup_yourpax
            read -p "Configure USB Gadget? (y/n): " usb_gadget_choice
            read -p "Setup services? (y/n): " services

            [ "$deps" = "y" ] && install_dependencies
            [ "$limits" = "y" ] && configure_system_limits
            [ "$setup_yourpax" = "y" ] && setup_your_pax
            [ "$setup_yourpax" = "y" ] && configure_display_mode
            [ "$usb_gadget_choice" = "y" ] && configure_usb_gadget
            [ "$services" = "y" ] && setup_services
            verify_installation
            ;;
        *)
            log "ERROR" "Invalid option selected"
            clean_exit 1
            ;;
    esac

    #remove git files (only if your-pax path is set and user agrees)
    if [ -n "$YOUR_PAX_PATH" ]; then
        echo -e "${YELLOW}Remove git files from $YOUR_PAX_PATH? (y/n)${NC}"
        echo -e "${YELLOW}This will delete .git directory and prevent future updates via git pull.${NC}"
        read -r remove_git
        if [ "$remove_git" = "y" ]; then
            find "$YOUR_PAX_PATH" -maxdepth 1 -name ".git*" -exec rm -rf {} +
            log "INFO" "Git files removed from $YOUR_PAX_PATH"
        else
            log "INFO" "Keeping git files in $YOUR_PAX_PATH"
        fi
    fi

    log "SUCCESS" "your-pax installation completed!"
    log "INFO" "Please reboot your system to apply all changes."
    echo -e "\n${GREEN}Installation completed successfully!${NC}"
    echo -e "${YELLOW}Important notes:${NC}"
    echo "1. If configuring Windows PC for USB gadget connection:"
    echo "   - Set static IP: 172.20.2.2"
    echo "   - Subnet Mask: 255.255.255.0"
    echo "   - Default Gateway: 172.20.2.1"
    echo "   - DNS Servers: 8.8.8.8, 8.8.4.4"
    echo "2. Web interface will be available at: http://[device-ip]:8000"

    read -p "Would you like to reboot now? (y/n): " reboot_now
    if [ "$reboot_now" = "y" ]; then
        if reboot; then
            log "INFO" "System reboot initiated."
        else
            log "ERROR" "Failed to initiate reboot."
            exit 1
        fi
    else
        echo -e "${YELLOW}Reboot your system to apply all changes & run your-pax service.${NC}"
    fi
}

main




