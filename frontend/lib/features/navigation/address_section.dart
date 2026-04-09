import 'package:flutter/material.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/widgets/text_input_bar.dart';
import 'package:safepath/features/navigation/current_place_widget.dart';

class AddressSection extends StatelessWidget {
  final TextEditingController controller;
  final String currentLocation;
  final Function(String) onSubmitted;
  final VoidCallback onClear;
  final VoidCallback onMicTap;

  const AddressSection({
    super.key,
    required this.controller,
    required this.currentLocation,
    required this.onSubmitted,
    required this.onClear,
    required this.onMicTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '장소 주소 선택',
          style: AppTextStyles.title2.copyWith(
            color: ColorCollection.point,
            fontSize: 20,
          ),
        ),
        const SizedBox(height: 15),
        TextInputBar(
          controller: controller,
          onClear: onClear,
          hintText: '목적지를 입력하세요.',
          micTap: onMicTap,
          onSubmitted: onSubmitted,
        ),
        const SizedBox(height: 15),
        CurrentPlaceWidget(label: '선택된 위치', location: currentLocation),
      ],
    );
  }
}
