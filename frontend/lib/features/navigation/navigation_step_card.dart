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
  final int? distance;
  final bool isApproaching;

  const NavigationStepCard({
    super.key,
    required this.direction,
    required this.instruction,
    this.distance,
    this.isApproaching = true,
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

  String _getTtsMessage(DirectionType direction, int? distance, String instruction) {
    if (!isApproaching) return instruction.isNotEmpty ? instruction : '계속 직진하세요';
    if (distance == null) return '계속 직진하세요';

    final action = switch (direction) {
      DirectionType.straight => '직진',
      DirectionType.left => '좌회전',
      DirectionType.right => '우회전',
      DirectionType.crosswalk => '횡단보도를 이용',
    };

    if (direction == DirectionType.straight) return '계속 직진하세요';
    if (distance <= 15) return '$action하세요';
    if (distance <= 30) return '잠시 후 $action하세요';
    if (distance <= 50) return instruction.isNotEmpty ? instruction : '잠시 후 $action하세요';
    return '계속 직진하세요';
  }

  String _formatDistance(int? distance) {
    if (distance == null) return '--';
    if (distance >= 1000) return '${(distance / 1000).toStringAsFixed(1)}km';
    return '${distance}m';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
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
                        direction == DirectionType.straight
                            ? '${_formatDistance(distance)} 직진'
                            : '${_formatDistance(distance)} 후 ${_getDirectionLabel(direction)}',
                        style: AppTextStyles.title2.copyWith(
                          color: ColorCollection.point,
                          fontSize: 20,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        instruction,
                        style: AppTextStyles.labelRegular.copyWith(
                          color: ColorCollection.point,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
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
                color: ColorCollection.main.withValues(alpha: 0.5),
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
                        _getTtsMessage(direction, distance, instruction),
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
