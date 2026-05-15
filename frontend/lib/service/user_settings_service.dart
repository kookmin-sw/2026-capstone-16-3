import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:safepath/service/api_client.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ─── 안내 문장 길이 enum ──────────────────────────────────────────────────────

enum MessageLength {
  short,
  medium,
  long;

  /// 백엔드 통신값: SHORT / MEDIUM / LONG
  String get backendValue => name.toUpperCase();

  /// 슬라이더 double 값: 0.0 / 0.5 / 1.0
  double get sliderValue => index / 2.0;

  String get displayLabel {
    switch (this) {
      case MessageLength.short:
        return '간결';
      case MessageLength.medium:
        return '보통';
      case MessageLength.long:
        return '상세';
    }
  }

  static MessageLength fromSlider(double v) {
    if (v < 0.25) return MessageLength.short;
    if (v < 0.75) return MessageLength.medium;
    return MessageLength.long;
  }

  static MessageLength fromBackend(String? value) {
    return MessageLength.values.firstWhere(
      (e) => e.backendValue == value,
      orElse: () => MessageLength.medium,
    );
  }
}

// ─── 설정 데이터 모델 ─────────────────────────────────────────────────────────

class UserSettings {
  final MessageLength sentenceLength;
  final int vibrationStrength;
  final bool voiceGuidanceEnabled;
  final bool soundEffectEnabled;

  /// 사용자의 푸시 알림 수신 의사 (OS 권한과 별개)
  /// OFF → 추후 발송 로직에서 이 값을 보고 전송 스킵
  final bool pushNotificationEnabled;

  /// TTS 실제 재생 속도 (0.5배속 ~ 5.0배속)
  final double guidanceSpeed;

  const UserSettings({
    required this.sentenceLength,
    required this.vibrationStrength,
    required this.voiceGuidanceEnabled,
    required this.soundEffectEnabled,
    required this.pushNotificationEnabled,
    required this.guidanceSpeed,
  });

  static const UserSettings defaults = UserSettings(
    sentenceLength: MessageLength.medium,
    vibrationStrength: 50,
    voiceGuidanceEnabled: true,
    soundEffectEnabled: true,
    pushNotificationEnabled: true,
    guidanceSpeed: 2.0,
  );

  /// 서버 응답에서 파싱. 로컬 전용 필드(soundEffect, pushNotification)는 포함하지 않음.
  factory UserSettings.fromJson(Map<String, dynamic> json) => UserSettings(
    sentenceLength: MessageLength.fromBackend(json['sentenceLength'] as String?),
    vibrationStrength: (json['vibrationStrength'] as num?)?.toInt() ?? 50,
    voiceGuidanceEnabled: json['voiceGuidanceEnabled'] as bool? ?? true,
    soundEffectEnabled: true,   // 로컬 전용 — fetch()에서 SharedPreferences로 덮어씀
    pushNotificationEnabled: true, // 로컬 전용 — fetch()에서 SharedPreferences로 덮어씀
    guidanceSpeed: (json['guidanceSpeed'] as num?)?.toDouble() ?? 2.0,
  );

  /// 서버에 전송할 필드만 포함. 로컬 전용 필드는 제외.
  Map<String, dynamic> toServerJson() => {
    'sentenceLength': sentenceLength.backendValue,
    'vibrationStrength': vibrationStrength,
    'voiceGuidanceEnabled': voiceGuidanceEnabled,
    'guidanceSpeed': double.parse(guidanceSpeed.toStringAsFixed(1)),
  };

  UserSettings copyWith({
    MessageLength? sentenceLength,
    int? vibrationStrength,
    bool? voiceGuidanceEnabled,
    bool? soundEffectEnabled,
    bool? pushNotificationEnabled,
    double? guidanceSpeed,
  }) => UserSettings(
    sentenceLength: sentenceLength ?? this.sentenceLength,
    vibrationStrength: vibrationStrength ?? this.vibrationStrength,
    voiceGuidanceEnabled: voiceGuidanceEnabled ?? this.voiceGuidanceEnabled,
    soundEffectEnabled: soundEffectEnabled ?? this.soundEffectEnabled,
    pushNotificationEnabled:
        pushNotificationEnabled ?? this.pushNotificationEnabled,
    guidanceSpeed: guidanceSpeed ?? this.guidanceSpeed,
  );
}

// ─── API 서비스 ───────────────────────────────────────────────────────────────

class UserSettingsService {
  static final UserSettingsService _instance = UserSettingsService._internal();
  factory UserSettingsService() => _instance;
  UserSettingsService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  MessageLength _cachedSentenceLength = MessageLength.medium;
  MessageLength get sentenceLength => _cachedSentenceLength;

  // 로컬 전용 설정 키 (서버 비관여)
  static const String _kPushNotif = 'push_notification_enabled';
  static const String _kSoundEffect = 'sound_effect_enabled';

  Future<UserSettings> fetch() async {
    final uri = Uri.parse('$_baseUrl/api/users/me/settings');
    debugPrint('🟢 [Settings] GET 요청: $uri');

    UserSettings serverSettings;
    try {
      final response = await ApiClient().get(uri);
      debugPrint('🟢 [Settings] GET 응답: ${response.statusCode} / ${response.body}');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        serverSettings = UserSettings.fromJson(body['data'] as Map<String, dynamic>);
      } else {
        debugPrint('🔴 [Settings] GET 실패: ${response.statusCode}');
        serverSettings = UserSettings.defaults;
      }
    } catch (e) {
      debugPrint('🔴 [Settings] GET 오류: $e');
      serverSettings = UserSettings.defaults;
    }

    // 로컬 전용 필드는 SharedPreferences 값 우선 적용
    final prefs = await SharedPreferences.getInstance();
    final localPush = prefs.getBool(_kPushNotif);
    final localSound = prefs.getBool(_kSoundEffect);
    final settings = serverSettings.copyWith(
      pushNotificationEnabled: localPush,
      soundEffectEnabled: localSound,
    );

    _cachedSentenceLength = settings.sentenceLength;
    debugPrint(
      '✅ [Settings] 로드 완료 → '
      'sentenceLength=${settings.sentenceLength.backendValue}, '
      'vibration=${settings.vibrationStrength}, '
      'voiceGuide=${settings.voiceGuidanceEnabled}, '
      'soundEffect=${settings.soundEffectEnabled} (local=${localSound ?? "없음"}), '
      'pushNotification=${settings.pushNotificationEnabled} (local=${localPush ?? "없음"}), '
      'speed=${settings.guidanceSpeed}',
    );
    return settings;
  }

  Future<void> patch(UserSettings settings) async {
    // 로컬 전용 필드는 SharedPreferences에 즉시 저장
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_kPushNotif, settings.pushNotificationEnabled);
    await prefs.setBool(_kSoundEffect, settings.soundEffectEnabled);

    _cachedSentenceLength = settings.sentenceLength;

    // 서버에는 서버 관리 필드만 전송
    final uri = Uri.parse('$_baseUrl/api/users/me/settings');
    debugPrint('🟢 [Settings] PATCH 요청: $uri / body=${settings.toServerJson()}');
    try {
      final response = await ApiClient().patch(uri, body: settings.toServerJson());
      debugPrint('🟢 [Settings] PATCH 응답: ${response.statusCode} / ${response.body}');
      if (response.statusCode == 200) {
        debugPrint('✅ [Settings] PATCH 성공');
      } else {
        debugPrint('🔴 [Settings] PATCH 실패: ${response.statusCode}');
      }
    } catch (e) {
      debugPrint('🔴 [Settings] PATCH 오류: $e');
    }
  }
}
