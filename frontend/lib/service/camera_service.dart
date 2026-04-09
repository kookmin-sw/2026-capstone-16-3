import 'dart:async';
import 'dart:convert';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:safepath/service/token_storage.dart';

/// 카메라 사용 모드
enum CameraMode { detection, navigation }

/// 디버그용 캡처 이벤트 — DetectionActiveView에서 상태 표시에 사용
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
///   - 후면 카메라 초기화 (프리뷰 없이 프레임만 수집)
///   - 1초마다 사진 캡처 → POST /api/guide/image 전송
///   - Detection / Navigation 모드 공용 싱글톤
///
/// 호출 위치:
///   - DetectionScreen: 탐지 시작 → start(detection) / 중지 → stop()
///   - NavigationScreen: 길찾기 시작 → start(navigation) / 종료 → stop()
/// =======================================================
class CameraService {
  static final CameraService _instance = CameraService._internal();
  factory CameraService() => _instance;
  CameraService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  /// true → 카메라 대신 테스트 이미지(assets/images/test_detection.jpg)를 주기 전송
  static const bool useTestImage = false;

  /// 캡처 전송 주기
  static const Duration _captureInterval = Duration(milliseconds: 500);

  CameraController? _controller;
  Timer? _captureTimer;
  CameraMode? _currentMode;
  bool _isRunning = false;

  /// 이전 캡처가 아직 진행 중일 때 중복 실행 방지
  bool _isSending = false;

  /// 캡처 결과를 UI에 실시간으로 알려주는 스트림
  /// DetectionActiveView에서 listen해서 탐지 상태 표시에 활용
  final _captureEventController = StreamController<CaptureEvent>.broadcast();
  Stream<CaptureEvent> get captureEventStream => _captureEventController.stream;

  bool get isRunning => _isRunning;
  CameraMode? get currentMode => _currentMode;

  /// 디버그 전용 — CameraPreview 위젯에 넘길 때 사용
  /// useTestImage 조건 안에서만 접근할 것
  CameraController? get debugController => _controller;

  // ─── 시작 ─────────────────────────────────────────────────────────────────

  /// 카메라를 초기화하고 주기적 캡처를 시작한다.
  /// 이미 실행 중이면 무시한다.
  /// 디버그 모드에서는 카메라 초기화를 건너뛰고 테스트 이미지를 주기적으로 전송한다.
  Future<void> start(CameraMode mode) async {
    if (_isRunning) return;

    try {
      if (!useTestImage) {
        // 사용 가능한 카메라 목록에서 후면 카메라를 선택
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

        // initialize() 완료 전에 stop()이 호출된 경우 중단
        if (_controller == null) return;

        debugPrint('🟡 [Camera] 카메라 초기화 완료 / 모드: $mode');
      } else {
        debugPrint('🟣 [Camera][DBG] 디버그 모드: 카메라 생략, 테스트 이미지 주기 전송 시작');
      }

      _isRunning = true;
      _currentMode = mode;

      // 0.5초마다 _captureAndSend 실행
      _captureTimer = Timer.periodic(
        _captureInterval,
        (_) => _captureAndSend(),
      );
    } catch (e) {
      _isRunning = false;
      debugPrint('🔴 [Camera] 카메라 시작 실패: $e');
      rethrow;
    }
  }

  // ─── 정지 ─────────────────────────────────────────────────────────────────

  /// 캡처 타이머를 취소하고 카메라를 해제한다.
  Future<void> stop() async {
    if (!_isRunning) return;

    // 먼저 플래그를 내려서 진행 중인 캡처가 컨트롤러에 접근하지 못하게 막음
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

  // ─── 캡처 & 전송 ──────────────────────────────────────────────────────────

  static const _debugAssetPath = 'assets/images/test_detection.jpg';

  /// 사진을 찍고(또는 디버그 테스트 이미지를 로드하고) 서버로 전송한다.
  /// 성공/실패 결과를 captureEventStream으로 내보낸다.
  Future<void> _captureAndSend() async {
    if (!_isRunning || _isSending) return;

    _isSending = true;
    try {
      final Uint8List bytes;

      if (useTestImage) {
        // 디버그 모드: 카메라 대신 번들 테스트 이미지를 사용
        final byteData = await rootBundle.load(_debugAssetPath);
        bytes = byteData.buffer.asUint8List();
        debugPrint(
          '🟣 [Camera][DBG] 테스트 이미지 로드 완료 (${bytes.length} bytes) → 전송 시작',
        );
      } else {
        if (_controller == null || !_controller!.value.isInitialized) return;
        final file = await _controller!.takePicture();

        // takePicture 완료 후 이미 stop()이 호출됐으면 중단
        if (!_isRunning) return;

        bytes = await file.readAsBytes();
        debugPrint('🟡 [Camera] 캡처 완료 (${bytes.length} bytes) → 전송 시작');
      }

      final success = await _sendFrame(bytes);

      if (!_captureEventController.isClosed) {
        _captureEventController.add(
          CaptureEvent(
            time: DateTime.now(),
            success: success,
            bytes: bytes.length,
          ),
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

  /// POST /api/guide/image 또는 navigation 엔드포인트로 이미지를 전송한다.
  ///
  /// Request: multipart/form-data
  ///   - image      : JPEG 바이너리
  ///   - captured_at: 캡처 시각 (UTC ISO 8601)
  ///
  /// Response: { "success": bool, "data": {}, "error": { ... } }
  /// success=true이고 HTTP 200일 때만 전송 성공으로 처리한다.
  Future<bool> _sendFrame(Uint8List bytes) async {
    // detection 모드: /api/guide/image / navigation 모드: 별도 엔드포인트 (미구현)
    final endpoint = _currentMode == CameraMode.detection
        ? '$_baseUrl/api/guide/image'
        : '$_baseUrl/api/navigation/frame'; // TODO: navigation 엔드포인트 확정 시 수정

    final capturedAt = DateTime.now().toUtc().toIso8601String();
    final accessToken = await TokenStorage().accessToken;

    debugPrint('🟡 [Camera] BE 요청: POST $endpoint / captured_at: $capturedAt');

    final request = http.MultipartRequest('POST', Uri.parse(endpoint))
      ..headers['Authorization'] = 'Bearer ${accessToken ?? ''}'
      ..files.add(
        http.MultipartFile.fromBytes(
          'image',
          bytes,
          filename: 'image_${DateTime.now().millisecondsSinceEpoch}.jpg',
          contentType: MediaType('image', 'jpeg'),
        ),
      )
      ..fields['captured_at'] = capturedAt;

    final streamedResponse = await request.send();
    final responseBody = await streamedResponse.stream.bytesToString();

    debugPrint(
      '🟡 [Camera] BE 응답: ${streamedResponse.statusCode} / $responseBody',
    );

    if (streamedResponse.statusCode == 200) {
      final json = jsonDecode(responseBody) as Map<String, dynamic>;
      final success = json['success'] as bool? ?? false;
      if (!success) {
        // 서버가 200을 줬지만 success=false인 경우 (에러 내용 출력)
        final error = json['error'] as Map<String, dynamic>?;
        debugPrint(
          '🔴 [Camera] 서버 오류: ${error?['code']} / ${error?['message']}',
        );
      }
      return success;
    }

    return false;
  }
}
