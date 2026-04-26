import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:safepath/service/api_client.dart';

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

  /// TTS 실제 재생 속도 (0.5배속 ~ 5.0배속)
  final double guidanceSpeed;

  const UserSettings({
    required this.sentenceLength,
    required this.vibrationStrength,
    required this.voiceGuidanceEnabled,
    required this.soundEffectEnabled,
    required this.guidanceSpeed,
  });

  static const UserSettings defaults = UserSettings(
    sentenceLength: MessageLength.medium,
    vibrationStrength: 1073741824,
    voiceGuidanceEnabled: true,
    soundEffectEnabled: true,
    guidanceSpeed: 2.0,
  );

  factory UserSettings.fromJson(Map<String, dynamic> json) => UserSettings(
    sentenceLength: MessageLength.fromBackend(json['sentenceLength'] as String?),
    vibrationStrength:
        (json['vibrationStrength'] as num?)?.toInt() ?? 1073741824,
    voiceGuidanceEnabled: json['voiceGuidanceEnabled'] as bool? ?? true,
    soundEffectEnabled: json['soundEffectEnabled'] as bool? ?? true,
    guidanceSpeed: (json['guidanceSpeed'] as num?)?.toDouble() ?? 2.0,
  );

  Map<String, dynamic> toJson() => {
    'sentenceLength': sentenceLength.backendValue,
    'vibrationStrength': vibrationStrength,
    'voiceGuidanceEnabled': voiceGuidanceEnabled,
    'soundEffectEnabled': soundEffectEnabled,
    'guidanceSpeed': double.parse(guidanceSpeed.toStringAsFixed(1)),
  };

  UserSettings copyWith({
    MessageLength? sentenceLength,
    int? vibrationStrength,
    bool? voiceGuidanceEnabled,
    bool? soundEffectEnabled,
    double? guidanceSpeed,
  }) => UserSettings(
    sentenceLength: sentenceLength ?? this.sentenceLength,
    vibrationStrength: vibrationStrength ?? this.vibrationStrength,
    voiceGuidanceEnabled: voiceGuidanceEnabled ?? this.voiceGuidanceEnabled,
    soundEffectEnabled: soundEffectEnabled ?? this.soundEffectEnabled,
    guidanceSpeed: guidanceSpeed ?? this.guidanceSpeed,
  );
}

// ─── API 서비스 ───────────────────────────────────────────────────────────────

class UserSettingsService {
  static final UserSettingsService _instance = UserSettingsService._internal();
  factory UserSettingsService() => _instance;
  UserSettingsService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  Future<UserSettings> fetch() async {
    final uri = Uri.parse('$_baseUrl/api/users/me/settings');
    debugPrint('🟢 [Settings] GET 요청: $uri');
    try {
      final response = await ApiClient().get(uri);
      debugPrint('🟢 [Settings] GET 응답: ${response.statusCode} / ${response.body}');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        final settings = UserSettings.fromJson(body['data'] as Map<String, dynamic>);
        debugPrint(
          '✅ [Settings] GET 성공 → '
          'sentenceLength=${settings.sentenceLength.backendValue}, '
          'vibration=${settings.vibrationStrength}, '
          'voiceGuide=${settings.voiceGuidanceEnabled}, '
          'soundEffect=${settings.soundEffectEnabled}, '
          'speed=${settings.guidanceSpeed}',
        );
        return settings;
      }
      debugPrint('🔴 [Settings] GET 실패: ${response.statusCode}');
    } catch (e) {
      debugPrint('🔴 [Settings] GET 오류: $e');
    }
    debugPrint('⚠️ [Settings] 기본값으로 fallback');
    return UserSettings.defaults;
  }

  Future<void> patch(UserSettings settings) async {
    final uri = Uri.parse('$_baseUrl/api/users/me/settings');
    debugPrint(
      '🟢 [Settings] PATCH 요청: $uri / body=${settings.toJson()}',
    );
    try {
      final response = await ApiClient().patch(uri, body: settings.toJson());
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
