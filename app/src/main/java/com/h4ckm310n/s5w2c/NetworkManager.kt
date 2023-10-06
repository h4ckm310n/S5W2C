package com.h4ckm310n.s5w2c

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import java.net.Inet4Address

class NetworkManager constructor(private val context: Context) {
    var wifiNetwork: Network? = null
    var cellularNetwork: Network? = null

    fun initNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val wifiRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val cellularRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.requestNetwork(cellularRequest, object: ConnectivityManager.NetworkCallback() {})

        connectivityManager.registerNetworkCallback(wifiRequest, object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.getLinkProperties(network)!!.linkAddresses.map {
                    Logger.log("WiFi address: ${it.address.hostAddress!!}")
                }
                wifiNetwork = network
                Logger.log("WiFi available")
            }

            override fun onLost(network: Network) {
                wifiNetwork = null
                Logger.err("WiFi lost")
            }
        })

        connectivityManager.registerNetworkCallback(cellularRequest, object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cellularNetwork = network
                Logger.log("Cellular available")
            }

            override fun onLost(network: Network) {
                cellularNetwork = null
                Logger.err("Cellular lost")
            }
        })
    }
}