import 'package:dotted_border/dotted_border.dart';
import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class NavigationVoiceGuideCard extends StatelessWidget {
  final String voiceGuide;

  const NavigationVoiceGuideCard({super.key, required this.voiceGuide});

  @override
  Widget build(BuildContext context) {
    return DottedBorder(
      borderType: BorderType.RRect,
      radius: const Radius.circular(10),
      dashPattern: const [4, 4], // 점 길이, 간격
      color: ColorCollection.yellow,
      strokeWidth: 2,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: ColorCollection.yellow.withOpacity(0.1),
          borderRadius: BorderRadius.circular(10),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const Icon(
                  Icons.warning_amber_rounded,
                  color: ColorCollection.yellow,
                  size: 40,
                ),
                const SizedBox(width: 10),
                Text(
                  '주의',
                  style: AppTextStyles.title2.copyWith(
                    color: ColorCollection.point,
                    fontSize: 20,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              voiceGuide,
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
