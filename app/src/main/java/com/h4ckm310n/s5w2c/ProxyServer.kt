package com.h4ckm310n.s5w2c

import android.annotation.SuppressLint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread

object ProxyServer {
    // Thread
    private var serverThread: Thread? = null
    private var stopListen = false

    fun startServer() {
        serverThread = thread { listen() }
    }

    fun stopServer() {
        stopListen = true
        if (!server!!.isClosed)
            server!!.close()
        if (!udpServer!!.isClosed)
            udpServer!!.close()
    }

    // Server
    private var PORT = (ConfigManager.getConfig("port") as String).toInt()

    private val CMD = mapOf(0x01 to "CONNECT", 0x02 to "BIND", 0x03 to "UDP")
    private val ATYP = mapOf(0x01 to "IPV4", 0x03 to "DOMAIN", 0x04 to "IPV6")

    private var FORWARD_BUFF_SIZE = (ConfigManager.getConfig("forward_buff_size") as String).toInt() * 1024
    private var FORWARD_TIMEOUT = (ConfigManager.getConfig("timeout") as String).toInt() * 1000
    private var server: ServerSocket? = null
    private var udpServer: DatagramSocket? = null

    @SuppressLint("StaticFieldLeak")
    var networkManager: NetworkManager? = null
    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        fun handleClient(client: Socket) = GlobalScope.launch(Dispatchers.IO) {
            Logger.log("Client: ${client.inetAddress.hostAddress!!}:${client.port}")
            try {
                if (handshake(client))
                    handleRequest(client)
                client.close()
                Logger.log("Client closed")
            } catch (e: Exception) {
                client.close()
                Logger.err("Client closed unexpectedly\n${e.stackTraceToString()}")
            }
        }

