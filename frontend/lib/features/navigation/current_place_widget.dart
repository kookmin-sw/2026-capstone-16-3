import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class CurrentPlaceWidget extends StatelessWidget {
  final String label; // 위치 라벨
  final String location; // 장소 주소
  final Color? labelTextColor; // default : point
  final Color? locationTextColor; // default : point
  final Color? iconColor; // default : point
  final Color? iconBackgroundColor; // default : main

  const CurrentPlaceWidget({
    super.key,
    required this.label,
    required this.location,
    this.labelTextColor = ColorCollection.point,
    this.locationTextColor = ColorCollection.point,
    this.iconColor = ColorCollection.point,
    this.iconBackgroundColor = ColorCollection.main,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ColorCollection.point.withOpacity(0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.point, width: 1),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 16, 16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 35,
              height: 35,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: iconBackgroundColor,
                borderRadius: BorderRadius.circular(5),
              ),
              child: Icon(Icons.location_pin, color: iconColor),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    softWrap: true,
                    style: AppTextStyles.labelRegular.copyWith(
                      color: labelTextColor ?? ColorCollection.point,
                    ),
                  ),
                  Text(
                    location,
                    softWrap: true,
                    style: AppTextStyles.bodyBold.copyWith(
                      color: labelTextColor ?? ColorCollection.point,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
