import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/model/detection_result.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/object_tflite_runner.dart';

/// 온디바이스 장애물 탐지 서비스
///
/// CameraService.frameStream을 구독하여 ObjectTfliteRunner로 추론하고
/// DetectionEvent를 eventStream으로 방출한다.
class OnDeviceDetectionService {
  static final OnDeviceDetectionService _instance =
      OnDeviceDetectionService._internal();
  factory OnDeviceDetectionService() => _instance;
  OnDeviceDetectionService._internal();

  // objectLabels 순서 = 모델 출력 class_id 순서와 일치해야 함
  // 모델 학습 YAML의 names 순서와 맞게 재확인 필요
  static const List<String> _objectLabels = [
    'wheelchair',               // 0
    'truck',                    // 1
    'tree_trunk',               // 2
    'traffic_sign',             // 3
    'traffic_light',            // 4
    'traffic_light_controller', // 5
    'table',                    // 6
    'stroller',                 // 7
    'stop',                     // 8
    'scooter',                  // 9
    'potted_plant',             // 10
    'power_controller',         // 11
    'pole',                     // 12
    'person',                   // 13
    'parking_meter',            // 14
    'movable_signage',          // 15
    'motorcycle',               // 16
    'kiosk',                    // 17
    'fire_hydrant',             // 18
    'dog',                      // 19
    'chair',                    // 20
    'cat',                      // 21
    'carrier',                  // 22
    'car',                      // 23
    'bus',                      // 24
    'bollard',                  // 25
    'bicycle',                  // 26
    'bench',                    // 27
    'barricade',                // 28
  ];

  // 클래스명 기반 위험도 (inference.py SEVERITY_MAP 기준)
  static const Map<String, int> _severity = {
    'truck': 3,
    'scooter': 3,
    'motorcycle': 3,
    'car': 3,
    'bus': 3,
    'wheelchair': 2,
    'tree_trunk': 2,
    'table': 2,
    'stop': 2,
    'pole': 2,
    'person': 2,
    'fire_hydrant': 2,
    'bollard': 2,
    'bicycle': 2,
    'bench': 2,
    'barricade': 2,
    'traffic_light': 1,
  };

  final _runner = ObjectTfliteRunner(
    inputSize: 640,
    labels: _objectLabels,
    scoreThreshold: 0.25,
  );
  final _controller = StreamController<DetectionEvent>.broadcast();
  StreamSubscription<Uint8List>? _frameSub;

  bool _modelLoaded = false;
  bool _isRunning = false;
  bool _isProcessing = false;

  Stream<DetectionEvent> get eventStream => _controller.stream;
  bool get isRunning => _isRunning;

  // ─── 시작 / 정지 ──────────────────────────────────────────────────────────

  Future<void> start(CameraMode mode) async {
    if (_isRunning) return;

    if (!_modelLoaded) {
      await _runner.load();
      _modelLoaded = true;
      debugPrint('🟢 [OnDevice] TFLite 모델 로드 완료');
      if (kDebugMode) debugPrint('🟢 [OnDevice] 모델 정보: ${_runner.getModelInfo()}');
    }

    await CameraService().start(mode);
    _frameSub = CameraService().frameStream.listen(_onFrame);
    _isRunning = true;
    debugPrint('🟢 [OnDevice] 온디바이스 탐지 시작 (mode: $mode)');
  }

  Future<void> stop() async {
    if (!_isRunning) return;
    _isRunning = false;
    await _frameSub?.cancel();
    _frameSub = null;
    await CameraService().stop();
    debugPrint('🟡 [OnDevice] 온디바이스 탐지 중지');
  }

  // ─── 프레임 처리 ──────────────────────────────────────────────────────────

  Future<void> _onFrame(Uint8List bytes) async {
    if (_isProcessing) return;
    _isProcessing = true;
    try {
      final result = await _runner.runOnImageBytes(bytes);
      debugPrint('🟣 [OnDevice] 추론 완료 (${result["elapsedMs"]}ms)');
      final detections = result['detections'] as List<DetectionResult>;
      final event = _selectBestEvent(detections);
      if (event != null && !_controller.isClosed) {
        _controller.add(event);
      }
    } catch (e) {
      debugPrint('🔴 [OnDevice] 추론 실패: $e');
    } finally {
      _isProcessing = false;
    }
  }

  // ─── 최우선 탐지 이벤트 선택 ──────────────────────────────────────────────

  DetectionEvent? _selectBestEvent(List<DetectionResult> detections) {
    DetectionEvent? best;
    int bestPriority = 2; // 2 이하는 무시

    for (final det in detections) {
      final cx = (det.x1 + det.x2) / 2.0;
      final w = (det.x2 - det.x1).abs();
      final h = (det.y2 - det.y1).abs();
      final area = w * h;

      final distance = _distanceFromArea(area);
      final hRegion = _hRegion(cx);

      final severity = _severity[det.label] ?? 1;
      final immediacy = _immediacy(distance);
      final isNearOrMid = distance == 'near' || distance == 'mid';
      final onPath = (hRegion == 'center' && isNearOrMid) ? 1 : 0;
      final narrowsPath = isNearOrMid ? 1 : 0;
      final priority = severity + immediacy + onPath + narrowsPath;

      if (priority <= 2) continue;
      if (priority <= bestPriority) continue;

      bestPriority = priority;

      final alertLevel = _alertLevel(priority);
      final clockDir = _clockDirection(cx);
      final objectName = DetectionEvent.koName(det.label);
      final particle = _trailingConsonant(objectName) ? '이' : '가';

      best = DetectionEvent(
        guideText: _guideText(clockDir, objectName, particle, alertLevel),
        primaryObjectClass: det.label,
        clockDirection: clockDir,
        distance: distance,
        alertLevel: alertLevel,
      );
    }

    if (best != null) {
      debugPrint(
        '🟢 [OnDevice] 탐지: ${best.primaryObjectClass} '
        '(${best.alertLevel}) ${best.clockDirection} 방향 / priority=$bestPriority',
      );
    }

    return best;
  }

  // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

  String _distanceFromArea(double area) {
    if (area >= 0.10) return 'near';
    if (area >= 0.03) return 'mid';
    return 'far';
  }

  String _hRegion(double cx) {
    if (cx < 0.33) return 'left';
    if (cx < 0.66) return 'center';
    return 'right';
  }

  int _immediacy(String distance) => switch (distance) {
    'near' => 3,
    'mid' => 2,
    _ => 1,
  };

  String _alertLevel(int priority) {
    if (priority >= 7) return 'high';
    if (priority >= 5) return 'medium';
    return 'low';
  }

  String _clockDirection(double cx) {
    if (cx < 0.2) return '10시';
    if (cx < 0.4) return '11시';
    if (cx < 0.6) return '12시';
    if (cx < 0.8) return '1시';
    return '2시';
  }

  String _guideText(
    String clockDir,
    String objectName,
    String particle,
    String alertLevel,
  ) {
    final base = '$clockDir 방향에 $objectName$particle 있습니다.';
    return switch (alertLevel) {
      'high' => '$base 즉시 이동하세요.',
      'medium' => '$base 피해 주세요.',
      _ => '$base 주의하며 이동하세요.',
    };
  }

  bool _trailingConsonant(String text) {
    if (text.isEmpty) return false;
    final code = text.codeUnitAt(text.length - 1);
    if (code < 0xAC00 || code > 0xD7A3) return false;
    return (code - 0xAC00) % 28 != 0;
  }
}
