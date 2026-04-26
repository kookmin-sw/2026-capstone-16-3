import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/features/navigation/navigation_step_card.dart';

class NavigationStartOverviewCard extends StatelessWidget {
  final int totalDistance;
  final int totalTime;
  final int firstStepDistance;
  final DirectionType firstStepDirection;

  const NavigationStartOverviewCard({
    super.key,
    required this.totalDistance,
    required this.totalTime,
    required this.firstStepDistance,
    required this.firstStepDirection,
  });

  String _formatDistance(int distance) {
    if (distance >= 1000) return '${(distance / 1000).toStringAsFixed(1)}km';
    return '${distance}m';
  }

  String _formatTime(int time) {
    if (time >= 60) {
      final hour = time ~/ 60;
      final minute = time % 60;
      return minute == 0 ? '${hour}시간' : '${hour}시간 ${minute}분';
    }
    return '${time}분';
  }

  String _directionLabel(DirectionType d) {
    switch (d) {
      case DirectionType.left:
        return '좌회전';
      case DirectionType.right:
        return '우회전';
      case DirectionType.crosswalk:
        return '횡단보도 이용';
      case DirectionType.straight:
        return '직진';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.main, width: 2),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '길안내를 시작합니다.',
            style: AppTextStyles.labelBold.copyWith(color: ColorCollection.point),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const Icon(Icons.straighten, color: ColorCollection.point, size: 18),
              const SizedBox(width: 6),
              Text(
                '총 ${_formatDistance(totalDistance)}',
                style: AppTextStyles.labelRegular.copyWith(color: ColorCollection.point),
              ),
              const SizedBox(width: 16),
              const Icon(Icons.access_time, color: ColorCollection.point, size: 18),
              const SizedBox(width: 6),
              Text(
                '약 ${_formatTime(totalTime)}',
                style: AppTextStyles.labelRegular.copyWith(color: ColorCollection.point),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            '${_formatDistance(firstStepDistance)} 앞에서 ${_directionLabel(firstStepDirection)}하세요',
            style: AppTextStyles.title2.copyWith(
              color: ColorCollection.point,
              fontSize: 18,
            ),
          ),
        ],
      ),
    );
  }
}