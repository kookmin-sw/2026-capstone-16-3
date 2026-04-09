import 'package:flutter/material.dart';
import 'package:safepath/common/enum/place_category.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/features/navigation/category_item.dart';

class CategorySection extends StatelessWidget {
  final PlaceCategory? selectedCategory;
  final Function(PlaceCategory) onSelected;

  const CategorySection({
    super.key,
    required this.selectedCategory,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '대표 아이콘 설정',
          style: AppTextStyles.title2.copyWith(
            color: ColorCollection.point,
            fontSize: 20,
          ),
        ),
        const SizedBox(height: 15),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(10, 15, 10, 15),
          decoration: BoxDecoration(
            color: ColorCollection.point.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.point, width: 1),
          ),
          child: LayoutBuilder(
            builder: (context, constraints) {
              final itemWidth = (constraints.maxWidth - 20) / 3;

              return Wrap(
                alignment: WrapAlignment.center,
                spacing: 5, // 가로 간격
                runSpacing: 10, // 세로 간격
                children: PlaceCategory.values.map((category) {
                  return SizedBox(
                    width: itemWidth,
                    child: CategoryItem(
                      category: category,
                      isSelected: selectedCategory == category,
                      onTap: () => onSelected(category),
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ),
      ],
    );
  }
}
