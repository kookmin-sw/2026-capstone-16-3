import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

enum DirectionType {
  straight, // 직진
  left, // 좌회전
  right, // 우회전
  crosswalk, // 횡단보도
}

class NavigationStepCard extends StatelessWidget {
  final DirectionType direction;
  final String instruction;
  final int distance;

  const NavigationStepCard({
    super.key,
    required this.direction,
    required this.instruction,
    required this.distance,
  });

  IconData _getDirectionIcon(DirectionType direction) {
    switch (direction) {
      case DirectionType.straight:
        return Icons.arrow_upward;
      case DirectionType.left:
        return Icons.turn_left;
      case DirectionType.right:
        return Icons.turn_right;
      case DirectionType.crosswalk:
        return Icons.directions_walk;
    }
  }

  String _getDirectionLabel(DirectionType direction) {
    switch (direction) {
      case DirectionType.straight:
        return '직진';
      case DirectionType.left:
        return '좌회전';
      case DirectionType.right:
        return '우회전';
      case DirectionType.crosswalk:
        return '횡단보도';
    }
  }

  String _formatDistance(int distance) {
    if (distance >= 1000) {
      return '${(distance / 1000).toStringAsFixed(1)}km';
    }
    return '${distance}m';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ColorCollection.point.withOpacity(0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.main, width: 2),
      ),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Row(
              children: [
                Container(
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: ColorCollection.main,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(
                    _getDirectionIcon(direction),
                    color: ColorCollection.point,
                    size: 45,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${_formatDistance(distance)} ${_getDirectionLabel(direction)}',
                        style: AppTextStyles.title2.copyWith(
                          color: ColorCollection.point,
                          fontSize: 20,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        '다음 안내까지',
                        style: AppTextStyles.labelRegular.copyWith(
                          color: ColorCollection.point,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),

            const SizedBox(height: 20),

            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 16),
              decoration: BoxDecoration(
                color: ColorCollection.main.withOpacity(0.5),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.volume_up_rounded,
                    color: ColorCollection.point,
                    size: 35,
                  ),
                  const SizedBox(width: 7),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(
                        instruction,
                        style: AppTextStyles.labelBold.copyWith(
                          color: ColorCollection.point,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
