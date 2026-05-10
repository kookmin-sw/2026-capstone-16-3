import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:safepath/common/widgets/permission_onboarding_sheet.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/detection/detection_active_view.dart';
import 'package:safepath/features/detection/detection_idle_view.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/detection_ws_service.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/vibration_service.dart';

class DetectionScreen extends StatefulWidget {
  final ValueChanged<bool>? onDetectingChanged;

  const DetectionScreen({super.key, this.onDetectingChanged});

  @override
  State<DetectionScreen> createState() => _DetectionScreenState();
}

class _DetectionScreenState extends State<DetectionScreen> {
  bool _isDetecting = false;

  /// WS로 수신한 최신 탐지 결과 목록 (최근 3개까지 유지)
  final List<DetectionEvent> _obstacles = [];

  /// 탐지 시작 후 수신된 이벤트 총 횟수
  int _detectedCount = 0;

  /// WS STOMP 연결 완료 여부
  bool _wsConnected = false;

  StreamSubscription<DetectionEvent>? _wsSub;

  // ─── 탐지 시작 ───────────────────────────────────────────────────────────

  Future<void> _startDetection() async {
    // 시트 내부에서 거부 유형별 다이얼로그 안내가 완료된 후 false 반환
    if (!await PermissionOnboardingSheet.show(context, needsLocation: false)) return;

    SoundEffectService().play(SoundEffect.actionStart);
    VibrationService().vibrate(VibrationEffect.actionStart);
    await CameraService().start(CameraMode.detection);
    await DetectionWsService().connect(
      onConnected: () {
        if (mounted) setState(() => _wsConnected = true);
      },
      onDisconnected: () {
        if (mounted) setState(() => _wsConnected = false);
      },
    );

    _wsSub = DetectionWsService().eventStream?.listen(_onDetectionEvent);

    await SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    setState(() {
      _isDetecting = true;
      _wsConnected = false;
      _obstacles.clear();
      _detectedCount = 0;
    });

    widget.onDetectingChanged?.call(true);
  }

  // ─── 탐지 중지 ───────────────────────────────────────────────────────────

  Future<void> _stopDetection() async {
    SoundEffectService().play(SoundEffect.actionStop);
    VibrationService().vibrate(VibrationEffect.actionStop);
    await _wsSub?.cancel();
    _wsSub = null;

    await TtsService().stop();
    await CameraService().stop();
    await DetectionWsService().disconnect();

    // 세로 모드로 복원
    await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);

    setState(() {
      _isDetecting = false;
      _wsConnected = false;
    });

    widget.onDetectingChanged?.call(false);
  }

  // ─── WS 이벤트 수신 ──────────────────────────────────────────────────────

  void _onDetectionEvent(DetectionEvent event) {
    setState(() {
      if (!event.isActive) {
        // 종료 이벤트: 해당 userId 카드 제거
        _obstacles.removeWhere((e) => e.userId == event.userId);
        return;
      }
      final idx = _obstacles.indexWhere((e) => e.userId == event.userId);
      if (idx != -1) {
        // 동일 ID 갱신: 위치 유지, 데이터만 업데이트
        _obstacles[idx] = event;
      } else {
        // 신규 장애물: 목록 맨 앞에 추가
        _detectedCount++;
        _obstacles.insert(0, event);
      }
    });

    if (!event.isActive) return;

    // 장애물 위험 등급별 진동 피드백
    VibrationService().vibrate(switch (event.alertLevel) {
      'high' => VibrationEffect.obstacleLevel3,
      'medium' => VibrationEffect.obstacleLevel2,
      _ => VibrationEffect.obstacleLevel1,
    });

    // high → 진행 중인 음성 중단 후 즉시 출력
    // medium / low → 말하는 중이 아닐 때만 출력
    if (event.guideText.isNotEmpty) {
      TtsService().speak(
        event.guideText,
        interrupt: event.alertLevel == 'high',
      );
    }
  }

  @override
  void dispose() {
    _wsSub?.cancel();
    super.dispose();
  }

  // ─── 빌드 ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_isDetecting,
      child: Scaffold(
        // active 상태일 때 AppBar 없음
        appBar: _isDetecting ? null : const CustomTitleBar(title: '실외 장애물 탐지'),
        body: SafeArea(
          child: _isDetecting
              ? DetectionActiveView(
                  onStop: _stopDetection,
                  detectedCount: _detectedCount,
                  obstacles: _obstacles,
                  wsConnected: _wsConnected,
                )
              : DetectionIdleView(onStart: _startDetection),
        ),
      ),
    );
  }
}
