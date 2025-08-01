package com.example.environment_sensing

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LogService: Service() {
    companion object {
        const val CHANNEL_ID = "12345"
    }
    private lateinit var notificationManager: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // バックグラウンドでログを出力し続ける処理（1秒ごとにログ）
        Log.d("Service", "サービスが開始")
        // 通知マネージャーを取得（通知チャンネル作成などに使用）
        notificationManager = NotificationManagerCompat.from(this)

        // 通知チャンネルの作成
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("ログ出力")
            .build()
        // チャンネルを通知マネージャーに登録
        notificationManager.createNotificationChannel(channel)

        // 通知をタップしたときにMainActivityを開くIntentを作成
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        // 通知を作成
        val notification = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ログ出力中")
            .setContentText("フォアグラウンドサービスでログを出力し続けています")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
        // 通知の表示
        startForeground(1212, notification)
        var i = 0
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                Log.d("Service", i.toString())
                i++
                delay(1000)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // サービス終了時の処理
        Log.d("Service", "サービスが終了")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}