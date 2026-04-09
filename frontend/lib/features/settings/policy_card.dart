import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/models/policy_section.dart';

class PolicyCard extends StatelessWidget {
  final PolicySection section;
  final bool isSummary;

  const PolicyCard({super.key, required this.section, this.isSummary = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: EdgeInsets.only(bottom: isSummary ? 0 : 20),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        border: Border.all(color: ColorCollection.main),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        crossAxisAlignment: isSummary
            ? CrossAxisAlignment.center
            : CrossAxisAlignment.start,
        children: [
          Text(
            section.title,
            style: AppTextStyles.bodyBold.copyWith(
              color: isSummary ? ColorCollection.main : ColorCollection.point,
            ),
          ),
          const SizedBox(height: 15),
          Text(
            section.content,
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
            ),
          ),
        ],
      ),
    );
  }
}
