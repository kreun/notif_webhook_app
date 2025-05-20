package com.example.notif_webhook_app

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.flutter.plugin.common.MethodChannel

class MyNotificationListenerService : NotificationListenerService() {
    companion object {
        private const val TAG = "NotifService"
        private const val CHANNEL = "notif_channel"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification

        // ① 그룹 요약(summary) 알림이면 무시
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        ) {
            Log.d(TAG, "요약 알림 무시: pkg=${sbn.packageName}, id=${sbn.id}")
            return
        }

        // ② 제목·본문이 모두 비어 있으면 무시
        val extras = notification.extras
        val title = extras.getString("android.title") ?: ""
        val text  = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) {
            Log.d(TAG, "빈 알림 무시: pkg=${sbn.packageName}")
            return
        }

        // ③ 실제 메시지 알림 처리
        val pkg       = sbn.packageName
        val timestamp = sbn.postTime

        Log.d(TAG, "📨 실제 알림 → pkg:$pkg, title:$title, text:$text, time:$timestamp")

        // Flutter 측으로 전달
        val extrasMap = mutableMapOf<String, String>()
    for (key in extras.keySet()) {
        extrasMap[key] = extras.get(key)?.toString() ?: ""
    }

    val payload = mapOf(
        "package" to pkg,
        "title"   to title,
        "text"    to text,
        "time"    to timestamp.toString(),
        "flags"   to notification.flags,
        "extras"  to extrasMap          // <--- **추가된** 필드
    )

    // Flutter에 전송
    val engine = (applicationContext as MainApplication).flutterEngine
    MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
        .invokeMethod("onNotification", payload)
    }
}
