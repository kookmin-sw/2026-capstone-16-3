import 'package:flutter/foundation.dart';
import 'package:vibration/vibration.dart';

// ─── 진동 효과 종류 ───────────────────────────────────────────────────────────

enum VibrationEffect {
  /// 버튼 터치 확인 (가장 짧고 가볍게 — 시각장애인 촉각 피드백 필수)
  buttonTap,

  /// 탐지/길찾기 시작
  actionStart,

  /// 탐지/길찾기 중지
  actionStop,

  /// 장애물 낮은 위험 (low)
  obstacleLevel1,

  /// 장애물 중간 위험 (medium)
  obstacleLevel2,

  /// 장애물 높은 위험 (high) — 즉각 주의 필요
  obstacleLevel3,
}

// ─── 진동 서비스 ──────────────────────────────────────────────────────────────

class VibrationService {
  static final VibrationService _instance = VibrationService._internal();
  factory VibrationService() => _instance;
  VibrationService._internal();

  /// 진동 강도: 0(무진동)~100(최대), 설정 화면에서 주입
  int _strength = 50;

  bool? _hasVibrator;
  bool? _hasAmplitudeControl;

  // ─── 우선순위 (낮은 priority 진동이 높은 것을 끊지 못하도록) ──────────────
  static const Map<VibrationEffect, int> _priorities = {
    VibrationEffect.buttonTap: 0,
    VibrationEffect.actionStop: 1,
    VibrationEffect.actionStart: 1,
    VibrationEffect.obstacleLevel1: 2,
    VibrationEffect.obstacleLevel2: 3,
    VibrationEffect.obstacleLevel3: 4,
  };

  /// 현재 진행 중인 진동의 우선순위와 예상 종료 시각
  int _currentPriority = -1;
  DateTime _currentEnd = DateTime(0);

  // ─── 효과별 최소 재실행 간격(ms) — 스팸 방지 ────────────────────────────
  static const Map<VibrationEffect, int> _cooldowns = {
    VibrationEffect.buttonTap: 100,
    VibrationEffect.actionStart: 400,
    VibrationEffect.actionStop: 400,
    VibrationEffect.obstacleLevel1: 700,
    VibrationEffect.obstacleLevel2: 600,
    VibrationEffect.obstacleLevel3: 450, // 긴급이므로 더 자주 허용
  };

  final Map<VibrationEffect, DateTime> _lastFired = {};

  void setStrength(int value) => _strength = value.clamp(0, 100);

  Future<void> vibrate(VibrationEffect effect) async {
    if (_strength == 0) return;

    final now = DateTime.now();
    final priority = _priorities[effect]!;

    // 더 높은 우선순위 진동이 진행 중이면 낮은 것은 건너뜀
    if (now.isBefore(_currentEnd) && priority < _currentPriority) return;

    // 같은 효과의 연속 호출 쿨다운
    final last = _lastFired[effect];
    final cooldown = _cooldowns[effect] ?? 300;
    if (last != null && now.difference(last).inMilliseconds < cooldown) return;
    _lastFired[effect] = now;

    _hasVibrator ??= await Vibration.hasVibrator() ?? false;
    if (!_hasVibrator!) return;

    _hasAmplitudeControl ??= await Vibration.hasAmplitudeControl() ?? false;

    final p = _patterns[effect]!;

    // 기존 진동 중단 후 새 패턴 시작
    await Vibration.cancel();

    final totalMs = p.durations.fold(0, (sum, d) => sum + d);
    _currentPriority = priority;
    _currentEnd = now.add(Duration(milliseconds: totalMs));

    try {
      // iOS는 amplitude 미지원 → 패턴만 사용
      if (defaultTargetPlatform != TargetPlatform.iOS &&
          _hasAmplitudeControl!) {
        final amp = (_strength * 255 / 100).round().clamp(1, 255);
        final scaled = p.amplitudes.map((a) => a == 0 ? 0 : amp).toList();
        await Vibration.vibrate(pattern: p.durations, intensities: scaled);
      } else {
        await Vibration.vibrate(pattern: p.durations);
      }
    } catch (e) {
      debugPrint('🔴 [Vibration] 진동 실패 ($effect): $e');
    }
  }

  // ─── 패턴 정의 ─────────────────────────────────────────────────────────────
  // durations: [초기대기, 진동, 쉬기, 진동, ...]
  // amplitudes: 대응하는 강도(0=쉬기, 1~255=기준 세기) → _strength 비율로 스케일

  static final Map<VibrationEffect, _VibPattern> _patterns = {
    VibrationEffect.buttonTap: const _VibPattern(
      // 25ms 단일 펄스 — 버튼 확인용으로만 쓰이는 가벼운 진동
      durations: [0, 25],
      amplitudes: [0, 80],
    ),
    VibrationEffect.actionStart: const _VibPattern(
      // 짧은-긴 두 박자 "시작" 리듬
      durations: [0, 60, 80, 120],
      amplitudes: [0, 160, 0, 220],
    ),
    VibrationEffect.actionStop: const _VibPattern(
      // 긴 단일 펄스 — 종료를 명확하게
      durations: [0, 180],
      amplitudes: [0, 180],
    ),
    VibrationEffect.obstacleLevel1: const _VibPattern(
      // 단일 짧은 펄스 (낮은 위험)
      durations: [0, 100],
      amplitudes: [0, 120],
    ),
    VibrationEffect.obstacleLevel2: const _VibPattern(
      // 두 번 중간 펄스 (중간 위험)
      durations: [0, 200, 20, 200],
      amplitudes: [0, 190, 0, 190],
    ),
    VibrationEffect.obstacleLevel3: const _VibPattern(
      // 세 번 강한 펄스 (높은 위험 — 즉각 주의)
      durations: [0, 350, 20, 350, 20, 350],
      amplitudes: [0, 255, 0, 255, 0, 255],
    ),
  };
}

class _VibPattern {
  final List<int> durations;
  final List<int> amplitudes;
  const _VibPattern({required this.durations, required this.amplitudes});
}
