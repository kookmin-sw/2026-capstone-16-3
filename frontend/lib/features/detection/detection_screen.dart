import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/detection/detection_active_view.dart';
import 'package:safepath/features/detection/detection_idle_view.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/on_device_detection_service.dart';
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

  /// 온디바이스 탐지 결과 목록 (최근 3개)
  final List<DetectionEvent> _obstacles = [];

  int _detectedCount = 0;

  /// 온디바이스 서비스 준비 여부 (DetectionActiveView wsConnected 파라미터 재활용)
  bool _isReady = false;

  StreamSubscription<DetectionEvent>? _detectionSub;

  // ─── 탐지 시작 ───────────────────────────────────────────────────────────

  Future<void> _startDetection() async {
    SoundEffectService().play(SoundEffect.actionStart);
    VibrationService().vibrate(VibrationEffect.actionStart);

    await OnDeviceDetectionService().start(CameraMode.detection);
    _detectionSub =
        OnDeviceDetectionService().eventStream.listen(_onDetectionEvent);

    await SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    setState(() {
      _isDetecting = true;
      _isReady = true;
      _obstacles.clear();
      _detectedCount = 0;
    });

    widget.onDetectingChanged?.call(true);
  }

  // ─── 탐지 중지 ───────────────────────────────────────────────────────────

  Future<void> _stopDetection() async {
    SoundEffectService().play(SoundEffect.actionStop);
    VibrationService().vibrate(VibrationEffect.actionStop);

    await _detectionSub?.cancel();
    _detectionSub = null;

    await TtsService().stop();
    await OnDeviceDetectionService().stop();

    await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);

    setState(() {
      _isDetecting = false;
      _isReady = false;
    });

    widget.onDetectingChanged?.call(false);
  }

  // ─── 탐지 이벤트 처리 ────────────────────────────────────────────────────

  void _onDetectionEvent(DetectionEvent event) {
    setState(() {
      _detectedCount++;
      _obstacles.insert(0, event);
      if (_obstacles.length > 3) _obstacles.removeLast();
    });

    VibrationService().vibrate(switch (event.alertLevel) {
      'high' => VibrationEffect.obstacleLevel3,
      'medium' => VibrationEffect.obstacleLevel2,
      _ => VibrationEffect.obstacleLevel1,
    });

    if (event.guideText.isNotEmpty) {
      TtsService().speak(
        event.guideText,
        interrupt: event.alertLevel == 'high',
      );
    }
  }

  @override
  void dispose() {
    _detectionSub?.cancel();
    super.dispose();
  }

  // ─── 빌드 ────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_isDetecting,
      child: Scaffold(
        appBar: _isDetecting ? null : const CustomTitleBar(title: '실외 장애물 탐지'),
        body: SafeArea(
          child: _isDetecting
              ? DetectionActiveView(
                  onStop: _stopDetection,
                  detectedCount: _detectedCount,
                  obstacles: _obstacles,
                  wsConnected: _isReady,
                )
              : DetectionIdleView(onStart: _startDetection),
        ),
      ),
    );
  }
}