import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:safepath/features/navigation/navigation_step_card.dart';
import 'package:safepath/models/route_result.dart';

/// 디버그 전용 경로 안내 PiP 오버레이
///
/// kDebugMode일 때만 렌더링됨 — release 빌드에서는 SizedBox.shrink()로 대체
///
/// 사용 예시:
///   Stack(
///     children: [
///       ... 본 화면 위젯 ...
///       RouteDebugOverlay(
///         pointSteps: _pointSteps,
///         currentStepIndex: _currentStepIndex,
///         distanceToStep: _debugDistance,
///         outsideThreshold: _hasBeenOutsideThreshold,
///         threshold: _arrivalThresholdMeters,
///       ),
///     ],
///   )
class RouteDebugOverlay extends StatefulWidget {
  final List<RouteStep> pointSteps;
  final int currentStepIndex;
  final double? distanceToStep;
  final bool outsideThreshold;
  final double threshold;
  final double minDistanceToStep;
  final double? rawDistance;
  final double? segmentDist;
  final int deviationCount;
  final int pathIndex;

  const RouteDebugOverlay({
    super.key,
    required this.pointSteps,
    required this.currentStepIndex,
    required this.distanceToStep,
    required this.outsideThreshold,
    required this.threshold,
    this.minDistanceToStep = double.infinity,
    this.rawDistance,
    this.segmentDist,
    this.deviationCount = 0,
    this.pathIndex = 0,
  });

  @override
  State<RouteDebugOverlay> createState() => _RouteDebugOverlayState();
}

class _RouteDebugOverlayState extends State<RouteDebugOverlay> {
  bool _visible = true;

  @override
  Widget build(BuildContext context) {
    if (!kDebugMode) return const SizedBox.shrink();

    final vp = MediaQuery.of(context).viewPadding;

    if (!_visible) {
      return Positioned(
        right: 8 + vp.right,
        top: 8 + vp.top,
        child: ExcludeSemantics(child: GestureDetector(
          onTap: () => setState(() => _visible = true),
          child: Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.6),
              borderRadius: BorderRadius.circular(6),
            ),
            child: const Icon(Icons.navigation, color: Colors.white, size: 18),
          ),
        )),
      );
    }

    return Positioned(
      right: 8 + vp.right,
      top: 8 + vp.top,
      child: ExcludeSemantics(child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          // 이탈 감지 상태 표시
          Container(
            width: 210,
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            color: Colors.black.withValues(alpha: 0.85),
            child: Text(
              'DEV  seg:${widget.segmentDist != null ? '${widget.segmentDist!.toStringAsFixed(1)}m' : '--'}  cnt:${widget.deviationCount}/3  path:${widget.pathIndex}',
              style: TextStyle(
                color: widget.deviationCount > 0 ? Colors.orange : Colors.cyan,
                fontSize: 10,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          // 헤더 (닫기 버튼)
          GestureDetector(
            onTap: () => setState(() => _visible = false),
            child: Container(
              width: 210,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.85),
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(8),
                  topRight: Radius.circular(8),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'ROUTE DEBUG',
                    style: TextStyle(
                      color: Colors.yellow,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Row(
                    children: [
                      Text(
                        'Step ${widget.currentStepIndex}/${widget.pointSteps.length}',
                        style: const TextStyle(
                          color: Colors.white70,
                          fontSize: 10,
                        ),
                      ),
                      const SizedBox(width: 6),
                      const Icon(Icons.close, color: Colors.white, size: 14),
                    ],
                  ),
                ],
              ),
            ),
          ),

          // step 목록
          Container(
            width: 210,
            constraints: const BoxConstraints(maxHeight: 260),
            decoration: BoxDecoration(
              color: Colors.black.withValues(alpha: 0.75),
              borderRadius: const BorderRadius.only(
                bottomLeft: Radius.circular(8),
                bottomRight: Radius.circular(8),
              ),
            ),
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(6),
              child: Column(
                children: [
                  for (int i = 0; i < widget.pointSteps.length; i++)
                    _StepDebugCard(
                      step: widget.pointSteps[i],
                      isCurrent: i == widget.currentStepIndex,
                      distanceToStep: i == widget.currentStepIndex
                          ? widget.distanceToStep
                          : null,
                      rawDistance: i == widget.currentStepIndex
                          ? widget.rawDistance
                          : null,
                      outsideThreshold: i == widget.currentStepIndex
                          ? widget.outsideThreshold
                          : null,
                      threshold: widget.threshold,
                      minDistanceToStep: i == widget.currentStepIndex
                          ? widget.minDistanceToStep
                          : double.infinity,
                    ),
                ],
              ),
            ),
          ),
        ],
      )),
    );
  }
}

class _StepDebugCard extends StatelessWidget {
  final RouteStep step;
  final bool isCurrent;
  final double? distanceToStep;
  final double? rawDistance;
  final bool? outsideThreshold;
  final double threshold;
  final double minDistanceToStep;

  const _StepDebugCard({
    required this.step,
    required this.isCurrent,
    required this.distanceToStep,
    required this.outsideThreshold,
    required this.threshold,
    this.rawDistance,
    this.minDistanceToStep = double.infinity,
  });

  DirectionType _turnTypeToDirection(int? turnType) {
    switch (turnType) {
      case 12:
      case 16:
      case 17:
        return DirectionType.left;
      case 13:
      case 18:
      case 19:
        return DirectionType.right;
      case 211:
      case 212:
      case 213:
      case 214:
      case 215:
      case 216:
      case 217:
      case 125:
      case 126:
        return DirectionType.crosswalk;
      default:
        return DirectionType.straight;
    }
  }

  IconData _directionIcon(DirectionType d) {
    switch (d) {
      case DirectionType.left:
        return Icons.turn_left;
      case DirectionType.right:
        return Icons.turn_right;
      case DirectionType.crosswalk:
        return Icons.directions_walk;
      case DirectionType.straight:
        return Icons.arrow_upward;
    }
  }

  @override
  Widget build(BuildContext context) {
    final direction = _turnTypeToDirection(step.turnType);
    final isClose =
        distanceToStep != null && distanceToStep! < threshold;

    return Container(
      margin: const EdgeInsets.only(bottom: 4),
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: isCurrent
            ? Colors.green.withValues(alpha: 0.15)
            : Colors.white.withValues(alpha: 0.05),
        border: Border.all(
          color: isCurrent ? Colors.green : Colors.white24,
          width: isCurrent ? 1.5 : 0.5,
        ),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                _directionIcon(direction),
                color: isCurrent ? Colors.green : Colors.white38,
                size: 14,
              ),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  step.description ?? '',
                  style: TextStyle(
                    color: isCurrent ? Colors.white : Colors.white54,
                    fontSize: 9,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),

          // 현재 step만 실시간 거리 표시
          if (isCurrent && distanceToStep != null) ...[
            const SizedBox(height: 4),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${distanceToStep!.toStringAsFixed(1)}m',
                  style: TextStyle(
                    color: isClose ? Colors.green : Colors.orange,
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      outsideThreshold == true ? '진행 중' : '대기 중',
                      style: TextStyle(
                        color: outsideThreshold == true ? Colors.green : Colors.orange,
                        fontSize: 9,
                      ),
                    ),
                    if (minDistanceToStep != double.infinity && outsideThreshold == true)
                      Text(
                        'pass: ${((rawDistance ?? distanceToStep ?? minDistanceToStep) - minDistanceToStep).toStringAsFixed(1)}/8m',
                        style: const TextStyle(color: Colors.cyan, fontSize: 9),
                      ),
                  ],
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}