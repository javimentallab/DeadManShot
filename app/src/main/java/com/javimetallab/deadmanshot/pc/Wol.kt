package com.javimetallab.deadmanshot.pc

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Sends a WoL magic packet (6 × 0xFF + MAC × 16) to UDP port 9. */
object Wol {
    private val MAC_RE = Regex("^([0-9a-fA-F]{2}[-:]?){5}[0-9a-fA-F]{2}$")

    fun isMacValid(mac: String): Boolean = MAC_RE.matches(mac.trim())

    private fun parseMac(mac: String): ByteArray {
        val hex = mac.replace(Regex("[-:]"), "")
        require(hex.length == 12) { "MAC must be 6 bytes" }
        return ByteArray(6) { i ->
            Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16).toByte()
        }
    }

    fun send(mac: String, broadcast: String = "255.255.255.255", port: Int = 9) {
        val macBytes = parseMac(mac)
        val packet = ByteArray(6 + 16 * 6)
        for (i in 0 until 6) packet[i] = 0xFF.toByte()
        for (i in 0 until 16) System.arraycopy(macBytes, 0, packet, 6 + i * 6, 6)
        val address = InetAddress.getByName(broadcast)
        DatagramSocket().use { socket ->
            socket.broadcast = true
            socket.send(DatagramPacket(packet, packet.size, address, port))
            // Second packet improves reliability: cheap insurance.
            socket.send(DatagramPacket(packet, packet.size, address, port))
        }
    }
}
