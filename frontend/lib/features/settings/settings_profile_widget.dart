import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 설정 화면 상단 유저 프로필 행
class SettingsProfileWidget extends StatelessWidget {
  final String name;
  final VoidCallback? onTap;

  const SettingsProfileWidget({super.key, required this.name, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '프로필: $name. 프로필 편집',
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          decoration: BoxDecoration(
            color: ColorCollection.main,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.main, width: 2),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.account_circle_outlined,
                color: ColorCollection.point,
                size: 40,
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  name,
                  style: AppTextStyles.bodyBold.copyWith(
                    color: ColorCollection.point,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const Icon(
                Icons.chevron_right,
                color: ColorCollection.point,
                size: 30,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
