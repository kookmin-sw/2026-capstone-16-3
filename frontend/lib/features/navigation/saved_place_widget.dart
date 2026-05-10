import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/enum/place_category.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/vibration_service.dart';

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
    final bool isTappable = !isEditMode && onTap != null;

    final Widget card = Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: isTappable
            ? () {
                SoundEffectService().play(SoundEffect.buttonTap);
                VibrationService().vibrate(VibrationEffect.buttonTap);
                onTap!();
              }
            : null,
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
                  // 아이콘은 label에 포함되므로 개별 읽기 제외
                  ExcludeSemantics(
                    child: Container(
                      width: 35,
                      height: 35,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: iconBackgroundColor,
                        borderRadius: BorderRadius.circular(5),
                      ),
                      child: Icon(category?.icon, color: iconColor, size: 25),
                    ),
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
                  // 삭제 버튼은 카드와 독립된 별도 semantics 노드
                  // Semantics.onTap: TalkBack 더블탭 시 ACTION_CLICK을 직접 수신
                  Semantics(
                    button: true,
                    label: '$label 삭제',
                    excludeSemantics: true,
                    onTap: onDelete == null
                        ? null
                        : () {
                            SoundEffectService().play(SoundEffect.buttonTap);
                            VibrationService().vibrate(VibrationEffect.buttonTap);
                            onDelete!();
                          },
                    child: GestureDetector(
                      onTap: onDelete == null
                          ? null
                          : () {
                              SoundEffectService().play(SoundEffect.buttonTap);
                              VibrationService().vibrate(VibrationEffect.buttonTap);
                              onDelete!();
                            },
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
                    ),
                  )
                else
                  // 화살표 아이콘은 장식용 — 개별 읽기 제외
                  ExcludeSemantics(
                    child: Icon(
                      Icons.arrow_forward_ios_rounded,
                      color: ColorCollection.main,
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );

    // 일반 모드: 카드 전체를 하나의 버튼으로 읽힘
    // 편집 모드: 카드는 비활성, 삭제 버튼이 별도 노드로 읽힘
    if (isEditMode) return card;

    return Semantics(
      button: true,
      label: '$label, $location',
      enabled: onTap != null,
      onTap: onTap,
      excludeSemantics: true,
      child: card,
    );
  }
}