import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/detection/detection_active_view.dart';
import 'package:safepath/features/detection/detection_idle_view.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/detection_ws_service.dart';

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
    await _wsSub?.cancel();
    _wsSub = null;

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
      _detectedCount++;
      _obstacles.insert(0, event); // 최신 이벤트를 목록 맨 앞에
      if (_obstacles.length > 3) _obstacles.removeLast(); // 최근 3개 유지
    });
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
