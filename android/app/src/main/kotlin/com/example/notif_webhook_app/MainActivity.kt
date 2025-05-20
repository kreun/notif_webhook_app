package com.example.notif_webhook_app

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import io.flutter.embedding.android.FlutterActivity

class MainActivity: FlutterActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkNotificationPermission()
  }

  private fun checkNotificationPermission() {
    val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
    if (!enabledPackages.contains(packageName)) {
      // 알림 접근 권한이 꺼져 있으면 설정 화면으로
      startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
  }
}