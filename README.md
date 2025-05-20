TODO 앱이 계속 실행되는지 확인


작업 파일 2025.05.20


File: android/app/src/main/kotlin/com/example/notif_webhook_app/MainActivity.kt
kotlin
Copy
Edit
package com.example.notif_webhook_app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.flutter.embedding.android.FlutterActivity

/**
 * MainActivity
 *
 * FlutterActivity를 상속하여 Flutter 엔진 실행 및 UI를 담당.
 * 앱 시작 시 Notification Listener 권한이 활성화되어 있지 않으면
 * 설정 화면으로 자동 이동시켜 사용자 권한 허용을 유도.
 */
class MainActivity : FlutterActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
    }

    /**
     * Notification Listener 권한이 없으면
     * 시스템 설정 → 알림 접근 화면으로 이동시킴
     */
    private fun checkNotificationPermission() {
        val enabledListeners = NotificationManagerCompat
            .getEnabledListenerPackages(this)
        if (!enabledListeners.contains(packageName)) {
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
File: android/app/src/main/kotlin/com/example/notif_webhook_app/MainApplication.kt
kotlin
Copy
Edit
package com.example.notif_webhook_app

import android.app.Application
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor

/**
 * MainApplication
 *
 * Application 레벨에서 FlutterEngine 을 미리 생성하여
 * NotificationListenerService 에서 언제든 접근 가능하도록 함.
 */
class MainApplication : Application() {

    // 전역 FlutterEngine 인스턴스
    lateinit var flutterEngine: FlutterEngine

    override fun onCreate() {
        super.onCreate()

        // FlutterEngine 초기화
        flutterEngine = FlutterEngine(this).apply {
            // Dart entrypoint 실행 (main.dart)
            dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
            )
        }
    }
}
File: android/app/src/main/kotlin/com/example/notif_webhook_app/MyNotificationListenerService.kt
kotlin
Copy
Edit
package com.example.notif_webhook_app

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.flutter.plugin.common.MethodChannel

/**
 * MyNotificationListenerService
 *
 * 시스템에 게시된 모든 Notification 을 수신.
 * - 그룹 요약(summary) 및 빈 알림을 필터링
 * - extras 를 포함한 payload 구성
 * - MethodChannel 을 통해 Flutter 쪽으로 전달
 */
class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifService"
        private const val CHANNEL = "notif_channel"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification

        // 1) 그룹 요약 알림 무시 (Android N 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        ) {
            Log.d(TAG, "Summary ignored: pkg=${sbn.packageName}, id=${sbn.id}")
            return
        }

        // 2) 제목·본문 비어있으면 무시
        val extras = notification.extras
        val title  = extras.getString("android.title") ?: ""
        val text   = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) {
            Log.d(TAG, "Empty notification ignored: pkg=${sbn.packageName}")
            return
        }

        // 3) 실제 처리할 알림 데이터 준비
        val pkg       = sbn.packageName
        val timestamp = sbn.postTime
        Log.d(TAG, "Notification received → pkg:$pkg, title:$title, text:$text, time:$timestamp")

        // 4) extras 전체 맵으로 변환
        val extrasMap = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            extrasMap[key] = extras.get(key)?.toString() ?: ""
        }

        // 5) Flutter 로 보낼 payload 구성
        val payload = mapOf(
            "package" to pkg,
            "title"   to title,
            "text"    to text,
            "time"    to timestamp.toString(),
            "flags"   to notification.flags,
            "extras"  to extrasMap
        )

        // 6) MethodChannel 을 통해 Flutter 에 전달
        val engine = (applicationContext as MainApplication).flutterEngine
        MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
            .invokeMethod("onNotification", payload)
    }
}
File: android/app/src/main/AndroidManifest.xml
xml
Copy
Edit
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.notif_webhook_app">

    <!-- 네트워크 및 알림 리스너 권한 -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"/>

    <application
        android:name="com.example.notif_webhook_app.MainApplication"
        android:label="Notification Webhook"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="true"
        android:theme="@style/LaunchTheme">

        <!-- Flutter 런처 액티비티 -->
        <activity
            android:name="com.example.notif_webhook_app.MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:theme="@style/LaunchTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- NotificationListenerService 등록 -->
        <service
            android:name="com.example.notif_webhook_app.MyNotificationListenerService"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService"/>
            </intent-filter>
        </service>

    </application>
</manifest>
File: pubspec.yaml
yaml
Copy
Edit
name: notif_webhook_app
description: Flutter 앱에서 알림을 수신해 Google Sheets 로 전송
publish_to: 'none'

version: 1.0.0+1

environment:
  sdk: ">=2.18.0 <3.0.0"

dependencies:
  flutter:
    sdk: flutter

  # HTTP 요청용 패키지
  http: ^0.13.0

flutter:
  uses-material-design: true
File: lib/notification_service.dart
dart
Copy
Edit
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

/// NotificationService
///
/// MethodChannel 을 통해 네이티브(Android) 에서 전달된
/// 알림 데이터를 수신하고,
/// 반복 필터링, 로깅 후 Google Sheets 로 전송합니다.
class NotificationService {
  static const _channel = MethodChannel('notif_channel');

  // Google Apps Script Web App URL
  static const _scriptUrl =
      'https://script.google.com/macros/s/AKfycbw9U8MeSEWzfOkuLRl-mLGPTOnpKz0m4cTOAatZ-VfgLpm92ua0AA5--KDaEvdesojB/exec';

  /// 채널 핸들러 등록
  void init() {
    _channel.setMethodCallHandler((call) async {
      if (call.method != 'onNotification') return;

      // 1) 전체 페이로드(raw) 로그
      print('[NotifService RAW] ${call.arguments}');

      final args = Map<String, dynamic>.from(call.arguments);

      // 2) extras 맵만 로깅
      final extras = Map<String, dynamic>.from(args['extras'] ?? {});
      print('=== Notification Extras ===');
      extras.forEach((k, v) => print('$k: $v'));
      print('============================');

      // 3) 주요 필드 파싱
      final pkg   = args['package'] as String? ?? '';
      final title = args['title']   as String? ?? '';
      final text  = args['text']    as String? ?? '';

      print('[NotifService] pkg:$pkg, title:$title, text:$text');

      // 4) Google Sheet 에 전송
      await _sendToSheet(title, text, pkg);
    });
  }

  /// Google Sheets 로 데이터를 POST
  Future<void> _sendToSheet(
      String title, String text, String appPackage) async {
    final payload = {
      'title': title,
      'text': text,
      'app': appPackage,
    };

    try {
      final response = await http.post(
        Uri.parse(_scriptUrl),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(payload),
      );
      print('[Webhook] status=${response.statusCode}, body=${response.body}');
    } catch (e) {
      // 프로덕션: Sentry 등 에러 리포팅 툴에 전송 가능
      print('[Webhook] error: $e');
    }
  }
}
File: lib/main.dart
dart
Copy
Edit
import 'package:flutter/material.dart';
import 'notification_service.dart';

/// 앱 진입점
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // 알림 서비스 초기화
  NotificationService().init();
  runApp(const MyApp());
}

/// MyApp
///
/// 단순 UI: 백그라운드에서 알림 리스너가 동작 중임을 표시
class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: Center(
          child: Text(
            'Notification → Webhook',
            style: TextStyle(fontSize: 16),
          ),
        ),
      ),
    );
  }
}
