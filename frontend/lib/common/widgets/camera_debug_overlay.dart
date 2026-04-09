import 'dart:async';

import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'package:safepath/service/camera_service.dart';

/// 디버그 전용 카메라 PiP 오버레이
///
/// kDebugMode일 때만 렌더링됨 — release 빌드에서는 SizedBox.shrink()로 대체
///
/// 사용 예시:
///   Stack(
///     children: [
///       ... 본 화면 위젯 ...
///       const CameraDebugOverlay(),
///     ],
///   )
class CameraDebugOverlay extends StatefulWidget {
  const CameraDebugOverlay({super.key});

  @override
  State<CameraDebugOverlay> createState() => _CameraDebugOverlayState();
}

class _CameraDebugOverlayState extends State<CameraDebugOverlay> {
  CaptureEvent? _lastEvent;
  StreamSubscription<CaptureEvent>? _sub;
  bool _visible = true;

  @override
  void initState() {
    super.initState();
    _sub = CameraService().captureEventStream.listen((event) {
      if (mounted) setState(() => _lastEvent = event);
    });
  }

  @override
  void dispose() {
    _sub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Release 빌드에서는 완전히 제거
    if (!kDebugMode) return const SizedBox.shrink();

    if (!_visible) {
      return Positioned(
        right: 8,
        top: 8,
        child: GestureDetector(
          onTap: () => setState(() => _visible = true),
          child: Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.6),
              borderRadius: BorderRadius.circular(6),
            ),
            child: const Icon(Icons.videocam, color: Colors.white, size: 18),
          ),
        ),
      );
    }

    final controller = CameraService().debugController;
    final isReady = controller != null && controller.value.isInitialized;

    return Positioned(
      right: 8,
      top: 8,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          // 닫기 버튼
          GestureDetector(
            onTap: () => setState(() => _visible = false),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.7),
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(8),
                  topRight: Radius.circular(8),
                ),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'CAM DEBUG',
                    style: TextStyle(
                      color: Colors.yellow,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  SizedBox(width: 6),
                  Icon(Icons.close, color: Colors.white, size: 14),
                ],
              ),
            ),
          ),

          // 카메라 프리뷰
          Container(
            width: 140,
            height: 100,
            decoration: BoxDecoration(
              border: Border.all(
                color: isReady ? Colors.green : Colors.red,
                width: 2,
              ),
            ),
            child: isReady
                ? CameraPreview(controller)
                : const ColoredBox(
                    color: Colors.black,
                    child: Center(
                      child: Text(
                        'NO CAMERA',
                        style: TextStyle(color: Colors.red, fontSize: 11),
                      ),
                    ),
                  ),
          ),

          // 캡처 상태
          Container(
            width: 140,
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            color: Colors.black.withValues(alpha: 0.75),
            child: _lastEvent == null
                ? const Text(
                    '대기 중...',
                    style: TextStyle(color: Colors.grey, fontSize: 10),
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            _lastEvent!.success
                                ? Icons.check_circle
                                : Icons.error,
                            color: _lastEvent!.success
                                ? Colors.green
                                : Colors.red,
                            size: 12,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            _lastEvent!.success ? '전송 성공' : '전송 실패',
                            style: TextStyle(
                              color: _lastEvent!.success
                                  ? Colors.green
                                  : Colors.red,
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                      Text(
                        '크기: ${(_lastEvent!.bytes / 1024).toStringAsFixed(1)} KB',
                        style: const TextStyle(
                          color: Colors.white70,
                          fontSize: 10,
                        ),
                      ),
                      Text(
                        '시각: ${_formatTime(_lastEvent!.time)}',
                        style: const TextStyle(
                          color: Colors.white70,
                          fontSize: 10,
                        ),
                      ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime t) =>
      '${t.hour.toString().padLeft(2, '0')}:'
      '${t.minute.toString().padLeft(2, '0')}:'
      '${t.second.toString().padLeft(2, '0')}';
}
