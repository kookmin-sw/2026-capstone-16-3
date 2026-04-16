import 'package:flutter/foundation.dart';
import 'package:flutter_tts/flutter_tts.dart';

/// =======================================================
/// TtsService
///
/// 역할:
///   - 장애물 탐지 결과(guideText)를 한국어 음성으로 출력
///   - alertLevel 기반 우선순위 처리
///     · high   → 진행 중인 음성을 중단하고 즉시 출력
///     · medium → 현재 말하는 중이 아닐 때만 출력
///     · low    → 현재 말하는 중이 아닐 때만 출력
///
/// 호출 위치:
///   - DetectionScreen: 탐지 이벤트 수신 시 speak() 호출
///   - DetectionScreen: 탐지 중지 시 stop() 호출
/// =======================================================
class TtsService {
  static final TtsService _instance = TtsService._internal();
  factory TtsService() => _instance;
  TtsService._internal();

  final FlutterTts _tts = FlutterTts();
  bool _initialized = false;
  bool _isSpeaking = false;

  // ─── 초기화 ────────────────────────────────────────────────────────────────

  Future<void> _init() async {
    if (_initialized) return;

    await _tts.setLanguage('ko-KR');
    await _tts.setSpeechRate(0.45); // 너무 빠르면 안내문 알아듣기 어려움
    await _tts.setVolume(1.0);
    await _tts.setPitch(1.0);

    _tts.setStartHandler(() {
      _isSpeaking = true;
      debugPrint('🔊 [TTS] 음성 출력 시작');
    });

    _tts.setCompletionHandler(() {
      _isSpeaking = false;
      debugPrint('🔊 [TTS] 음성 출력 완료');
    });

    _tts.setCancelHandler(() {
      _isSpeaking = false;
      debugPrint('🔊 [TTS] 음성 출력 취소');
    });

    _tts.setErrorHandler((msg) {
      _isSpeaking = false;
      debugPrint('🔴 [TTS] 오류: $msg');
    });

    _initialized = true;
    debugPrint('🔊 [TTS] 초기화 완료');
  }

  // ─── 음성 출력 ─────────────────────────────────────────────────────────────

  /// guideText를 음성으로 출력한다.
  ///
  /// [interrupt] = true  → 진행 중인 음성을 중단하고 즉시 출력 (high alert용)
  /// [interrupt] = false → 이미 말하는 중이면 스킵 (medium / low alert용)
  Future<void> speak(String text, {bool interrupt = false}) async {
    if (text.isEmpty) return;

    await _init();

    if (_isSpeaking) {
      if (interrupt) {
        await _tts.stop();
      } else {
        debugPrint('🔊 [TTS] 출력 중 — 스킵: $text');
        return;
      }
    }

    debugPrint('🔊 [TTS] 출력: $text');
    await _tts.speak(text);
  }

  // ─── 중지 ──────────────────────────────────────────────────────────────────

  /// 진행 중인 음성 출력을 즉시 중단한다.
  Future<void> stop() async {
    await _tts.stop();
    _isSpeaking = false;
    debugPrint('🔊 [TTS] 중지');
  }
}
