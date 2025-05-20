// lib/notification_service.dart

import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

class NotificationService {
  static const _channel = MethodChannel('notif_channel');
  // 여기에 본인의 Apps Script Web App URL을 넣어주세요.
  static const _scriptUrl = 'https://script.google.com/macros/s/AKfycbw9U8MeSEWzfOkuLRl-mLGPTOnpKz0m4cTOAatZ-VfgLpm92ua0AA5--KDaEvdesojB/exec';

  void init() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onNotification') {
        // final args = Map<String, dynamic>.from(call.arguments);

        final rawArgs = call.arguments;
        print('[NotifService RAW] $rawArgs');
        final args = Map<String, dynamic>.from(rawArgs);
        final extras = Map<String, dynamic>.from(args['extras'] ?? {});
        print('=== Notification Extras ===');
        extras.forEach((k, v) => print('$k: $v'));
        print('============================');

        final pkg = args['package'] as String? ?? '';
        final title = args['title'] as String? ?? '';
        final text = args['text'] as String? ?? '';

        // 터미널에 로그 남기기
        print('[NotifService] → pkg:$pkg, title:$title, text:$text');

        // 시트로 전송
        await _sendToSheet(title, text, pkg);
      }
    });
  }

  Future<void> _sendToSheet(String title, String text, String app) async {
    final payload = {
      'title': title,
      'text': text,
      'app': app,
    };

    try {
      final res = await http.post(
        Uri.parse(_scriptUrl),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(payload),
      );
      print('[Webhook] status=${res.statusCode}, body=${res.body}');
    } catch (e) {
      print('[Webhook] error: $e');
    }
  }
}
