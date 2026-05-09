import 'package:flutter/foundation.dart';
import 'package:flutter_tts/flutter_tts.dart';

enum TtsChannel { detection, navigation }

class TtsService {
  static final TtsService _instance = TtsService._internal();
  factory TtsService() => _instance;
  TtsService._internal();

  final FlutterTts _tts = FlutterTts();
  bool _initialized = false;
  bool _isSpeaking = false;
  bool _voiceGuidanceEnabled = true;

  // navigation 채널 전용 큐 (detection 중일 때 대기)
  String? _pendingNavMessage;

  static const double defaultSpeechRate = 1.0;

  Future<void> _init() async {
    if (_initialized) return;

    await _tts.setLanguage('ko-KR');
    await _tts.setSpeechRate(defaultSpeechRate);
    await _tts.setVolume(1.0);
    await _tts.setPitch(1.0);

    _tts.setStartHandler(() {
      _isSpeaking = true;
      debugPrint('🔊 [TTS] 음성 출력 시작');
    });

    _tts.setCompletionHandler(() {
      _isSpeaking = false;
      debugPrint('🔊 [TTS] 음성 출력 완료');
      // detection 종료 후 대기 중인 nav 안내 재생
      final pending = _pendingNavMessage;
      if (pending != null) {
        _pendingNavMessage = null;
        debugPrint('🔊 [TTS] 대기 nav 안내 출력: $pending');
        _tts.speak(pending);
      }
    });

    _tts.setCancelHandler(() {
      _isSpeaking = false;
      debugPrint('🔊 [TTS] 음성 출력 취소');
      // 외부 인터럽트(TalkBack 스크롤 피드백 등)로 취소된 경우
      // 대기 중인 nav 안내가 있으면 짧은 지연 후 재시도
      final pending = _pendingNavMessage;
      if (pending != null) {
        _pendingNavMessage = null;
        Future.delayed(const Duration(milliseconds: 600), () {
          if (!_isSpeaking && _voiceGuidanceEnabled) {
            debugPrint('🔊 [TTS] 취소 후 대기 nav 안내 재시도: $pending');
            _isSpeaking = true;
            _tts.speak(pending);
          }
        });
      }
    });

    _tts.setErrorHandler((msg) {
      _isSpeaking = false;
      debugPrint('🔴 [TTS] 오류: $msg');
    });

    _initialized = true;
    debugPrint('🔊 [TTS] 초기화 완료');
  }

  void setVoiceGuidanceEnabled(bool enabled) {
    _voiceGuidanceEnabled = enabled;
    debugPrint('🔊 [TTS] 음성 안내 ${enabled ? '켜짐' : '꺼짐'}');
  }

  /// 우선순위 규칙:
  ///
  ///   상황                         동작
  ///   ─────────────────────────────────────────────────────────────────
  ///   nav step전환 / ≤8m           detection 포함 모든 음성 끊고 즉시 출력
  ///   nav ≤20m / ≤40m             detection 말하는 중이면 큐 대기 → 완료 후 출력
  ///   detection high alert         nav 말하는 중이어도 끊고 즉시 출력
  ///   detection medium / low       말하는 중이면 스킵
  ///   목적지 도착                   detection 끊고 즉시 출력
  ///   ─────────────────────────────────────────────────────────────────
  ///   interrupt=true           → 채널 무관 현재 음성 끊고 즉시 출력
  ///   interrupt=false, nav     → detection 말하는 중이면 큐에 저장 후 완료 시 출력
  ///   interrupt=false, detect  → 말하는 중이면 스킵
  Future<void> speak(
    String text, {
    bool interrupt = false,
    TtsChannel channel = TtsChannel.detection,
  }) async {
    if (text.isEmpty || !_voiceGuidanceEnabled) return;
    await _init();

    if (_isSpeaking) {
      if (interrupt) {
        _pendingNavMessage = null;
        await _tts.stop();
      } else if (channel == TtsChannel.navigation) {
        // detection 말하는 중 → 대기
        _pendingNavMessage = text;
        debugPrint('🔊 [TTS] nav 안내 대기 중: $text');
        return;
      } else {
        // detection → nav 말하는 중이면 스킵
        debugPrint('🔊 [TTS] 출력 중 — 스킵: $text');
        return;
      }
    }

    _pendingNavMessage = null;
    _isSpeaking = true;
    debugPrint('🔊 [TTS] 출력: $text');
    await _tts.speak(text);
  }

  Future<void> setSpeechRate(double rate) async {
    await _init();
    await _tts.setSpeechRate(rate.clamp(0.5, 5.0));
  }

  Future<void> stop() async {
    _pendingNavMessage = null;
    await _tts.stop();
    _isSpeaking = false;
    debugPrint('🔊 [TTS] 중지');
  }
}