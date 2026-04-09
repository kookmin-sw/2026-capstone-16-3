import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 장애물과의 거리 단계
enum ObstacleProximity { near, mid, far }

/// 감지된 장애물 카드 위젯
///
/// 사용 예시:
/// ObstacleCard(
///   name: '오토바이',
///   distance: '가까움',
///   position: '1시 방향',
///   vibration: '강한 진동 (연이은 진동)',
///   proximity: ObstacleProximity.near,
///   guideText: '1시 방향에 오토바이가 있습니다. 중앙을 유지하며 천천히 직진하세요.',
/// )
class ObstacleCard extends StatelessWidget {
  final String name;
  final String distance;
  final String position;
  final String vibration;
  final ObstacleProximity proximity;

  /// AI 안내 문구 — WS 응답의 guideText
  final String guideText;

  const ObstacleCard({
    super.key,
    required this.name,
    required this.distance,
    required this.position,
    required this.vibration,
    required this.proximity,
    required this.guideText,
  });

  @override
  Widget build(BuildContext context) {
    final isNear = proximity == ObstacleProximity.near;

    final semanticsLabel =
        '$name. ${_proximityLabel(proximity)}. '
        '거리 : $distance. 위치 : $position. 진동 : $vibration.'
        '${isNear ? ' 즉시 주의 필요 - 매우 가까운 장애물.' : ''}';

    return Semantics(
      label: semanticsLabel,
      excludeSemantics: true,
      child: switch (proximity) {
        ObstacleProximity.near => _NearCard(this),
        ObstacleProximity.mid => _MidCard(this),
        ObstacleProximity.far => _FarCard(this),
      },
    );
  }

  static String _proximityLabel(ObstacleProximity p) => switch (p) {
    ObstacleProximity.near => 'Near',
    ObstacleProximity.mid => 'Mid',
    ObstacleProximity.far => 'Far',
  };
}

// ─── Near 카드 (실선 두꺼운 테두리) ───────────────────────────────────────────

class _NearCard extends StatelessWidget {
  final ObstacleCard data;
  const _NearCard(this.data);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: ColorCollection.main.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.main, width: 3),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _CardHeader(name: data.name, proximity: data.proximity),
            const SizedBox(height: 12),
            _InfoRow(icon: Icons.adjust, label: '거리', value: data.distance),
            const SizedBox(height: 8),
            _InfoRow(icon: Icons.explore, label: '위치', value: data.position),
            const SizedBox(height: 8),
            _InfoRow(icon: Icons.vibration, label: '진동', value: data.vibration),
            const SizedBox(height: 12),
            const Divider(color: ColorCollection.main, thickness: 1),
            const SizedBox(height: 8),
            Row(
              children: [
                const Icon(
                  Icons.notifications_active,
                  color: ColorCollection.main,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Text(
                  '즉시 주의 필요 - 매우 가까운 장애물',
                  style: AppTextStyles.labelBold.copyWith(
                    color: ColorCollection.main,
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

// ─── Mid 카드 (실선 두께 2) ───────────────────────────────────────────────────

class _MidCard extends StatelessWidget {
  final ObstacleCard data;
  const _MidCard(this.data);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.main, width: 2),
      ),
      padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _CardHeader(name: data.name, proximity: data.proximity),
          const SizedBox(height: 12),
          _InfoRow(icon: Icons.adjust, label: '거리', value: data.distance),
          const SizedBox(height: 8),
          _InfoRow(icon: Icons.explore, label: '위치', value: data.position),
          const SizedBox(height: 8),
          _InfoRow(icon: Icons.vibration, label: '진동', value: data.vibration),
        ],
      ),
    );
  }
}

// ─── Far 카드 (점선 테두리) ───────────────────────────────────────────────────

class _FarCard extends StatelessWidget {
  final ObstacleCard data;
  const _FarCard(this.data);

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _DashedBorderPainter(
        color: ColorCollection.point.withValues(alpha: 0.5),
        borderRadius: 10,
      ),
      child: Container(
        width: double.infinity,
        decoration: BoxDecoration(
          color: ColorCollection.point.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(10),
        ),
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _CardHeader(name: data.name, proximity: data.proximity),
            const SizedBox(height: 12),
            _InfoRow(icon: Icons.adjust, label: '거리', value: data.distance),
            const SizedBox(height: 8),
            _InfoRow(icon: Icons.explore, label: '위치', value: data.position),
            const SizedBox(height: 8),
            _InfoRow(icon: Icons.vibration, label: '진동', value: data.vibration),
          ],
        ),
      ),
    );
  }
}

// ─── 공통 서브 위젯 ───────────────────────────────────────────────────────────

class _CardHeader extends StatelessWidget {
  final String name;
  final ObstacleProximity proximity;

  const _CardHeader({required this.name, required this.proximity});

  @override
  Widget build(BuildContext context) {
    final isNear = proximity == ObstacleProximity.near;
    final icon = isNear ? Icons.notifications_active : Icons.info_outline;
    final label = ObstacleCard._proximityLabel(proximity);

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          name,
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        Row(
          children: [
            Icon(icon, color: ColorCollection.main, size: 18),
            const SizedBox(width: 4),
            Text(
              label,
              style: AppTextStyles.bodyBold.copyWith(
                color: ColorCollection.main,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, color: ColorCollection.main, size: 20),
        const SizedBox(width: 8),
        Text(
          '$label : $value',
          style: AppTextStyles.bodyRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
      ],
    );
  }
}

// ─── 점선 테두리 Painter ──────────────────────────────────────────────────────

class _DashedBorderPainter extends CustomPainter {
  final Color color;
  final double borderRadius;

  const _DashedBorderPainter({required this.color, required this.borderRadius});

  static const double _strokeWidth = 1.5;
  static const double _dashWidth = 6;
  static const double _dashSpace = 4;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = _strokeWidth
      ..style = PaintingStyle.stroke;

    final path = Path()
      ..addRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(
            _strokeWidth / 2,
            _strokeWidth / 2,
            size.width - _strokeWidth,
            size.height - _strokeWidth,
          ),
          Radius.circular(borderRadius),
        ),
      );

    canvas.drawPath(_dashPath(path), paint);
  }

  Path _dashPath(Path source) {
    final result = Path();
    for (final metric in source.computeMetrics()) {
      double distance = 0;
      while (distance < metric.length) {
        result.addPath(
          metric.extractPath(distance, distance + _dashWidth),
          Offset.zero,
        );
        distance += _dashWidth + _dashSpace;
      }
    }
    return result;
  }

  @override
  bool shouldRepaint(_DashedBorderPainter old) =>
      old.color != color || old.borderRadius != borderRadius;
}
