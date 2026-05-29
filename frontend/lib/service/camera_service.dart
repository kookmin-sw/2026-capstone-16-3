import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;

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

  /// true → 카메라 대신 테스트 이미지(assets/images/test_detection(2).jpg)를 주기 전송
  static const bool useTestImage = false;

  /// 캡처 전송 주기
  static const Duration _captureInterval = Duration(milliseconds: 500);

  CameraController? _controller;
  Timer? _captureTimer;
  CameraMode? _currentMode;
  bool _isRunning = false;

  /// 이전 캡처가 아직 진행 중일 때 중복 실행 방지
  bool _isSending = false;

  /// startImageStream()으로 수신한 최신 프레임
  CameraImage? _latestFrame;

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

        // takePicture() 대신 startImageStream() 사용:
        // iOS에서 takePicture()는 셔터 사운드가 강제 발생하지만
        // startImageStream()은 비디오 스트림 방식이라 소리 없음
        await _controller!.startImageStream((CameraImage image) {
          _latestFrame = image;
        });

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
    _latestFrame = null;

    _captureTimer?.cancel();
    _captureTimer = null;

    if (!useTestImage) {
      final controller = _controller;
      _controller = null;
      if (controller != null) {
        if (controller.value.isStreamingImages) {
          await controller.stopImageStream();
        }
        await controller.dispose();
      }
    }

    debugPrint('🟡 [Camera] 카메라 정지 완료');
  }

  // ─── 캡처 & 전송 ──────────────────────────────────────────────────────────

  static const _debugAssetPath = 'assets/images/test_detection(2).jpg';

  /// 최신 스트림 프레임(또는 디버그 테스트 이미지)을 서버로 전송한다.
  /// 성공/실패 결과를 captureEventStream으로 내보낸다.
  Future<void> _captureAndSend() async {
    if (!_isRunning || _isSending) return;

    _isSending = true;
    try {
      final Uint8List bytes;
      final String imageType;

      if (useTestImage) {
        // 디버그 모드: 카메라 대신 번들 테스트 이미지를 사용
        final byteData = await rootBundle.load(_debugAssetPath);
        bytes = byteData.buffer.asUint8List();
        imageType = _debugAssetPath.endsWith('.png') ? 'png' : 'jpeg';
        debugPrint(
          '🟣 [Camera][DBG] 테스트 이미지 로드 완료 (${bytes.length} bytes, $imageType) → 전송 시작',
        );
      } else {
        if (_controller == null || !_controller!.value.isInitialized) return;

        final frame = _latestFrame;
        if (frame == null) return; // 아직 첫 프레임 미수신

        final result = await _cameraImageToBytes(frame);
        if (result == null) return;

        // 변환 완료 후 이미 stop()이 호출됐으면 중단
        if (!_isRunning) return;

        bytes = result.$1;
        imageType = result.$2;
        debugPrint('🟡 [Camera] 프레임 변환 완료 (${bytes.length} bytes, $imageType) → 전송 시작');
      }

      final success = await _sendFrame(bytes, imageType: imageType);

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

  // ─── 이미지 포맷 변환 ──────────────────────────────────────────────────────

  /// CameraImage → (bytes, imageType) 변환
  ///
  /// - jpeg     (Android 일부): planes[0].bytes가 바로 JPEG 바이너리
  /// - yuv420   (Android 다수): BT.601 정수 연산으로 RGBA 변환 후 PNG 인코딩
  /// - bgra8888 (iOS)         : dart:ui로 PNG 인코딩 후 반환
  Future<(Uint8List, String)?> _cameraImageToBytes(CameraImage image) async {
    if (image.format.group == ImageFormatGroup.jpeg) {
      return (image.planes[0].bytes, 'jpeg');
    }

    if (image.format.group == ImageFormatGroup.yuv420) {
      return _yuv420ToPng(image);
    }

    if (image.format.group == ImageFormatGroup.bgra8888) {
      return _rawPixelsToPng(
        image.planes[0].bytes,
        image.width,
        image.height,
        ui.PixelFormat.bgra8888,
      );
    }

    debugPrint('🔴 [Camera] 지원하지 않는 이미지 포맷: ${image.format.group}');
    return null;
  }

  /// YUV420(YUV_420_888) → RGBA → PNG
  ///
  /// Android에서 JPEG 스트리밍이 지원되지 않을 때 yuv420으로 fallback됨.
  /// BT.601 정수 연산(비트 시프트)으로 float 대비 성능을 높임.
  Future<(Uint8List, String)?> _yuv420ToPng(CameraImage image) async {
    final int width = image.width;
    final int height = image.height;

    final yPlane = image.planes[0];
    final uPlane = image.planes[1];
    final vPlane = image.planes[2];

    final yBytes = yPlane.bytes;
    final uBytes = uPlane.bytes;
    final vBytes = vPlane.bytes;

    final int yRowStride = yPlane.bytesPerRow;
    final int uvRowStride = uPlane.bytesPerRow;
    // pixelStride=1 → I420(planar), pixelStride=2 → NV12/NV21(semi-planar)
    final int uvPixelStride = uPlane.bytesPerPixel ?? 1;

    final rgba = Uint8List(width * height * 4);

    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        final yVal = yBytes[row * yRowStride + col] & 0xFF;
        final uvIdx = (row ~/ 2) * uvRowStride + (col ~/ 2) * uvPixelStride;
        final uVal = uBytes[uvIdx] & 0xFF;
        final vVal = vBytes[uvIdx] & 0xFF;

        // BT.601 YCbCr → RGB (정수 연산)
        final c = yVal - 16;
        final d = uVal - 128;
        final e = vVal - 128;

        final pIdx = (row * width + col) * 4;
        rgba[pIdx]     = ((298 * c + 409 * e + 128) >> 8).clamp(0, 255);
        rgba[pIdx + 1] = ((298 * c - 100 * d - 208 * e + 128) >> 8).clamp(0, 255);
        rgba[pIdx + 2] = ((298 * c + 516 * d + 128) >> 8).clamp(0, 255);
        rgba[pIdx + 3] = 255;
      }
    }

    return _rawPixelsToPng(rgba, width, height, ui.PixelFormat.rgba8888);
  }

  /// 원시 픽셀 버퍼 → PNG (dart:ui 경유)
  Future<(Uint8List, String)?> _rawPixelsToPng(
    Uint8List pixels,
    int width,
    int height,
    ui.PixelFormat format,
  ) async {
    final completer = Completer<ui.Image>();
    ui.decodeImageFromPixels(pixels, width, height, format, completer.complete);
    final uiImage = await completer.future;
    final byteData = await uiImage.toByteData(format: ui.ImageByteFormat.png);
    uiImage.dispose();
    if (byteData == null) return null;
    return (byteData.buffer.asUint8List(), 'png');
  }

  // ─── 프레임 전송 ──────────────────────────────────────────────────────────

  /// POST /api/guide/image 엔드포인트로 이미지를 전송한다.
  ///
  /// Request: multipart/form-data
  ///   - image      : JPEG 또는 PNG 바이너리
  ///   - captured_at: 캡처 시각 (UTC ISO 8601)
  ///
  /// Response: { "success": bool, "data": {}, "error": { ... } }
  /// success=true이고 HTTP 200일 때만 전송 성공으로 처리한다.
  Future<bool> _sendFrame(Uint8List bytes, {String imageType = 'jpeg'}) async {
    final endpoint = '$_baseUrl/api/guide/image';

    final capturedAt = DateTime.now().toUtc().toIso8601String();
    final accessToken = await TokenStorage().accessToken;
    final ext = imageType == 'png' ? 'png' : 'jpg';

    debugPrint('🟡 [Camera] BE 요청: POST $endpoint / captured_at: $capturedAt');

    final request = http.MultipartRequest('POST', Uri.parse(endpoint))
      ..headers['Authorization'] = 'Bearer ${accessToken ?? ''}'
      ..files.add(
        http.MultipartFile.fromBytes(
          'image',
          bytes,
          filename: 'image_${DateTime.now().millisecondsSinceEpoch}.$ext',
          contentType: MediaType('image', imageType),
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
