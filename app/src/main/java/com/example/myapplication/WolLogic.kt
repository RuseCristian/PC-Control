package com.example.myapplication

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun sendWakeOnLan(macAddress: String, ipAddress: String, port: Int): String {
    return try {
        val cleanMac = macAddress.replace(":", "").replace("-", "")
        val macBytes = ByteArray(6)
        for (i in 0..5) {
            macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }

        val bytes = ByteArray(6 + 16 * macBytes.size)
        for (i in 0..5) bytes[i] = 0xff.toByte()
        for (i in 6 until bytes.size step macBytes.size) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
        }

        val address = InetAddress.getByName(ipAddress)
        val packet = DatagramPacket(bytes, bytes.size, address, port)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(packet)
        }
        "Wake-on-LAN packet sent (Port $port)"
    } catch (e: Exception) {
        "WOL Error: ${e.message}"
    }
}
