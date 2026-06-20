package com.yourpax.app.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.yourpax.app.util.Constants

/**
 * Performs real Bluetooth device discovery (not just a bonded-set lookup).
 *
 * Wraps [BluetoothAdapter.startDiscovery] and surfaces each discovered device
 * via the `onFound` callback by registering a [BroadcastReceiver] for
 * [BluetoothDevice.ACTION_FOUND]. Discovered devices are also cached so that
 * [findYourPaxDevice] can match by name across both bonded and freshly
 * discovered devices.
 */
class BluetoothScanner(
    private val bluetoothAdapter: BluetoothAdapter?,
    private val context: Context
) {

    private var isScanning = false
    private var onDeviceFound: ((BluetoothDevice) -> Unit)? = null
    private var onScanFinished: (() -> Unit)? = null

    private val discoveredDevices = LinkedHashMap<String, BluetoothDevice>()
    private var discoveryReceiver: BroadcastReceiver? = null
    private var timeoutThread: Thread? = null

    fun startScan(
        onFound: (BluetoothDevice) -> Unit,
        onFinished: () -> Unit
    ) {
        if (bluetoothAdapter == null || isScanning) return
        onDeviceFound = onFound
        onScanFinished = onFinished
        isScanning = true
        discoveredDevices.clear()

        // Register a receiver that collects each found device and fires onFound.
        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                ?: return
                        if (device.address != null) {
                            discoveredDevices[device.address] = device
                        }
                        onDeviceFound?.invoke(device)
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        finishScan()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)

        // startDiscovery is unreliable while an existing discovery is running.
        bluetoothAdapter.cancelDiscovery()

        // Safety timeout: discovery can hang on some stacks; force-finish.
        timeoutThread = Thread {
            try {
                Thread.sleep(Constants.BT_SCAN_TIMEOUT)
            } catch (_: InterruptedException) {
                return@Thread
            }
            finishScan()
        }.also { it.isDaemon = true; it.start() }

        bluetoothAdapter.startDiscovery()
    }

    @Synchronized
    private fun finishScan() {
        if (!isScanning) return
        isScanning = false
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (_: SecurityException) {
        }
        try {
            discoveryReceiver?.let { context.unregisterReceiver(it) }
        } catch (_: IllegalArgumentException) {
            // Already unregistered.
        }
        discoveryReceiver = null
        timeoutThread?.interrupt()
        timeoutThread = null
        onScanFinished?.invoke()
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Searches bonded AND discovered devices for one whose name matches your-pax.
     */
    fun findYourPaxDevice(): BluetoothDevice? {
        val candidates = getBondedDevices() + discoveredDevices.values
        return candidates.distinctBy { it.address }.find {
            val name = try { it.name } catch (_: SecurityException) { null }
            name?.contains(Constants.YOUR_PAX_BT_NAME, ignoreCase = true) == true
        }
    }

    fun stopScan() {
        finishScan()
    }
}