        stopListen = false
        Logger.items.clear()
        try {
            networkManager!!.initNetwork()

            server = ServerSocket(PORT)
            thread { listenUDP() }

            var client: Socket?
            while (!stopListen) {
                if (networkManager!!.wifiNetwork == null || networkManager!!.cellularNetwork == null)
                    continue
                client = server!!.accept()
                handleClient(client)
            }
            server!!.close()
            Logger.log("Server closed")
        } catch (e: SocketException) {
            if (stopListen)
                Logger.log("Server closed")
            else
                Logger.err("Server closed unexpectedly\n${e.stackTraceToString()}")
        } catch (e: Exception) {
            Logger.err("Server closed unexpectedly\n${e.stackTraceToString()}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun listenUDP() {
        fun handleUDPRequest(client: DatagramPacket) = GlobalScope.launch(Dispatchers.IO) {
            try {
                var addr = ""
                val buff = client.data
                if (buff[2] != 0x00.toByte()) {
                    Logger.err("UDP fragmentation not supported")
                    return@launch
                }

                var offset = 0
                when (ATYP[buff[3].toInt()]) {
                    "DOMAIN" -> {
                        val ndomain = buff[4].toInt()
                        addr = String(buff.copyOfRange(5, 5 + ndomain))
                        offset = 5 + ndomain
                    }

                    "IPV4" -> {
                        val ipIntArray = IntArray(4)
                        for (i in 4..7) {
                            ipIntArray[i - 4] = buff[i].toInt() and 0xff
                        }
                        addr = ipIntArray.joinToString(".")
                        offset = 8
                    }

                    "IPV6" -> {
                        Logger.err("IPV6 not supported yet")
                    }
                }

                if (addr == "" || offset == 0) {
                    return@launch
                }
                val port =
                    ((buff[offset].toInt() and 0xff) shl 8) or (buff[offset + 1].toInt() and 0xff)

                offset += 2
                val data = buff.copyOfRange(offset, client.length)

                val target = DatagramSocket()
                networkManager!!.cellularNetwork!!.bindSocket(target)
                target.connect(InetSocketAddress(addr, port))

                // Send to target
                var packet = DatagramPacket(data, data.size, InetSocketAddress(addr, port))
                target.send(packet)

                // Send to client
                val resp = buff.copyOfRange(0, offset)
                packet = DatagramPacket(buff, FORWARD_BUFF_SIZE)
                target.receive(packet)
                client.data = resp + buff.copyOfRange(0, packet.length)
                udpServer!!.send(client)
            } catch (e: Exception) {
                Logger.err("Failed to forward UDP packet\n${e.stackTraceToString()}")
            }
        }

        try {
            udpServer = DatagramSocket(PORT)
            var client: DatagramPacket?
            while (!stopListen) {
                if (networkManager!!.wifiNetwork == null || networkManager!!.cellularNetwork == null)
                    continue
                val buff = ByteArray(FORWARD_BUFF_SIZE)
                client = DatagramPacket(buff, FORWARD_BUFF_SIZE)
                udpServer!!.receive(client)
                handleUDPRequest(client)
            }
            udpServer!!.close()
        } catch (e: Exception) {
            if (stopListen)
                Logger.log("UDP server closed")
            else
                Logger.err("UDP server closed unexpectedly\n${e.stackTraceToString()}")
        }
    }

    private fun handshake(client: Socket): Boolean {
        val inputStream = client.getInputStream()
        val outputStream = client.getOutputStream()
        val buff = ByteArray(255)
        inputStream.read(buff, 0, 2)
        val version = buff[0]
        val nmethod = buff[1]
        if (version != 0x05.toByte() || nmethod < 1.toByte()) {
            Logger.err("Invalid handshake: ${version.toInt()} ${nmethod.toInt()}")
            outputStream.write(byteArrayOf(0x05.toByte(), 0xff.toByte()))
            outputStream.flush()
            return false
        }
        inputStream.read(buff, 0, nmethod.toInt())

        outputStream.write(byteArrayOf(0x05.toByte(), 0x00.toByte()))
        outputStream.flush()
        Logger.log("Handshake success")
        return true
    }

    private suspend fun handleRequest(client: Socket) = withContext(Dispatchers.IO) {
        val inputStream = client.getInputStream()

        val buff = ByteArray(255)
        inputStream.read(buff, 0, 4)
        val cmd = buff[1]
        val type = buff[3]
        var addr = ""
        when (ATYP[type.toInt()]) {
            "DOMAIN" -> {
                inputStream.read(buff, 0, 1)
                val ndomain = buff[0].toInt()
                inputStream.read(buff, 0, ndomain)
                addr = String(buff.copyOfRange(0, ndomain))
            }
            "IPV4" -> {
                inputStream.read(buff, 0, 4)
                val ipIntArray = IntArray(4)
                for (i in 0..3) {
                    ipIntArray[i] = buff[i].toInt() and 0xff
                }
                addr = ipIntArray.joinToString(".")
            }
            "IPV6" -> {
                inputStream.read(buff, 0, 16)
                Logger.err("IPV6 not supported yet")
            }
        }
        if (addr == "")
            return@withContext

        inputStream.read(buff, 0, 2)
        val port = ((buff[0].toInt() and 0xff) shl 8) or (buff[1].toInt() and 0xff)

        when (CMD[cmd.toInt()]) {
            "CONNECT" -> {
                handleConnect(client, addr, port)
            }
            "BIND" -> {
                Logger.err("BIND not supported yet")
            }
            "UDP" -> {
                handleUDPAssociate(client, addr, port)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun handleConnect(client: Socket, addr: String, port: Int) = withContext(Dispatchers.IO) {
        fun sendConnectResponse(rep: Byte) {
            val outputStream = client.getOutputStream()
            val addrBytes = client.localAddress.address
            val portBytes = byteArrayOf(((PORT and 0xff00) shr 8).toByte(), (PORT and 0xff).toByte())
            outputStream.write(byteArrayOf(0x05.toByte(), rep, 0x00.toByte(), 0x01.toByte()) + addrBytes + portBytes)
            outputStream.flush()
        }

        fun forward(inputStream: InputStream, outputStream: OutputStream) {
            var t = System.currentTimeMillis()
            var n: Int
            val buff = ByteArray(FORWARD_BUFF_SIZE)
            while (true) {
                try {
                    n = inputStream.read(buff)
                    if (n <= 0) {
                        // Timeout
                        if (System.currentTimeMillis() - t > FORWARD_TIMEOUT)
                            break
                        continue
                    }
                    outputStream.write(buff, 0, n)
                    outputStream.flush()
                    t = System.currentTimeMillis()
                } catch (e: Exception) {
                    break
                }
            }
        }

        Logger.log("Target: $addr:$port")
        // Connect to target
        val target: Socket?
        try {
            target = networkManager!!.cellularNetwork!!.socketFactory.createSocket(addr, port)
        } catch (e: Exception) {
            sendConnectResponse(0x01.toByte())
            Logger.err("Failed to connect target\n${e.stackTraceToString()}")
            return@withContext
        }
        sendConnectResponse(0x00.toByte())

        // Forward data
        try {
            val clientInputStream = client.getInputStream()
            val clientOutputStream = client.getOutputStream()
            val targetInputStream = target.getInputStream()
            val targetOutputStream = target.getOutputStream()

            val job = GlobalScope.launch(Dispatchers.IO) { forward(clientInputStream, targetOutputStream) }
            forward(targetInputStream, clientOutputStream)
            job.join()

        } catch (e: Exception) {
            Logger.err("Failed to forward\n${e.stackTraceToString()}")
        } finally {
            target.close()
            Logger.log("$addr Target closed")
        }
    }

    private suspend fun handleUDPAssociate(client: Socket, addr: String, port: Int) = withContext(Dispatchers.IO) {
        fun sendUDPResponse() {
            val outputStream = client.getOutputStream()
            val addrBytes = client.localAddress.address
            val portBytes = byteArrayOf(((PORT and 0xff00) shr 8).toByte(), (PORT and 0xff).toByte())
            outputStream.write(byteArrayOf(0x05.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte()) + addrBytes + portBytes)
            outputStream.flush()
        }

        if (addr != "0.0.0.0")
            Logger.log("UDP Client: $addr:$port")
        sendUDPResponse()

        // Wait for UDP
        delay(10000)
    }
}