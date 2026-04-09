import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 사용 예시 :
/// 1) 활성화된 상태 - onTap != null
/// ActionButton(
///   label : '안내 시작',
///   icon : Icons.play,
///   onTap : () {},
///)
/// 2) 비활성화된 상태 - onTap == null
/// ActionButton(
///   label : '안내 시작',
///   icon : Icons.play,
///)

class ActionButton extends StatelessWidget {
  final String label; // 버튼 라벨
  final Color? textColor; // 라벨 텍스트 색상 (default : point)
  final TextStyle? textStyle; // 라벨 텍스트 스타일 (default : title2)
  final Color? backgroundColor; // 버튼 색상 (default : main)
  final IconData? icon; // 아이콘
  final Color? iconColor; // 아이콘 색상 (default : point)
  final double? iconSize; // 아이콘 크기 (default : 35)
  final VoidCallback? onTap; // 클릭시 동작 함수 (null이면 버튼 비활성화)

  const ActionButton({
    super.key,
    required this.label,
    this.textColor = ColorCollection.point,
    this.textStyle,
    this.backgroundColor = ColorCollection.main,
    this.icon,
    this.iconColor = ColorCollection.point,
    this.iconSize = 35,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final bool isEnabled = onTap != null; // 버튼의 비활성화, 활성화 여부 관리

    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          constraints: const BoxConstraints(minHeight: 75),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: isEnabled ? backgroundColor : Colors.grey,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: backgroundColor ?? ColorCollection.main,
              width: 2,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, color: iconColor, size: iconSize),
                const SizedBox(width: 6),
              ],
              Text(
                label,
                style: (textStyle ?? AppTextStyles.title2).copyWith(
                  color: textColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
