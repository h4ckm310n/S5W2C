package com.h4ckm310n.s5w2c

import android.annotation.SuppressLint
import android.net.Network
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
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
    }

    // Server
    private const val PORT = 3111

    private val CMD = mapOf(0x01 to "CONNECT", 0x02 to "BIND", 0x03 to "UDP")
    private val ATYP = mapOf(0x01 to "IPV4", 0x03 to "DOMAIN", 0x04 to "IPV6")

    private const val FORWARD_BUFF_SIZE = 5 * 1024 * 1024

    @SuppressLint("StaticFieldLeak")
    var networkManager: NetworkManager? = null
    @OptIn(DelicateCoroutinesApi::class)
    fun listen() {
        stopListen = false
        val server: ServerSocket?
        Logger.items.clear()
        try {
            networkManager!!.initNetwork()

            server = ServerSocket(PORT)
            Logger.log(server.toString())

            var client: Socket?
            while (!stopListen) {
                if (networkManager!!.wifiNetwork == null || networkManager!!.cellularNetwork == null)
                    continue
                client = server.accept()
                Logger.log(client.toString())
                GlobalScope.launch(Dispatchers.IO) {
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
            }
            server.close()
            Logger.log("Server closed")
        } catch (e: Exception) {
            Logger.err("Server closed unexpectedly\n${e.stackTraceToString()}")
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
            outputStream.write(byteArrayOf(0x05.toByte(), 0xff.toByte()))
            return false
        }
        inputStream.read(buff, 0, nmethod.toInt())

        outputStream.write(byteArrayOf(0x05.toByte(), 0x00.toByte()))
        outputStream.flush()
        Logger.log("Handshake success")
        return true
    }

    private suspend fun handleRequest(client: Socket) {
        val inputStream = client.getInputStream()

        val buff = ByteArray(255)
        inputStream.read(buff, 0, 4)
        val cmd = buff[1]
        val type = buff[3]
        var domain = ""
        when (ATYP[type.toInt()]) {
            "DOMAIN" -> {
                inputStream.read(buff, 0, 1)
                val ndomain = buff[0].toInt()
                inputStream.read(buff, 0, ndomain)
                domain = String(buff.copyOfRange(0, ndomain))
            }
            "IPV4" -> {
                inputStream.read(buff, 0, 4)
                domain = buff.copyOfRange(0, 4).joinToString(".")
            }
            "IPV6" -> {
                inputStream.read(buff, 0, 16)
                Logger.err("IPV6 not supported yet")
            }
        }
        if (domain == "")
            return

        inputStream.read(buff, 0, 2)
        val port = ((buff[0].toInt() and 0xff) shl 8) or (buff[1].toInt() and 0xff)

        when (CMD[cmd.toInt()]) {
            "CONNECT" -> {
                handleConnect(client, domain, port)
            }
            "BIND" -> {
                Logger.err("BIND not supported yet")
            }
            "UDP" -> {
                Logger.err("UDP not supported yet")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun handleConnect(client: Socket, domain: String, port: Int) {
        fun sendConnectResponse(rep: Byte) {
            val outputStream = client.getOutputStream()
            val addrBytes = client.localAddress.address
            val portBytes = byteArrayOf(((PORT and 0xff00) shr 8).toByte(), (PORT and 0xff).toByte())
            outputStream.write(byteArrayOf(0x05.toByte(), rep, 0x00.toByte(), 0x01.toByte()) + addrBytes + portBytes)
            outputStream.flush()
        }

        Logger.log("Target: $domain:$port")
        // Connect to target
        val target: Socket?
        try {
            target = networkManager!!.cellularNetwork!!.socketFactory.createSocket(domain, port)
        } catch (e: Exception) {
            sendConnectResponse(0x01.toByte())
            Logger.err("Failed to connect target\n${e.stackTraceToString()}")
            return
        }
        sendConnectResponse(0x00.toByte())

        // Forward data
        try {
            val buff = ByteArray(FORWARD_BUFF_SIZE)
            val clientInputStream = client.getInputStream()
            val clientOutputStream = client.getOutputStream()
            val targetInputStream = target.getInputStream()
            val targetOutputStream = target.getOutputStream()

            var n: Int
            var t1: Long
            // Send response
            val job = GlobalScope.launch(Dispatchers.IO) {
                t1 = System.currentTimeMillis()
                while (true) {
                    try {
                        n = targetInputStream.read(buff)
                        if (n <= 0) {
                            // Timeout
                            if (System.currentTimeMillis() - t1 > 30000)
                                break
                            continue
                        }
                        clientOutputStream.write(buff, 0, n)
                        clientOutputStream.flush()
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            // Send request
            t1 = System.currentTimeMillis()
            while (true) {
                try {
                    n = clientInputStream.read(buff)
                    if (n <= 0) {
                        // Timeout
                        if (System.currentTimeMillis() - t1 > 30000)
                            break
                        continue
                    }
                    targetOutputStream.write(buff, 0, n)
                    targetOutputStream.flush()
                } catch (e: Exception) {
                    break
                }
            }
            job.join()

        } catch (e: Exception) {
            Logger.err("Failed to forward\n${e.stackTraceToString()}")
        } finally {
            target.close()
            Logger.log("$domain Target closed")
        }
    }

}