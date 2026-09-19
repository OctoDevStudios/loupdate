import 'package:flutter/services.dart';

import 'loupdate_config.dart';

class Loupdate {
  static const MethodChannel _channel = MethodChannel('com.loupdate/flutter');

  /// Call once at startup (e.g. in main() after WidgetsFlutterBinding).
  static Future<void> init(LoupdateConfig config) {
    return _channel.invokeMethod<void>('init', config.toMap());
  }

  /// Check for updates and show soft/force UI when needed.
  static Future<String> check() async {
    final result = await _channel.invokeMethod<String>('check');
    return result ?? 'unknown';
  }
}
