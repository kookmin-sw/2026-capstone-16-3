import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

enum RouteStatus { safe, warning, danger }

class NavigationOverviewCard extends StatelessWidget {
  final int distance; // 남은 거리
  final int time; // 남은 소요 시간
  final RouteStatus status;

  const NavigationOverviewCard({
    super.key,
    required this.distance,
    required this.time,
    required this.status,
  });

  String _getStatusText(RouteStatus status) {
    switch (status) {
      case RouteStatus.safe:
        return '안전';
      case RouteStatus.warning:
        return '주의';
      case RouteStatus.danger:
        return '위험';
    }
  }

  Color _getStatusColor(RouteStatus status) {
    switch (status) {
      case RouteStatus.safe:
        return ColorCollection.green;
      case RouteStatus.warning:
        return ColorCollection.yellow;
      case RouteStatus.danger:
        return ColorCollection.red;
    }
  }

  String _formatDistance(int distance) {
    if (distance >= 1000) {
      return '${(distance / 1000).toStringAsFixed(1)}km';
    }
    return '${distance}m';
  }

  String _formatTime(int time) {
    if (time >= 60) {
      final hour = time ~/ 60;
      final minute = time % 60;

      if (minute == 0) {
        return '${hour}시간';
      }
      return '${hour}시간 ${minute}분';
    }
    return '${time}분';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.point, width: 3),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              _formatDistance(distance),
              style: AppTextStyles.title1.copyWith(
                color: ColorCollection.point,
                fontSize: 40,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '총 거리',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
              ),
            ),
            const SizedBox(height: 8),
            Divider(color: ColorCollection.point, thickness: 2),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _formatTime(time),
                        style: AppTextStyles.title2.copyWith(
                          color: ColorCollection.point,
                          fontSize: 20,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '총 소요 시간',
                        style: AppTextStyles.labelBold.copyWith(
                          color: ColorCollection.point,
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _getStatusText(status),
                        style: AppTextStyles.title2.copyWith(
                          color: _getStatusColor(status),
                          fontSize: 20,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '경로 상태',
                        style: AppTextStyles.labelBold.copyWith(
                          color: ColorCollection.point,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
