import 'package:flutter/material.dart';

import 'package:safepath/common/widgets/logo_widget.dart';
import 'package:safepath/common/widgets/button_widget.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/theme/color_collection.dart';

class HomeScreen extends StatelessWidget {
  final void Function(int) onTabChange;
  const HomeScreen({super.key, required this.onTabChange});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              const SizedBox(height: 24),
              const Center(child: LogoWidget()),
              const SizedBox(height: 24),
              Expanded(
                child: CustomButton(
                  icon: Icons.remove_red_eye,
                  iconColor: ColorCollection.main,
                  iconSize: 38,
                  title: '장애물 탐지',
                  subtitle: '실시간으로 보행 위험물을 감지합니다.',
                  backgroundColor: ColorCollection.background,
                  titleColor: ColorCollection.main,
                  titleStyle: AppTextStyles.title2,
                  subtitleColor: ColorCollection.point,
                  subtitleStyle: AppTextStyles.labelRegular,
                  borderColor: ColorCollection.main,
                  onTap: () => onTabChange(1),
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: CustomButton(
                  icon: Icons.navigation,
                  iconColor: ColorCollection.main,
                  iconSize: 38,
                  title: '스마트 길찾기',
                  subtitle: '가장 안전한 경로로 안내합니다.',
                  backgroundColor: ColorCollection.background,
                  titleColor: ColorCollection.main,
                  titleStyle: AppTextStyles.title2,
                  subtitleColor: ColorCollection.point,
                  subtitleStyle: AppTextStyles.labelRegular,
                  borderColor: ColorCollection.main,
                  onTap: () => onTabChange(2),
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: CustomButton(
                  icon: Icons.directions_walk_rounded,
                  iconColor: ColorCollection.main,
                  iconSize: 38,
                  title: '통합 모드',
                  subtitle: '경로 안내와 장애물 감지를 함께 제공합니다.',
                  backgroundColor: ColorCollection.background,
                  titleColor: ColorCollection.main,
                  titleStyle: AppTextStyles.title2,
                  subtitleColor: ColorCollection.point,
                  subtitleStyle: AppTextStyles.labelRegular,
                  borderColor: ColorCollection.main,
                  onTap: () => onTabChange(3),
                ),
              ),
              const SizedBox(height: 16),
              Expanded(
                child: CustomButton(
                  icon: Icons.settings,
                  iconColor: ColorCollection.main,
                  iconSize: 38,
                  title: '설정',
                  subtitle: '사용자 맞춤 설정을 진행합니다.',
                  backgroundColor: ColorCollection.background,
                  titleColor: ColorCollection.main,
                  titleStyle: AppTextStyles.title2,
                  subtitleColor: ColorCollection.point,
                  subtitleStyle: AppTextStyles.labelRegular,
                  borderColor: ColorCollection.main,
                  onTap: () => onTabChange(4),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}
