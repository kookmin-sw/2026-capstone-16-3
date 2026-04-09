import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 탐지 시작/중지 원형 버튼
///
/// 사용 예시:
/// DetectionButton(isDetecting: false, onTap: () {})  // 주황 - 탐지 시작
/// DetectionButton(isDetecting: true, onTap: () {})   // 빨강 - 탐지 중지
class DetectionButton extends StatelessWidget {
  /// true: 탐지 중 (빨강/중지), false: 대기 중 (주황/시작)
  final bool isDetecting;
  final VoidCallback onTap;

  /// 버튼 지름 (기본값: 180)
  final double size;

  const DetectionButton({
    super.key,
    required this.isDetecting,
    required this.onTap,
    this.size = 180,
  });

  @override
  Widget build(BuildContext context) {
    final color = isDetecting ? ColorCollection.red : ColorCollection.main;
    final icon = isDetecting ? Icons.stop : Icons.play_arrow;
    final label = isDetecting ? '탐지 중지' : '탐지 시작';

    // 내부 요소들을 버튼 크기에 비례하여 스케일
    final innerCircleSize = size * 0.34;
    final iconSize = size * 0.20;
    final spacing = size * 0.06;

    return Semantics(
      button: true,
      label: label,
      excludeSemantics: true,
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // 내부 원형 테두리 + 아이콘
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
                child: Icon(icon, color: ColorCollection.point, size: iconSize),
              ),
              SizedBox(height: spacing),
              Text(
                label,
                style: (isDetecting
                        ? AppTextStyles.headline.copyWith(fontSize: 23)
                        : AppTextStyles.headline)
                    .copyWith(color: ColorCollection.point),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
