import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class EditButton extends StatelessWidget {
  final VoidCallback? onTap;
  const EditButton({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          constraints: const BoxConstraints(minHeight: 75),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: ColorCollection.main.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.main, width: 1),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.add, color: ColorCollection.main, size: 30),
              const SizedBox(width: 6),
              Text(
                '추가',
                style: AppTextStyles.title2.copyWith(
                  color: ColorCollection.main,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
