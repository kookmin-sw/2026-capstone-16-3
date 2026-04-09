import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/features/detection/detection_button_widget.dart';

/// 탐지 대기 화면 (탐지 시작 전)
class DetectionIdleView extends StatelessWidget {
  final VoidCallback onStart;

  const DetectionIdleView({super.key, required this.onStart});

  @override
  Widget build(BuildContext context) {
    return SizedBox.expand(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Spacer(),
            DetectionButton(isDetecting: false, onTap: onStart, size: 271),
            const SizedBox(height: 65),
            Semantics(
              label: '카메라를 가슴 앞에 착용해주세요. 실시간으로 위험 요소를 찾아 안내해 드립니다.',
              excludeSemantics: true,
              child: SizedBox(
                width: double.infinity,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Text(
                      '카메라를 가슴 앞에 착용해주세요.',
                      style: AppTextStyles.bodyRegular.copyWith(
                        color: ColorCollection.point,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),
                    Text(
                      '실시간으로 위험 요소를 찾아\n안내해 드립니다.',
                      style: AppTextStyles.bodyRegular.copyWith(
                        color: ColorCollection.point,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),
            const Spacer(),
          ],
        ),
      ),
    );
  }
}
