import 'package:flutter/material.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/theme/color_collection.dart';

/// SafePath 로고 위젯
///
/// 사용 예시:
/// const LogoWidget()
/// const LogoWidget(imageSize: 100)
class LogoWidget extends StatelessWidget {
  const LogoWidget({super.key, this.imageSize = 80});

  final double imageSize;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // 로고 이미지
        Container(
          width: imageSize,
          height: imageSize,
          decoration: BoxDecoration(
            color: ColorCollection.main,
            borderRadius: BorderRadius.circular(16),
          ),
          // child: Icon(
          //   Icons.route,
          //   size: imageSize * 0.6,
          //   color: ColorCollection.point,
          // ),
          // TODO: 이미지 확정 시 아래 코드로 교체
          child: Image.asset(
            'assets/images/logo.png',
            width: imageSize,
            height: imageSize,
          ),
        ),
        const SizedBox(height: 16),
        // SafePath 텍스트
        Text(
          '길벗',
          style: AppTextStyles.title1.copyWith(color: ColorCollection.point),
        ),
        const SizedBox(height: 4),
        // 서브 텍스트
        Text(
          'AI 보행보조 어플리케이션',
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
      ],
    );
  }
}
