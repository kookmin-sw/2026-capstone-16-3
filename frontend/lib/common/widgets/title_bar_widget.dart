import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 커스텀 타이틀 바 위젯
///
/// 사용 예시:
/// // 뒤로가기 버튼 없는 버전
/// CustomTitleBar(title: '실외 장애물 탐지')
///
/// // 뒤로가기 버튼 있는 버전 (기본 동작: Navigator.pop)
/// CustomTitleBar(title: '사용 방법', showBackButton: true)
///
/// // 뒤로가기 동작 커스텀 (예: 홈 버튼)
/// CustomTitleBar(
///   title: '사용 방법',
///   showBackButton: true,
///   onBack: () => Navigator.pushReplacementNamed(context, AppRouter.home),
/// )
///
class CustomTitleBar extends StatelessWidget implements PreferredSizeWidget {
  /// 타이틀 텍스트
  final String title;

  /// 뒤로가기 버튼 표시 여부 (기본값: false)
  final bool showBackButton;

  /// 뒤로가기 버튼 탭 콜백 (null이면 Navigator.pop 동작)
  final VoidCallback? onBack;

  const CustomTitleBar({
    super.key,
    required this.title,
    this.showBackButton = false,
    this.onBack,
  });

  @override
  Size get preferredSize => const Size.fromHeight(64);

  @override
  Widget build(BuildContext context) {
    final topPadding = MediaQuery.of(context).padding.top;
    return Container(
      width: double.infinity,
      color: ColorCollection.background,
      padding: EdgeInsets.fromLTRB(16, topPadding, 16, 0),
      child: Stack(
        alignment: Alignment.center,
        children: [
          if (showBackButton)
            Align(
              alignment: Alignment.centerLeft,
              child: Semantics(
                button: true,
                label: '뒤로 가기',
                excludeSemantics: true,
                child: InkWell(
                  onTap: onBack ?? () => Navigator.pop(context),
                  borderRadius: BorderRadius.circular(8),
                  child: Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: ColorCollection.point.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color: ColorCollection.point.withValues(alpha: 0.4),
                        width: 1.5,
                      ),
                    ),
                    child: const Icon(
                      Icons.arrow_back,
                      color: ColorCollection.point,
                      size: 24,
                    ),
                  ),
                ),
              ),
            ),
          Text(
            title,
            style: AppTextStyles.title2.copyWith(color: ColorCollection.point),
          ),
        ],
      ),
    );
  }
}
