import 'dart:math';

import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/camera_debug_overlay.dart';
import 'package:safepath/features/detection/detection_button_widget.dart';
import 'package:safepath/features/detection/obstacle_card_widget.dart';
import 'package:safepath/model/detection_event.dart';

/// 탐지 진행 화면 (탐지 중)
class DetectionActiveView extends StatelessWidget {
  final VoidCallback onStop;

  /// 탐지된 물체 총 수 (WS 이벤트 수신 횟수)
  final int detectedCount;

  /// 최신 탐지 이벤트 목록 (최근 순, DetectionScreen에서 관리)
  final List<DetectionEvent> obstacles;

  /// WS STOMP 연결 완료 여부
  final bool wsConnected;

  const DetectionActiveView({
    super.key,
    required this.onStop,
    required this.detectedCount,
    required this.obstacles,
    required this.wsConnected,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Row(
            children: [
              // ── 좌측: 버튼 + 상태 + 탐지 수 ──────────────────────────────
              SizedBox(
                width: 220,
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    DetectionButton(
                      isDetecting: true,
                      onTap: onStop,
                      size: 180,
                    ),
                    const SizedBox(height: 16),

                    // 실시간 감지 활성화
                    Semantics(
                      label: '실시간 감지 활성화 중',
                      excludeSemantics: true,
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 10,
                            height: 10,
                            decoration: const BoxDecoration(
                              color: ColorCollection.green,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            '실시간 감지 활성화',
                            style: AppTextStyles.labelBold.copyWith(
                              color: ColorCollection.point,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 탐지된 물체 수 박스
                    Semantics(
                      label: '탐지된 물체 $detectedCount개',
                      excludeSemantics: true,
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        decoration: BoxDecoration(
                          color: ColorCollection.point.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(
                            color: ColorCollection.main,
                            width: 3,
                          ),
                        ),
                        child: Column(
                          children: [
                            Text(
                              '$detectedCount',
                              style: AppTextStyles.title1.copyWith(
                                color: ColorCollection.main,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '탐지된 물체',
                              style: AppTextStyles.labelRegular.copyWith(
                                color: ColorCollection.point,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 20),

              // ── 우측: 장애물 목록 ─────────────────────────────────────────
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '감지된 장애물',
                      style: AppTextStyles.title2.copyWith(
                        color: ColorCollection.point,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Expanded(
                      child: !wsConnected
                          ? const _ConnectingPlaceholder()
                          : obstacles.isEmpty
                          ? const _NoObstaclePlaceholder()
                          : ListView.separated(
                              itemCount: obstacles.length,
                              separatorBuilder: (_, __) =>
                                  const SizedBox(height: 10),
                              itemBuilder: (_, index) {
                                final e = obstacles[index];
                                return ObstacleCard(
                                  name: e.objectName,
                                  distance: e.distanceLabel,
                                  position: e.positionLabel,
                                  vibration: e.vibrationLabel,
                                  proximity: e.proximity,
                                  guideText: e.guideText,
                                );
                              },
                            ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const CameraDebugOverlay(),
      ],
    );
  }
}

// ─── 연결 중 플레이스홀더 (WS 미연결 상태) ────────────────────────────────────

class _ConnectingPlaceholder extends StatelessWidget {
  const _ConnectingPlaceholder();

  @override
  Widget build(BuildContext context) => _LoadingIndicator();
}

// ─── 장애물 없음 플레이스홀더 (WS 연결됨, 탐지 없음) ──────────────────────────

class _NoObstaclePlaceholder extends StatelessWidget {
  const _NoObstaclePlaceholder();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.check_circle_outline,
            color: ColorCollection.main,
            size: 36,
          ),
          const SizedBox(height: 12),
          Text(
            '주변에 장애물이 없습니다.',
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
            ),
          ),
        ],
      ),
    );
  }
}

/// 탐지 중 로딩 표시
class _LoadingIndicator extends StatefulWidget {
  @override
  State<_LoadingIndicator> createState() => _LoadingIndicatorState();
}

class _LoadingIndicatorState extends State<_LoadingIndicator>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '주변을 탐지하고 있습니다.',
      excludeSemantics: true,
      child: Center(
        child: Column(
          children: [
            const SizedBox(height: 16),
            AnimatedBuilder(
              animation: _controller,
              builder: (context, _) => CustomPaint(
                size: const Size(40, 40),
                painter: _LoadingPainter(progress: _controller.value),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              '주변을 탐지하고 있습니다.',
              style: AppTextStyles.labelRegular.copyWith(
                color: ColorCollection.point,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

class _LoadingPainter extends CustomPainter {
  final double progress;

  _LoadingPainter({required this.progress});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    const strokeWidth = 5.0;
    final radius = (size.width - strokeWidth) / 2;

    const gapAngle = 60 * pi / 180;
    const orangeSweep = 50 * pi / 180;

    final orangeStart = 2 * pi * progress - pi / 2;

    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      orangeStart + orangeSweep + gapAngle / 2,
      2 * pi - orangeSweep - gapAngle,
      false,
      Paint()
        ..color = ColorCollection.point
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round,
    );

    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      orangeStart,
      orangeSweep,
      false,
      Paint()
        ..color = ColorCollection.main
        ..style = PaintingStyle.stroke
        ..strokeWidth = strokeWidth
        ..strokeCap = StrokeCap.round,
    );
  }

  @override
  bool shouldRepaint(_LoadingPainter old) => old.progress != progress;
}
