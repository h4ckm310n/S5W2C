package com.h4ckm310n.s5w2c

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ProxyService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val context = applicationContext
        val networkManager = NetworkManager(context)
        ProxyServer.networkManager = networkManager
        ProxyServer.startServer()
        Logger.log("Service start")
        return START_STICKY
    }

    override fun onDestroy() {
        Logger.log("Service stop")
        super.onDestroy()
        ProxyServer.stopServer()
    }

}