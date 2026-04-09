import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/text_input_bar.dart';

class PlaceNameSection extends StatelessWidget {
  final TextEditingController controller;
  final VoidCallback onMicTap;
  const PlaceNameSection({
    super.key,
    required this.controller,
    required this.onMicTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '장소 이름 설정',
          style: AppTextStyles.title2.copyWith(
            color: ColorCollection.point,
            fontSize: 20,
          ),
        ),
        const SizedBox(height: 15),
        TextInputBar(
          controller: controller,
          hintText: '장소 이름을 입력하세요.',
          micTap: onMicTap,
          onSubmitted: (_) {},
        ),
      ],
    );
  }
}
