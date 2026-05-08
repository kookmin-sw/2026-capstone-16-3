import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 탐지 시작/중지 원형 버튼
///
/// DetectionButton(isDetecting: false, onTap: () {})  // 주황 - 탐지 시작 (펄스 애니메이션)
/// DetectionButton(isDetecting: true, onTap: () {})   // 빨강 - 탐지 중지
class DetectionButton extends StatefulWidget {
  final bool isDetecting;
  final VoidCallback onTap;
  final double size;

  const DetectionButton({
    super.key,
    required this.isDetecting,
    required this.onTap,
    this.size = 180,
  });

  @override
  State<DetectionButton> createState() => _DetectionButtonState();
}

class _DetectionButtonState extends State<DetectionButton>
    with TickerProviderStateMixin {
  late final AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2000),
    )..repeat();
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final color = widget.isDetecting
        ? ColorCollection.red
        : ColorCollection.main;
    final icon = widget.isDetecting ? Icons.stop : Icons.play_arrow;
    final label = widget.isDetecting ? '탐지 중지' : '탐지 시작';

    final innerCircleSize = widget.size * 0.34;
    final iconSize = widget.size * 0.20;
    final spacing = widget.size * 0.06;

    // 유리형 글로우 배경 = 링 기준 크기
    final glassSize = widget.size * 1.33;
    // Transform.scale 최대값 1.2x → SizedBox는 그만큼 확보해야 클리핑 방지
    final totalSize = widget.isDetecting ? widget.size : glassSize * 1.2;

    return Semantics(
      button: true,
      label: label,
      excludeSemantics: true,
      child: GestureDetector(
        onTap: widget.onTap,
        child: SizedBox(
          width: totalSize,
          height: totalSize,
          child: Stack(
            alignment: Alignment.center,
            children: [
              if (!widget.isDetecting) ...[
                // 글로우 배경 (glass morphism)
                Container(
                  width: glassSize,
                  height: glassSize,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: ColorCollection.main.withValues(alpha: 0.06),
                  ),
                ),
                // 펄스 링 1
                _PulseRing(
                  controller: _pulseController,
                  delayFraction: 0.0,
                  ringBaseSize: glassSize,
                  color: ColorCollection.main,
                ),
                // 펄스 링 2 (0.4s / 2.0s = 0.2 fraction 딜레이)
                _PulseRing(
                  controller: _pulseController,
                  delayFraction: 0.2,
                  ringBaseSize: glassSize,
                  color: ColorCollection.main,
                ),
              ],
              // 메인 버튼
              Container(
                width: widget.size,
                height: widget.size,
                decoration: BoxDecoration(
                  color: color,
                  shape: BoxShape.circle,
                  boxShadow: !widget.isDetecting
                      ? [
                          BoxShadow(
                            color: ColorCollection.main.withValues(alpha: 0.4),
                            blurRadius: 15,
                            spreadRadius: 2,
                          ),
                        ]
                      : null,
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Container(
                      width: innerCircleSize,
                      height: innerCircleSize,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: ColorCollection.point,
                          width: 2.5,
                        ),
                      ),
                      child: Icon(
                        icon,
                        color: ColorCollection.point,
                        size: iconSize,
                      ),
                    ),
                    SizedBox(height: spacing),
                    Text(
                      label,
                      style:
                          (widget.isDetecting
                                  ? AppTextStyles.headline.copyWith(
                                      fontSize: 23,
                                    )
                                  : AppTextStyles.headline)
                              .copyWith(color: ColorCollection.point),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 단일 펄스 링
///
/// Transform.scale + Opacity 위젯 조합 사용:
/// - border 두께가 일정하게 유지되며 원이 퍼짐 (컨테이너 크기 변경 방식 대비 더 자연스러움)
/// - scale: 0.85 → 1.35 (easeOut)
/// - opacity: 0.4 → 0.0 (easeOut — 퍼질수록 서서히 연해짐)
class _PulseRing extends StatelessWidget {
  final AnimationController controller;
  final double delayFraction;
  final double ringBaseSize;
  final Color color;

  const _PulseRing({
    required this.controller,
    required this.delayFraction,
    required this.ringBaseSize,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        final raw = (controller.value + delayFraction) % 1.0;
        final t = Curves.easeOut.transform(raw);
        final scale = 0.85 + t * (1.2 - 0.85);
        final opacity = (0.4 * (1.0 - t)).clamp(0.0, 1.0);

        return Transform.scale(
          scale: scale,
          child: Opacity(opacity: opacity, child: child),
        );
      },
      child: Container(
        width: ringBaseSize,
        height: ringBaseSize,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: color, width: 3.5),
        ),
      ),
    );
  }
}
