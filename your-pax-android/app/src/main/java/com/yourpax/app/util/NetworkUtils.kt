package com.yourpax.app.util

import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {
    fun getBluetoothPanIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.startsWith("bt") ||
                    networkInterface.name.startsWith("bnep") ||
                    networkInterface.name.startsWith("pan")
                ) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress) {
                            val ip = address.hostAddress ?: continue
                            if (ip.startsWith("192.168.")) return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
