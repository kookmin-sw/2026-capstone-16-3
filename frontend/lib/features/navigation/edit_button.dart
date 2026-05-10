import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/vibration_service.dart';

class EditButton extends StatelessWidget {
  final VoidCallback? onTap;
  const EditButton({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: '장소 추가',
      enabled: onTap != null,
      onTap: onTap,
      excludeSemantics: true,
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(10),
        child: InkWell(
          borderRadius: BorderRadius.circular(10),
          onTap: onTap == null
              ? null
              : () {
                  SoundEffectService().play(SoundEffect.buttonTap);
                  VibrationService().vibrate(VibrationEffect.buttonTap);
                  onTap!();
                },
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
      ),
    );
  }
}