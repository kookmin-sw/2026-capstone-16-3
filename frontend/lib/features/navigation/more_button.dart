import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/routes/app_router.dart';

class MoreButton extends StatelessWidget {
  final VoidCallback? onTap;
  const MoreButton({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 5),
          decoration: BoxDecoration(
            color: ColorCollection.point.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.point, width: 1),
          ),
          child: Padding(
            padding: const EdgeInsets.all(5),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Icon(
                  Icons.more_horiz_rounded,
                  size: 25,
                  color: ColorCollection.point,
                ),
                const SizedBox(width: 5),
                Text(
                  '더보기',
                  style: AppTextStyles.labelRegular.copyWith(
                    color: ColorCollection.point,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
