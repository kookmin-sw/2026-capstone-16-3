import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class UserGuideWidget extends StatelessWidget {
  final String title; // 제목
  final String description; // 설명글
  final int number; // 순서
  final Color? titleColor; // default : main
  final Color? descriptionColor; // default : point

  const UserGuideWidget({
    super.key,
    required this.title,
    required this.description,
    required this.number,
    this.titleColor = ColorCollection.main,
    this.descriptionColor = ColorCollection.point,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ColorCollection.point.withOpacity(0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.point, width: 2),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 45,
                  height: 45,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: ColorCollection.main,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    '$number',
                    style: AppTextStyles.headline.copyWith(
                      color: ColorCollection.background,
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    title,
                    style: AppTextStyles.title2.copyWith(
                      color: titleColor ?? ColorCollection.main,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 21),
            Text(
              description,
              style: AppTextStyles.bodyRegular.copyWith(
                color: descriptionColor ?? ColorCollection.point,
              ),
            ),
            const SizedBox(height: 10),
          ],
        ),
      ),
    );
  }
}
