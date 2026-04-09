import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class DeleteDialog extends StatelessWidget {
  final VoidCallback onDelete;

  const DeleteDialog({super.key, required this.onDelete});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: ColorCollection.point, width: 2),
      ),
      backgroundColor: ColorCollection.background,
      title: Text(
        '삭제하시겠습니까?',
        style: AppTextStyles.title2.copyWith(color: ColorCollection.point),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(
            '취소',
            style: AppTextStyles.labelBold.copyWith(
              color: ColorCollection.main,
            ),
          ),
        ),
        TextButton(
          onPressed: () {
            onDelete();
            Navigator.pop(context);
          },
          child: Text(
            '삭제',
            style: AppTextStyles.labelBold.copyWith(
              color: ColorCollection.main,
            ),
          ),
        ),
      ],
    );
  }
}
