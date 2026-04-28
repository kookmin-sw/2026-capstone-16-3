import 'dart:async';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// 카메라 사용 모드
enum CameraMode { detection, navigation }

/// 디버그용 캡처 이벤트 — CameraDebugOverlay에서 상태 표시에 사용
class CaptureEvent {
  final DateTime time;
  final bool success;
  final int bytes;

  const CaptureEvent({
    required this.time,
    required this.success,
    required this.bytes,
  });
}

/// =======================================================
/// CameraService
///
/// 역할:
///   - 후면 카메라 초기화
///   - 500ms마다 프레임 캡처 → frameStream으로 방출
///   - OnDeviceDetectionService가 구독하여 TFLite 추론에 활용
///   - Detection / Navigation 모드 공용 싱글톤
/// =======================================================
class CameraService {
  static final CameraService _instance = CameraService._internal();
  factory CameraService() => _instance;
  CameraService._internal();

  /// true → 실제 카메라 대신 테스트 이미지(assets/images/test_detection.jpg) 사용
  static const bool useTestImage = false;

  static const Duration _captureInterval = Duration(milliseconds: 500);

  CameraController? _controller;
  Timer? _captureTimer;
  CameraMode? _currentMode;
  bool _isRunning = false;
  bool _isSending = false;

  /// 캡처된 프레임 바이트 스트림 — OnDeviceDetectionService가 구독
  final _frameController = StreamController<Uint8List>.broadcast();
  Stream<Uint8List> get frameStream => _frameController.stream;

  /// 디버그 오버레이용 캡처 이벤트 스트림
  final _captureEventController = StreamController<CaptureEvent>.broadcast();
  Stream<CaptureEvent> get captureEventStream => _captureEventController.stream;

  bool get isRunning => _isRunning;
  CameraMode? get currentMode => _currentMode;

  /// 디버그 전용 — CameraPreview 위젯에 넘길 때 사용
  CameraController? get debugController => _controller;

  // ─── 시작 ─────────────────────────────────────────────────────────────────

  Future<void> start(CameraMode mode) async {
    if (_isRunning) return;

    try {
      if (!useTestImage) {
        final cameras = await availableCameras();
        final rear = cameras.firstWhere(
          (c) => c.lensDirection == CameraLensDirection.back,
          orElse: () => cameras.first,
        );

        _controller = CameraController(
          rear,
          ResolutionPreset.medium,
          enableAudio: false,
          imageFormatGroup: ImageFormatGroup.jpeg,
        );

        await _controller!.initialize();

        if (_controller == null) return;

        debugPrint('🟡 [Camera] 카메라 초기화 완료 / 모드: $mode');
      } else {
        debugPrint('🟣 [Camera][DBG] 테스트 이미지 모드: 주기 캡처 시작');
      }

      _isRunning = true;
      _currentMode = mode;

      _captureTimer = Timer.periodic(_captureInterval, (_) => _capture());
    } catch (e) {
      _isRunning = false;
      debugPrint('🔴 [Camera] 카메라 시작 실패: $e');
      rethrow;
    }
  }

  // ─── 정지 ─────────────────────────────────────────────────────────────────

  Future<void> stop() async {
    if (!_isRunning) return;

    _isRunning = false;
    _currentMode = null;

    _captureTimer?.cancel();
    _captureTimer = null;

    if (!useTestImage) {
      final controller = _controller;
      _controller = null;
      await controller?.dispose();
    }

    debugPrint('🟡 [Camera] 카메라 정지 완료');
  }

  // ─── 캡처 & 방출 ──────────────────────────────────────────────────────────

  static const _debugAssetPath = 'assets/images/test_detection.jpg';

  Future<void> _capture() async {
    if (!_isRunning || _isSending) return;
    _isSending = true;

    try {
      final Uint8List bytes;

      if (useTestImage) {
        final byteData = await rootBundle.load(_debugAssetPath);
        bytes = byteData.buffer.asUint8List();
        debugPrint('🟣 [Camera][DBG] 테스트 이미지 로드 (${bytes.length} bytes)');
      } else {
        if (_controller == null || !_controller!.value.isInitialized) return;
        final file = await _controller!.takePicture();
        if (!_isRunning) return;
        bytes = await file.readAsBytes();
        debugPrint('🟡 [Camera] 캡처 완료 (${bytes.length} bytes)');
      }

      if (!_frameController.isClosed) {
        _frameController.add(bytes);
      }

      if (!_captureEventController.isClosed) {
        _captureEventController.add(
          CaptureEvent(time: DateTime.now(), success: true, bytes: bytes.length),
        );
      }
    } catch (e) {
      debugPrint('🔴 [Camera] 캡처 실패: $e');
      if (!_captureEventController.isClosed) {
        _captureEventController.add(
          CaptureEvent(time: DateTime.now(), success: false, bytes: 0),
        );
      }
    } finally {
      _isSending = false;
    }
  }
}