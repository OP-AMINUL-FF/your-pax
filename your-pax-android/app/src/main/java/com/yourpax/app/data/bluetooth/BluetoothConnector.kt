package com.yourpax.app.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.yourpax.app.util.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles the phone-side half of the your-pax Bluetooth NAP connection.
 *
 * Connection model (user-driven PAN):
 *  1. [pairDevice] programmatically bonds the phone to the your-pax device via
 *     [BluetoothDevice.createBond], waiting for ACTION_BOND_STATE_CHANGED.
 *  2. The actual PAN (Internet Access) connection is enabled by the user in
 *     Android Settings -> Bluetooth -> your-pax -> "Internet access" ON.
 *     [awaitPanIp] polls until the bnep/pan interface is assigned an IP.
 *
 * No hidden BluetoothPan API is used, which keeps this portable across
 * Android versions including 13/14 where hidden-API access is restricted.
 */
class BluetoothConnector(private val context: Context) {

    private var isConnected = false

    private fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Pairs (bonds) the phone with [device]. Returns true once BOND_BONDED
     * is reached, or false on failure/timeout.
     */
    suspend fun pairDevice(device: BluetoothDevice, timeoutMs: Long = 30_000L): Boolean {
        if (!hasConnectPermission()) return false
        try {
            if (device.bondState == BluetoothDevice.BOND_BONDED) return true
        } catch (_: SecurityException) {
            return false
        }

        val settled = AtomicBoolean(false)
        var result = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
                val bondedDevice =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        ?: return
                if (bondedDevice.address != device.address) return
                val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val prev = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE)
                if (state == BluetoothDevice.BOND_BONDED && settled.compareAndSet(false, true)) {
                    result = true
                    unregisterQuietly(this)
                } else if (state == BluetoothDevice.BOND_NONE && prev != BluetoothDevice.BOND_NONE &&
                    settled.compareAndSet(false, true)
                ) {
                    result = false
                    unregisterQuietly(this)
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {
            return false
        }

        val bondStarted = try {
            device.createBond()
        } catch (_: SecurityException) {
            unregisterQuietly(receiver)
            return false
        }
        if (!bondStarted) {
            unregisterQuietly(receiver)
            return false
        }

        // Wait for bond-state change with timeout.
        withTimeoutOrNull(timeoutMs) {
            while (!settled.get()) delay(200)
        }
        if (settled.compareAndSet(false, true)) {
            unregisterQuietly(receiver)
        }
        return result
    }

    private fun unregisterQuietly(receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        } catch (_: SecurityException) {
        }
    }

    /**
     * Waits for the PAN (Internet Access) connection to be established by the
     * user and an IP assigned to the bt/bnep/pan interface. Returns the IP, or
     * null on timeout.
     */
    suspend fun awaitPanIp(timeoutMs: Long = 30_000L): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val ip = NetworkUtils.getBluetoothPanIp()
            if (ip != null) return ip
            delay(500)
        }
        return null
    }

    fun disconnect() {
        isConnected = false
    }

    fun isConnectedToPan(): Boolean = isConnected
}
