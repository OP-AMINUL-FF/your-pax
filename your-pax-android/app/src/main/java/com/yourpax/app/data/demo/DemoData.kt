package com.yourpax.app.data.demo

import com.yourpax.app.data.api.models.*

object DemoData {

    val demoCredentials = listOf(
        CredentialFile(name = "ftp_creds.txt", headers = listOf("service", "username", "password"), rows = listOf(listOf("FTP", "admin", "admin123"), listOf("SSH", "root", "toor"))),
        CredentialFile(name = "http_creds.txt", headers = listOf("url", "username", "password"), rows = listOf(listOf("192.168.1.1/login", "admin", "password"), listOf("example.com", "user", "pass123"))),
        CredentialFile(name = "wpa_handshakes.txt", headers = listOf("ssid", "bssid", "handshake"), rows = listOf(listOf("HomeWiFi", "AA:BB:CC:DD:EE:FF", "captured"), listOf("NeighborNet", "11:22:33:44:55:66", "captured")))
    )

    val demoStoreDataFull = StoreDataFull(
        handshakes = listOf(
            StoreFileItem(name = "homewifi_handshake.cap", path = "/data/output/handshakes/homewifi.cap", size = 12288, modified = "2024-01-15 14:30"),
            StoreFileItem(name = "neighbor_handshake.cap", path = "/data/output/handshakes/neighbor.cap", size = 8192, modified = "2024-01-14 10:00")
        ),
        pmkid = listOf(
            StoreFileItem(name = "office_pmkid.pmk", path = "/data/output/pmkid/office.pmk", size = 2048, modified = "2024-01-13 09:15")
        ),
        credsEvil = listOf(
            EvilCredEntry(time = "2024-01-15 14:35", password = "password123"),
            EvilCredEntry(time = "2024-01-15 14:36", password = "admin1234"),
            EvilCredEntry(time = "2024-01-15 14:40", password = "letmein")
        ),
        wpsReports = listOf(
            WpsReport(file = "wps_report.csv", content = "BSSID,SSID,PIN,PSK\nAA:BB:CC:DD:EE:01,HomeWiFi,12345678,password123\n"),
            WpsReport(file = "wps_result.txt", content = "Target: OfficeNet\nPIN: 98765432\nPSK: securepass")
        ),
        credsCracked = CredsCrackedData(
            count = 8,
            services = listOf(
                CrackedService(name = "FTP", count = 3),
                CrackedService(name = "SSH", count = 2),
                CrackedService(name = "HTTP", count = 2),
                CrackedService(name = "MySQL", count = 1)
            )
        ),
        stolenFiles = (1..20).map { i ->
            StoreFileItem(name = "file_$i.txt", path = "/data/output/data_stolen/host_192.168.1.1/file_$i.txt", size = (i * 1024).toLong(), modified = "2024-01-15", rel = "host_192.168.1.1/file_$i.txt")
        },
        scanResults = listOf(
            StoreFileItem(name = "scan_192.168.1.0-24.xml", path = "/data/output/scan_results/scan1.xml", size = 46080, modified = "2024-01-15"),
            StoreFileItem(name = "scan_10.0.0.0-24.xml", path = "/data/output/scan_results/scan2.xml", size = 32768, modified = "2024-01-14")
        ),
        vulnScans = listOf(
            StoreFileItem(name = "vuln_report_192.168.1.0-24.csv", path = "/data/output/vulnerabilities/vuln1.csv", size = 8192, modified = "2024-01-15"),
            StoreFileItem(name = "vuln_summary.txt", path = "/data/output/vulnerabilities/summary.txt", size = 2048, modified = "2024-01-15")
        ),
        zombies = listOf(
            StoreFileItem(name = "zombie_list.csv", path = "/data/output/zombies/list.csv", size = 1024, modified = "2024-01-15")
        ),
        netkbCount = 23
    )

    val demoStoreData = StoreData(
        handshakes = 3, pmkid = 1, credsEvil = 3, credsCracked = 8,
        stolenFiles = 20, scanResults = 156, vulnScans = 7, netkbCount = 23
    )

    val demoNetworks = listOf(
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:01", ssid = "HomeWiFi", channel = "6", signal = "-45", wpa = "WPA2", wps = "No"),
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:02", ssid = "NeighborNet", channel = "1", signal = "-67", wpa = "WPA2", wps = "Yes"),
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:03", ssid = "CoffeeShop", channel = "11", signal = "-72", wpa = "WPA3", wps = "No"),
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:04", ssid = "", channel = "3", signal = "-80", wpa = "WPA2", wps = "No"),
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:05", ssid = "Office-Guest", channel = "6", signal = "-55", wpa = "WPA2", wps = "No"),
        WiFiNetwork(bssid = "AA:BB:CC:DD:EE:06", ssid = "IoT_Network", channel = "1", signal = "-88", wpa = "WPA", wps = "Yes")
    )

    val demoScanResponse = NetworkScanResponse(
        headers = listOf("IP", "MAC", "Hostname", "Open Ports"),
        rows = listOf(
            listOf("192.168.1.1", "AA:BB:CC:DD:EE:11", "router", "80,443,22"),
            listOf("192.168.1.2", "AA:BB:CC:DD:EE:12", "laptop", "22,8080"),
            listOf("192.168.1.3", "AA:BB:CC:DD:EE:13", "phone", ""),
            listOf("192.168.1.100", "AA:BB:CC:DD:EE:14", "server", "443,3306,6379"),
            listOf("192.168.1.200", "AA:BB:CC:DD:EE:15", "printer", "80,9100")
        )
    )

