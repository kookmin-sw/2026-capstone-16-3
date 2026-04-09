import 'package:flutter/material.dart';
import 'package:safepath/common/enum/place_category.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class CategoryItem extends StatelessWidget {
  final PlaceCategory category;
  final bool isSelected; // 선택되었을 때의 UI
  final VoidCallback? onTap;

  const CategoryItem({
    super.key,
    required this.category,
    this.isSelected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.fromLTRB(2, 5, 2, 5),
        decoration: BoxDecoration(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: isSelected ? ColorCollection.point : Colors.transparent,
            width: 3,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 50,
              height: 50,
              decoration: BoxDecoration(
                color: ColorCollection.main,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(
                category.icon,
                color: ColorCollection.point,
                size: 35,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              category.label,
              textAlign: TextAlign.center,
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
                fontSize: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
