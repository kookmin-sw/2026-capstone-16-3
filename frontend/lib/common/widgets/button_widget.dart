import 'package:flutter/material.dart';

import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/theme/color_collection.dart';

/// 커스텀 버튼 위젯
///
/// 사용 예시:
/// // 아이콘 + 큰제목 + 부제목
/// CustomButton(
///   icon: Icons.location_on,
///   title: '스마트 길찾기',
///   subtitle: '가장 안전한 경로로 안내합니다.',
///   onTap: () {},
/// )
///
/// // 아이콘 없이, 테두리·배경 커스텀
/// CustomButton(
///   title: '사용 방법',
///   subtitle: '사용 가이드 보기',
///   titleColor: ColorCollection.point,
///   borderColor: ColorCollection.point,
///   onTap: () {},
/// )
///
class CustomButton extends StatelessWidget {
  /// 큰 제목 (null이면 표시 안 함)
  final String? title;

  /// 작은 부제목 (null이면 표시 안 함)
  final String? subtitle;

  /// 아이콘 (null이면 표시 안 함)
  final IconData? icon;

  /// 아이콘 크기 (기본 40)
  final double iconSize;

  /// 아이콘 색상 (기본 컬러 : main)
  final Color iconColor;

  /// 버튼 너비 (null이면 부모 너비에 맞춤)
  final double? width;

  /// 버튼 높이 (null이면 내용에 맞춤)
  final double? height;

  /// 내부 패딩 (기본 24/16)
  final EdgeInsetsGeometry padding;

  /// 배경 색상 (기본 컬러 : background)
  final Color backgroundColor;

  /// 테두리 색상 (기본 컬러 : main)
  final Color borderColor;

  /// 테두리 두께 (기본 3)
  final double borderWidth;

  /// 모서리 둥글기 (기본 10)
  final double borderRadius;

  /// 큰 제목 색상 (기본 컬러 : main)
  final Color titleColor;

  /// 작은 부제목 색상 (기본 컬러 : point)
  final Color subtitleColor;

  /// 큰 제목 스타일 (null이면 AppTextStyles.title1 사용)
  final TextStyle? titleStyle;

  /// 작은 부제목 스타일 (null이면 AppTextStyles.bodyRegular 사용)
  final TextStyle? subtitleStyle;

  /// 아이콘 ↔ 제목 사이 간격 (기본 10)
  final double iconTitleSpacing;

  /// 제목 ↔ 부제목 사이 간격 (기본 13)
  final double titleSubtitleSpacing;

  final VoidCallback? onTap;

  const CustomButton({
    super.key,
    this.title,
    this.subtitle,
    this.icon,
    this.iconSize = 40,
    this.iconColor = ColorCollection.main,
    this.width,
    this.height,
    this.padding = const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
    this.backgroundColor = ColorCollection.background,
    this.borderColor = ColorCollection.main,
    this.borderWidth = 3,
    this.borderRadius = 10,
    this.titleColor = ColorCollection.main,
    this.subtitleColor = ColorCollection.point,
    this.titleStyle,
    this.subtitleStyle,
    this.iconTitleSpacing = 10,
    this.titleSubtitleSpacing = 13,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    // Semantics: 스크린 리더(TalkBack/VoiceOver)에 버튼 역할과 내용을 한 번에 전달
    // excludeSemantics: 내부 Icon·Text가 개별로 읽히지 않도록 차단
    return Semantics(
      button: true,
      label: [title, subtitle].whereType<String>().join(', '),
      enabled: onTap != null,
      onTap: onTap,
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(borderRadius),
        child: Container(
          width: width,
          height: height,
          padding: padding,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(borderRadius),
            border: Border.all(color: borderColor, width: borderWidth),
          ),
          child: FittedBox(
            fit: BoxFit.scaleDown,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (icon != null) ...[
                  Icon(icon, color: iconColor, size: iconSize),
                  SizedBox(height: iconTitleSpacing),
                ],
                if (title != null)
                  Text(
                    title!,
                    style: (titleStyle ?? AppTextStyles.title1).copyWith(
                      color: titleColor,
                    ),
                    textAlign: TextAlign.center,
                  ),
                if (subtitle != null) ...[
                  SizedBox(height: titleSubtitleSpacing),
                  Text(
                    subtitle!,
                    style: (subtitleStyle ?? AppTextStyles.bodyRegular).copyWith(
                      color: subtitleColor,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
