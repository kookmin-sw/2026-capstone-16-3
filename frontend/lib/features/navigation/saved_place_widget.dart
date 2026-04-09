import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/enum/place_category.dart';

class SavedPlaceWidget extends StatelessWidget {
  final String label; // 저장된 장소 이름
  final String location; // 장소 주소
  final PlaceCategory? category; // 카테고리 아이콘
  final bool isEditMode; // 편집 모드 여부
  final Color? labelTextColor; // default : point
  final Color? locationTextColor; // default : point
  final Color? iconBackgroundColor; // default : main
  final Color? iconColor; // default = point
  final VoidCallback? onTap;
  final VoidCallback? onDelete; // 삭제 버튼 클릭

  const SavedPlaceWidget({
    super.key,
    required this.label,
    required this.location,
    this.category,
    this.isEditMode = false,
    this.labelTextColor = ColorCollection.point,
    this.locationTextColor = ColorCollection.point,
    this.iconBackgroundColor = ColorCollection.main,
    this.iconColor = ColorCollection.point,
    this.onTap,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Container(
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
                if (category != null) ...[
                  Container(
                    width: 35,
                    height: 35,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: iconBackgroundColor,
                      borderRadius: BorderRadius.circular(5),
                    ),
                    child: Icon(category?.icon, color: iconColor, size: 25),
                  ),
                  const SizedBox(width: 14),
                ],
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        label,
                        softWrap: true,
                        style: AppTextStyles.bodyBold.copyWith(
                          color: labelTextColor ?? ColorCollection.point,
                        ),
                      ),
                      if (!isEditMode)
                        Text(
                          location,
                          softWrap: true,
                          style: AppTextStyles.labelRegular.copyWith(
                            color: locationTextColor ?? ColorCollection.point,
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                if (isEditMode)
                  GestureDetector(
                    onTap: onDelete,
                    child: Container(
                      width: 50,
                      height: 50,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: ColorCollection.red,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: ColorCollection.point,
                          width: 1,
                        ),
                      ),
                      child: const Icon(
                        Icons.delete,
                        color: ColorCollection.point,
                        size: 35,
                      ),
                    ),
                  )
                else
                  Icon(
                    Icons.arrow_forward_ios_rounded,
                    color: ColorCollection.main,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
