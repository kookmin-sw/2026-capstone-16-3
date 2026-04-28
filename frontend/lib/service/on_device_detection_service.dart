import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/surface_tflite_runner.dart';

/// 온디바이스 장애물 탐지 서비스
///
/// CameraService.frameStream을 구독하여 SurfaceTfliteRunner로 추론하고
/// DetectionEvent를 eventStream으로 방출한다.
///
/// output0 형식: [1, 300, 38]
///   det[0..3] = x1,y1,x2,y2 (픽셀 좌표, 0~inputSize)
///   det[4]    = confidence
///   det[5]    = class_id (float → int)
///   det[6..37]= mask coefficients (무시)
class OnDeviceDetectionService {
  static final OnDeviceDetectionService _instance =
      OnDeviceDetectionService._internal();
  factory OnDeviceDetectionService() => _instance;
  OnDeviceDetectionService._internal();

  final _runner = SurfaceTfliteRunner(inputSize: 640);
  final _controller = StreamController<DetectionEvent>.broadcast();
  StreamSubscription<Uint8List>? _frameSub;

  bool _modelLoaded = false;
  bool _isRunning = false;
  bool _isProcessing = false;

  Stream<DetectionEvent> get eventStream => _controller.stream;
  bool get isRunning => _isRunning;

  // ─── 설정값 ───────────────────────────────────────────────────────────────

  static const double _confThreshold = 0.3;
  static const double _inputSize = 640.0;

  // 클래스 인덱스 → 영문 클래스명
  static const List<String> _classNames = [
    'wheelchair',              // 0
    'truck',                   // 1
    'tree_trunk',              // 2
    'traffic_sign',            // 3
    'traffic_light',           // 4
    'traffic_light_controller',// 5
    'table',                   // 6
    'stroller',                // 7
    'stop',                    // 8
    'scooter',                 // 9
    'potted_plant',            // 10
    'power_controller',        // 11
    'pole',                    // 12
    'person',                  // 13
    'parking_meter',           // 14
    'movable_signage',         // 15
    'motorcycle',              // 16
    'kiosk',                   // 17
    'fire_hydrant',            // 18
    'dog',                     // 19
    'chair',                   // 20
    'cat',                     // 21
    'carrier',                 // 22
    'car',                     // 23
    'bus',                     // 24
    'bollard',                 // 25
    'bicycle',                 // 26
    'bench',                   // 27
    'barricade',               // 28
  ];

  // 클래스 기반 위험도 기본값 (4=차량류, 3=중간, 2=낮음)
  static const Map<int, int> _classBase = {
    1: 4,  // truck
    9: 4,  // scooter
    16: 4, // motorcycle
    23: 4, // car
    24: 4, // bus
    26: 4, // bicycle
    0: 3,  // wheelchair
    6: 3,  // table
    7: 3,  // stroller
    12: 3, // pole
    13: 3, // person
    18: 3, // fire_hydrant
    19: 3, // dog
    20: 3, // chair
    21: 3, // cat
    22: 3, // carrier
    25: 3, // bollard
    27: 3, // bench
    28: 3, // barricade
    // 나머지 (2: tree_trunk, 3: traffic_sign, 4: traffic_light, ...) → 기본값 2
  };

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
      final event = _parseResult(result);
      if (event != null && !_controller.isClosed) {
        _controller.add(event);
      }
    } catch (e) {
      debugPrint('🔴 [OnDevice] 추론 실패: $e');
    } finally {
      _isProcessing = false;
    }
  }

  // ─── 추론 결과 파싱 ───────────────────────────────────────────────────────

  DetectionEvent? _parseResult(Map<String, dynamic> result) {
    final rawOutput0 = result['output0'] as List;
    final detections = rawOutput0[0] as List; // [300, 38]

    DetectionEvent? best;
    int bestPriority = 2; // 2 이하는 무시

    for (int i = 0; i < detections.length; i++) {
      final det = detections[i] as List;

      final conf = (det[4] as num).toDouble();
      if (conf < _confThreshold) continue;

      final classId = (det[5] as num).toInt();
      if (classId < 0 || classId >= _classNames.length) continue;

      // 픽셀 좌표 → 정규화 (0~1)
      final x1 = (det[0] as num).toDouble() / _inputSize;
      final y1 = (det[1] as num).toDouble() / _inputSize;
      final x2 = (det[2] as num).toDouble() / _inputSize;
      final y2 = (det[3] as num).toDouble() / _inputSize;

      final cx = (x1 + x2) / 2.0;
      final w = (x2 - x1).abs();
      final h = (y2 - y1).abs();
      final area = w * h;

      final distance = _distanceFromArea(area);
      final priority = (_classBase[classId] ?? 2)
          + _distanceScore(distance)
          + _pathScore(cx);

      if (priority <= 2) continue;
      if (priority <= bestPriority) continue;

      bestPriority = priority;

      final alertLevel = _alertLevel(priority);
      final clockDir = _clockDirection(cx);
      final className = _classNames[classId];
      final objectName = DetectionEvent.koName(className);
      final particle = _trailingConsonant(objectName) ? '이' : '가';

      best = DetectionEvent(
        guideText: _guideText(clockDir, objectName, particle, alertLevel),
        primaryObjectClass: className,
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
    if (area > 0.10) return 'near';
    if (area > 0.02) return 'mid';
    return 'far';
  }

  int _distanceScore(String distance) => switch (distance) {
    'near' => 3,
    'mid' => 2,
    _ => 1,
  };

  // 화면 중앙 경로 점유 여부
  int _pathScore(double cx) {
    if (cx >= 0.40 && cx <= 0.60) return 2; // on_path
    if (cx >= 0.25 && cx <= 0.75) return 1; // narrows_path
    return 0;
  }

  String _alertLevel(int priority) {
    if (priority >= 7) return 'high';
    if (priority >= 5) return 'medium';
    return 'low';
  }

  // 화면 x 비율 → 시계 방향 (9시~3시)
  String _clockDirection(double cx) {
    if (cx < 1 / 7) return '9시';
    if (cx < 2 / 7) return '10시';
    if (cx < 3 / 7) return '11시';
    if (cx < 4 / 7) return '12시';
    if (cx < 5 / 7) return '1시';
    if (cx < 6 / 7) return '2시';
    return '3시';
  }

  String _guideText(
    String clockDir,
    String objectName,
    String particle,
    String alertLevel,
  ) {
    final base = '$clockDir 방향에 $objectName$particle 있습니다.';
    return switch (alertLevel) {
      'high' => '$base 즉시 멈추거나 피하세요.',
      'medium' => '$base 주의하세요.',
      _ => base,
    };
  }

  bool _trailingConsonant(String text) {
    if (text.isEmpty) return false;
    final code = text.codeUnitAt(text.length - 1);
    if (code < 0xAC00 || code > 0xD7A3) return false;
    return (code - 0xAC00) % 28 != 0;
  }
}