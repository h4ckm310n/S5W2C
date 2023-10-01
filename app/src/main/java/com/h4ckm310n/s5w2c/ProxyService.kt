package com.h4ckm310n.s5w2c

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder

class ProxyService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val context = applicationContext
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notificationChannel = NotificationChannel("S5W2C.Notification", "S5W2C.Notification", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        val notification = Notification.Builder(this, "S5W2C.Notification")
            .setContentTitle("S5W2C")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

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
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

}