    val demoNetKBResponse = NetKBResponse(
        headers = listOf("IP", "Ports", "Status"),
        rows = listOf(
            listOf("192.168.1.1", "80,443,22", "success"),
            listOf("192.168.1.100", "80,443,22,3306,6379", "success"),
            listOf("192.168.1.200", "80,9100", "success"),
            listOf("192.168.1.250", "22", "failed"),
            listOf("10.0.0.1", "443", "pending")
        )
    )

    val demoLootFiles = listOf(
        LootFile(name = "documents", isDirectory = true, children = listOf(
            LootFile(name = "passwords.txt", path = "/loot/documents/passwords.txt"),
            LootFile(name = "notes.md", path = "/loot/documents/notes.md")
        ), path = "/loot/documents"),
        LootFile(name = "screenshots", isDirectory = true, children = listOf(
            LootFile(name = "screen1.png", path = "/loot/screenshots/screen1.png"),
            LootFile(name = "screen2.png", path = "/loot/screenshots/screen2.png")
        ), path = "/loot/screenshots"),
        LootFile(name = "network_dump.pcap", path = "/loot/network_dump.pcap"),
        LootFile(name = "wifi_handshakes.zip", path = "/loot/wifi_handshakes.zip"),
        LootFile(name = "config_backup.json", path = "/loot/config_backup.json")
    )

    val demoConfig = ConfigData(
        manualMode = false, webServer = true, debugMode = true,
        scanInterval = 180, scanVulnInterval = 900,
        wifiInterface = "wlan0", monitorInterface = "wlan0mon",
        evilApSsid = "FreePublicWiFi", evilApChannel = 6,
        portList = listOf(22, 80, 443, 8080, 3306),
        ipBlacklist = emptyList(), macBlacklist = emptyList(),
        startupDelay = 10, nmapAggressivity = "-T4",
        enableMonitorMode = true, evilApRunning = false
    )

    val demoWifiStatus = WiFiStatusResponse(connected = true, ssid = "HomeWiFi", signal = "-45")

    val demoAttackStatus = AttackStatusResponse(running = false, output = listOf("Idle"))

    val demoActionResponse = ActionResponse(status = "success", message = "Demo mode: action simulated")

    val demoBluetoothStatus = BluetoothStatus(
        active = true, bridgeIp = "192.168.4.1", port = 8000,
        ssid = "your-pax", connectedClients = 3,
        clients = listOf("Android Phone", "Laptop", "Tablet")
    )

    val demoBluetoothDevices = listOf(
        BluetoothDeviceInfo(name = "your-pax", ip = "192.168.4.1", mac = "00:11:22:33:44:55"),
        BluetoothDeviceInfo(name = "Test Device", ip = "192.168.4.2", mac = "AA:BB:CC:DD:EE:FF")
    )

    val demoLogResponse = LogResponse(
        logs = "[12:00:01] your-pax started\n[12:00:05] WiFi interface wlan0 initialized\n[12:00:10] Bluetooth NAP started on 192.168.4.1\n[12:01:00] Scan: found 5 hosts\n[12:02:30] Handshake captured from HomeWiFi\n[12:05:00] Evil AP 'FreePublicWiFi' started\n[12:10:00] 2 new credentials harvested\n[12:15:00] Vulnerability scan complete\n[12:20:00] Steal: 12 files extracted"
    )

    val demoEvilApStatus = EvilApStatusResponse(running = false, mode = "basic")

    val demoEvilClients = EvilClientsResponse(
        clients = listOf(
            EvilClientInfo(ip = "192.168.4.10", mac = "AA:BB:CC:DD:EE:10", hostname = "victim-phone"),
            EvilClientInfo(ip = "192.168.4.11", mac = "AA:BB:CC:DD:EE:11", hostname = "victim-laptop")
        )
    )

    val demoLootMonitor = LootMonitorData(
        dns = listOf(MonitorDnsEntry(domain = "example.com", client = "192.168.4.10")),
        http = listOf(MonitorHttpEntry(url = "http://example.com/login", os = "Android")),
        devices = listOf(MonitorDeviceEntry(hostname = "victim-phone", mac = "AA:BB:CC:DD:EE:10", ip = "192.168.4.10"))
    )

    val demoPortalList = PortalListResponse(
        portals = listOf(
            PortalInfo(name = "generic.html", source = "builtin"),
            PortalInfo(name = "google_login.html", source = "builtin"),
            PortalInfo(name = "facebook_login.html", source = "builtin"),
            PortalInfo(name = "isp_login.html", source = "builtin"),
            PortalInfo(name = "router_update.html", source = "builtin")
        )
    )

    val demoScanTargets = listOf(
        ScanTargetResult(bssid = "BB:CC:DD:EE:FF:01", ssid = "OfficeWiFi", channel = "6"),
        ScanTargetResult(bssid = "BB:CC:DD:EE:FF:02", ssid = "GuestNet", channel = "11")
    )
}
