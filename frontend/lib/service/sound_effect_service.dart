import 'package:audioplayers/audioplayers.dart';
import 'package:flutter/foundation.dart';

/// 효과음 종류
enum SoundEffect {
  /// 버튼 / 토글 터치음
  buttonTap,

  /// 탐지 시작 / 길찾기 시작음
  actionStart,

  /// 탐지 중지 / 길찾기 중지음
  actionStop,
}

/// =======================================================
/// SoundEffectService
///
/// 역할:
///   - 버튼 터치 및 동작(탐지/길찾기 시작·중지) 효과음 출력
///   - soundEffectEnabled = false 이면 모든 소리 무음 처리
///
/// 음원 파일 위치: assets/sounds/
///   - button_click.wav  : 버튼 터치음 (짧은 클릭, ~50ms)
///   - action_start.wav: 시작 효과음 (상승 톤, ~300ms)
///   - action_stop.wav : 종료 효과음 (하강 톤, ~300ms)
///
/// 플레이어 분리:
///   - _tapPlayer    : buttonTap 전용 — actionStart/Stop과 동시 재생 가능
///   - _actionPlayer : actionStart/Stop 전용
///
/// 무료 음원: https://freesound.org (CC0 라이선스 필터)
/// =======================================================
class SoundEffectService {
  static final SoundEffectService _instance = SoundEffectService._internal();
  factory SoundEffectService() => _instance;
  SoundEffectService._internal();

  // buttonTap 전용 — actionStart/Stop과 분리해 동시 재생 허용
  AudioPlayer? _tapPlayer;

  // actionStart / actionStop 전용
  AudioPlayer? _actionPlayer;

  bool _enabled = true;

  static final _tapContext = AudioContext(
    android: const AudioContextAndroid(
      contentType: AndroidContentType.sonification,
      usageType: AndroidUsageType.assistanceSonification,
      audioFocus: AndroidAudioFocus.none, // 다른 오디오 간섭 없이 즉시 재생
      stayAwake: false,
      isSpeakerphoneOn: false,
    ),
    iOS: AudioContextIOS(
      category: AVAudioSessionCategory.playback,
      options: {AVAudioSessionOptions.mixWithOthers}, // TTS와 동시 재생
    ),
  );

  static final _actionContext = AudioContext(
    android: const AudioContextAndroid(
      contentType: AndroidContentType.sonification,
      usageType: AndroidUsageType.assistanceSonification,
      audioFocus: AndroidAudioFocus.gainTransientMayDuck,
      stayAwake: false,
      isSpeakerphoneOn: false,
    ),
    iOS: AudioContextIOS(
      category: AVAudioSessionCategory.playback,
      options: {AVAudioSessionOptions.mixWithOthers}, // TTS와 동시 재생
    ),
  );

  AudioPlayer get _getTapPlayer {
    if (_tapPlayer == null) {
      _tapPlayer = AudioPlayer();
      _tapPlayer!.setAudioContext(_tapContext);
    }
    return _tapPlayer!;
  }

  AudioPlayer get _getActionPlayer {
    if (_actionPlayer == null) {
      _actionPlayer = AudioPlayer();
      _actionPlayer!.setAudioContext(_actionContext);
    }
    return _actionPlayer!;
  }

  void setEnabled(bool enabled) {
    _enabled = enabled;
    debugPrint('🔔 [Sound] 효과음 ${enabled ? '켜짐' : '꺼짐'}');
  }

  bool get isEnabled => _enabled;

  Future<void> play(SoundEffect effect) async {
    if (!_enabled) return;
    final (player, path) = switch (effect) {
      SoundEffect.buttonTap => (_getTapPlayer, 'sounds/button_click.wav'),
      SoundEffect.actionStart => (_getActionPlayer, 'sounds/action_start.wav'),
      SoundEffect.actionStop => (_getActionPlayer, 'sounds/action_stop.wav'),
    };
    try {
      await player.play(AssetSource(path));
      debugPrint('🔔 [Sound] 재생: ${effect.name}');
    } catch (e) {
      debugPrint('🔴 [Sound] 재생 실패 ($path): $e');
    }
  }
}
