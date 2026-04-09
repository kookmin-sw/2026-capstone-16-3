import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class TextInputBar extends StatelessWidget {
  final TextEditingController controller; // 텍스트 컨트롤러
  final String? hintText; // 힌트 텍스트
  final bool showSearchIcon; // 검색 아이콘 표시 여부 (default : true)
  final VoidCallback? micTap; // 음성 텍스트 입력 아이콘
  final ValueChanged<String>? onSubmitted;
  final VoidCallback? onClear; // 텍스트 및 외부 상태

  const TextInputBar({
    super.key,
    required this.controller,
    this.hintText,
    this.showSearchIcon = true,
    this.micTap,
    this.onSubmitted,
    this.onClear,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: TextField(
            controller: controller,
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
            ),
            onSubmitted: onSubmitted,
            onChanged: (_) {},
            decoration: InputDecoration(
              prefixIcon: showSearchIcon
                  ? const Icon(Icons.search, color: ColorCollection.main)
                  : null,
              suffixIcon: ValueListenableBuilder(
                valueListenable: controller,
                builder: (context, value, child) {
                  if (value.text.isEmpty) {
                    return const SizedBox.shrink();
                  }
                  return IconButton(
                    icon: const Icon(Icons.close, color: ColorCollection.main),
                    onPressed: () {
                      // 부모에서 상태 + controller 모두 관리하도록 위임
                      if (onClear != null) {
                        onClear!();
                      } else {
                        controller.clear(); // fallback
                      }
                    },
                  );
                },
              ),
              hintText: hintText,
              contentPadding: const EdgeInsets.symmetric(
                vertical: 16,
                horizontal: 12,
              ),
              filled: true,
              fillColor: ColorCollection.background,
              hintStyle: AppTextStyles.labelRegular.copyWith(
                color: ColorCollection.point,
                fontSize: 15,
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: BorderSide(color: ColorCollection.main, width: 2),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(10),
                borderSide: BorderSide(color: ColorCollection.main, width: 2),
              ),
            ),
          ),
        ),
        const SizedBox(width: 14),
        Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(10),
          child: InkWell(
            borderRadius: BorderRadius.circular(10),
            onTap: micTap,
            child: Container(
              width: 55,
              height: 55,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: ColorCollection.main,
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.mic,
                color: ColorCollection.point,
                size: 40,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
