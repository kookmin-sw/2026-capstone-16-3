import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 앱 정보 섹션 (페이지 연결 행 목록)
class SettingsInfoWidget extends StatelessWidget {
  final List<SettingsInfoItem> items;

  const SettingsInfoWidget({super.key, required this.items});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: items
          .map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _InfoRow(item: item),
            ),
          )
          .toList(),
    );
  }
}

class SettingsInfoItem {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  const SettingsInfoItem({required this.icon, required this.label, this.onTap});
}

class _InfoRow extends StatelessWidget {
  final SettingsInfoItem item;

  const _InfoRow({required this.item});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: item.label,
      excludeSemantics: true,
      child: InkWell(
        onTap: item.onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
          decoration: BoxDecoration(
            color: ColorCollection.point.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.point, width: 1),
          ),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: ColorCollection.main,
                  borderRadius: BorderRadius.circular(5),
                ),
                child: Icon(item.icon, color: ColorCollection.point, size: 25),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Text(
                  item.label,
                  style: AppTextStyles.bodyBold.copyWith(
                    color: ColorCollection.point,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const Icon(
                Icons.chevron_right,
                color: ColorCollection.point,
                size: 26,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
