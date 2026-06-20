# 🐛 Known Issues and Troubleshooting

<p align="center">
  <img src="https://github.com/user-attachments/assets/c5eb4cc1-0c3d-497d-9422-1614651a84ab" alt="thumbnail_IMG_0546" width="98">
</p>

## 📚 Table of Contents

- [Current Development Issues](#-current-development-issues)
- [Troubleshooting Steps](#-troubleshooting-steps)
- [License](#-license)

## 🪲 Current Development Issues

### Too Many Open Files

- **Problem**: `OSError: [Errno 24] Too many open files`
- **Workaround**: Increase system file descriptor limits:
  ```bash
  ulimit -n 4096
  ```
  Or edit `/etc/security/limits.conf` to make it permanent.
- **Monitoring**: Check open files with:
  ```bash
  lsof -p $(pgrep -f your-pax.py) | wc -l
  ```

## 🛠️ Troubleshooting Steps

### Service Issues

```bash
#See your-pax journalctl service
journalctl -fu your-pax.service

# Check service status
sudo systemctl status your-pax.service

# View detailed logs
sudo journalctl -u your-pax.service -f

or

sudo tail -f ~/your-pax/data/logs/*


# Check port usage (server tries 8000, 8001, 8002...)
sudo lsof -i :8000
sudo lsof -i :8001
```

### Display Issues

```bash
# Verify SPI devices
ls /dev/spi*

# Check user permissions
sudo usermod -a -G spi,gpio your-pax
```

### Network Issues

```bash
# Check network interfaces
ip addr show

# Test USB gadget interface
ip link show usb0
```

### Permission Issues

```bash
# Fix ownership (replace 'pi' with your username)
sudo chown -R pi:pi ~/your-pax

# Fix permissions
sudo chmod -R 755 ~/your-pax

# Add user to required groups
sudo usermod -a -G spi,gpio pi
```

### Bluetooth NAP Issues

```bash
# Check bluetooth service
sudo systemctl status bluetooth

# Check bt-nap service
sudo systemctl status bt-nap

# Check bridge interface
ip addr show pan0

# List connected Bluetooth NAP clients
ip neigh show dev pan0

# Restart Bluetooth NAP
sudo systemctl restart bt-nap

# View Bluetooth NAP logs
sudo journalctl -u bt-nap -f
```

### Monitor Mode Issues

```bash
# Check current wireless mode
iw dev wlan0 info

# Check if monitor interface exists
iw dev wlan0mon info

# Kill interfering processes
sudo airmon-ng check kill
```

---

## 📜 License

2024 - your-pax is distributed under the MIT License. For more details, please refer to the [LICENSE](LICENSE) file included in this repository.
