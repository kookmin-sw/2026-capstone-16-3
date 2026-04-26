import 'dart:async';
import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/route_debug_overlay.dart';
import 'package:safepath/features/navigation/navigation_step_card.dart';
import 'package:safepath/features/navigation/navigation_overview_card.dart';
import 'package:safepath/models/route_result.dart';
import 'package:safepath/service/navigation_service.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'dart:ui' show DisplayFeatureType;
import 'package:flutter/services.dart';

class NavigationIngScreen extends StatefulWidget {
  const NavigationIngScreen({super.key});

  @override
  State<NavigationIngScreen> createState() => _NavigationIngScreenState();
}

class _NavigationIngScreenState extends State<NavigationIngScreen> {
  RouteResult? _route;
  List<RouteStep> _pointSteps = [];
  RouteStep? _destinationStep;
  List<LatLng> _routePath = [];
  int _currentStepIndex = 0;
  StreamSubscription<Position>? _positionSub;
  bool _routeLoaded = false;

  // 목적지 정보 (재탐색 시 사용)
  double? _endX;
  double? _endY;
  String _endName = '';

  static const double _arrivalThresholdMeters = 10;
  bool _hasBeenOutsideThreshold = false;
  double? _debugDistance;

  // 경로 이탈 감지
  static const double _deviationThresholdMeters = 30.0;
  static const int _deviationCountThreshold = 3;
  int _deviationCount = 0;
  bool _isRecalculating = false;

  @override
  void initState() {
    super.initState();
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_routeLoaded) {
      final args = ModalRoute.of(context)?.settings.arguments;
      if (args is Map<String, dynamic>) {
        _route = args['route'] as RouteResult?;
        _endX = args['endX'] as double?;
        _endY = args['endY'] as double?;
        _endName = (args['endName'] as String?) ?? '';
      } else {
        _route = args as RouteResult?;
      }
      if (_route != null) {
        _loadRoute(_route!);
        _startLocationTracking();
        _routeLoaded = true;
      }
    }
  }

  void _loadRoute(RouteResult route) {
    _pointSteps = route.steps
        .where(
          (s) =>
              s.type == 'POINT' && s.pointType != 'SP' && s.pointType != 'EP',
        )
        .toList();
    _destinationStep = route.steps
        .where((s) => s.pointType == 'EP')
        .firstOrNull;
    _routePath = route.steps
        .where((s) => s.type == 'LINE')
        .expand((s) => s.path ?? <LatLng>[])
        .toList();
    debugPrint(
      '📍 [Nav] 전체 step 수: ${_pointSteps.length}, path 좌표 수: ${_routePath.length}',
    );
    for (int i = 0; i < _pointSteps.length; i++) {
      final s = _pointSteps[i];
      debugPrint(
        '  [$i] turnType=${s.turnType} (${s.latitude}, ${s.longitude}) ${s.description}',
      );
    }
    debugPrint(
      '  [목적지] (${_destinationStep?.latitude}, ${_destinationStep?.longitude})',
    );
  }

  void _startLocationTracking() {
    _positionSub = Geolocator.getPositionStream(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        distanceFilter: 5,
      ),
    ).listen(_onPositionUpdate);
  }

  void _onPositionUpdate(Position position) {
    if (_pointSteps.isEmpty || _currentStepIndex >= _pointSteps.length) return;

    final step = _pointSteps[_currentStepIndex];
    if (step.latitude == null || step.longitude == null) return;

    final distance = Geolocator.distanceBetween(
      position.latitude,
      position.longitude,
      step.latitude!,
      step.longitude!,
    );

    setState(() => _debugDistance = distance);
    debugPrint(
      '📍 [Nav] step $_currentStepIndex/${_pointSteps.length - 1} | '
      '남은 거리: ${distance.toStringAsFixed(1)}m | '
      '${_hasBeenOutsideThreshold ? "진행 중" : "대기 중"} | '
      '${_pointSteps[_currentStepIndex].description}',
    );

    if (distance >= _arrivalThresholdMeters) {
      _hasBeenOutsideThreshold = true;
    } else if (_hasBeenOutsideThreshold) {
      setState(() {
        _currentStepIndex++;
        _hasBeenOutsideThreshold = false;
      });
    }

    _checkDeviation(position);
  }

  void _checkDeviation(Position position) {
    if (_routePath.isEmpty || _isRecalculating) return;

    double minDist = double.infinity;
    for (final point in _routePath) {
      final d = Geolocator.distanceBetween(
        position.latitude,
        position.longitude,
        point.latitude,
        point.longitude,
      );
      if (d < minDist) minDist = d;
    }

    if (minDist > _deviationThresholdMeters) {
      _deviationCount++;
      debugPrint(
        '⚠️ [Nav] 경로 이탈: ${minDist.toStringAsFixed(1)}m ($_deviationCount/$_deviationCountThreshold회)',
      );
      if (_deviationCount >= _deviationCountThreshold) {
        _recalculateRoute(position);
      }
    } else {
      if (_deviationCount > 0) _deviationCount = 0;
    }
  }

  Future<void> _recalculateRoute(Position position) async {
    if (_isRecalculating || _endX == null || _endY == null) return;

    setState(() => _isRecalculating = true);
    _deviationCount = 0;
    debugPrint('🔄 [Nav] 경로 재탐색 시작...');

    try {
      final result = await NavigationService.getRoute(
        startX: position.longitude,
        startY: position.latitude,
        endX: _endX!,
        endY: _endY!,
        startName: '현재 위치',
        endName: _endName,
      );

      if (!mounted) return;

      setState(() {
        _route = result;
        _currentStepIndex = 0;
        _hasBeenOutsideThreshold = false;
        _debugDistance = null;
        _isRecalculating = false;
      });
      _loadRoute(result);
      debugPrint('✅ [Nav] 경로 재탐색 완료 - 새 step 수: ${_pointSteps.length}');
    } catch (e) {
      debugPrint('🔴 [Nav] 경로 재탐색 실패: $e');
      if (mounted) setState(() => _isRecalculating = false);
    }
  }

  @override
  void dispose() {
    _positionSub?.cancel();
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
    ]);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final vp = mq.viewPadding;
    final size = mq.size;

    double cutoutLeft = 0, cutoutTop = 0, cutoutRight = 0;
    for (final feature in mq.displayFeatures) {
      if (feature.type == DisplayFeatureType.cutout) {
        final b = feature.bounds;
        if (b.left <= 0) cutoutLeft = math.max(cutoutLeft, b.right);
        if (b.top <= 0) cutoutTop = math.max(cutoutTop, b.bottom);
        if (b.right >= size.width) {
          cutoutRight = math.max(cutoutRight, size.width - b.left);
        }
      }
    }

    final leftPad = math.max(vp.left, cutoutLeft) + 24;
    final topPad = math.max(vp.top, cutoutTop) + 24;
    final rightPad = math.max(vp.right, cutoutRight) + 24;
    final bottomPad = vp.bottom + 24;

    final hasCurrentStep =
        _pointSteps.isNotEmpty && _currentStepIndex < _pointSteps.length;
    final currentStep = hasCurrentStep ? _pointSteps[_currentStepIndex] : null;

    return PopScope(
      canPop: false,
      child: Scaffold(
        body: Stack(
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(leftPad, topPad, rightPad, 0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(
                          child: NavigationOverviewCard(
                            distance: _route?.totalDistance ?? 0,
                            time: _route != null
                                ? (_route!.totalTime / 60).ceil()
                                : 0,
                            status: RouteStatus.safe,
                          ),
                        ),
                        Padding(
                          padding: EdgeInsets.only(top: 16, bottom: bottomPad),
                          child: ActionButton(
                            label: '안내 중지',
                            icon: Icons.stop_circle_outlined,
                            backgroundColor: ColorCollection.red,
                            onTap: () {
                              SoundEffectService().play(SoundEffect.actionStop);
                              SystemChrome.setPreferredOrientations([
                                DeviceOrientation.portraitUp,
                                DeviceOrientation.portraitDown,
                              ]);
                              Navigator.pop(context);
                            },
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 24),
                  Expanded(
                    child: Column(
                      children: [
                        Expanded(
                          child: SingleChildScrollView(
                            padding: const EdgeInsets.symmetric(horizontal: 2),
                            child: Column(
                              children: [
                                if (currentStep != null)
                                  NavigationStepCard(
                                    direction: _turnTypeToDirection(
                                      currentStep.turnType,
                                    ),
                                    instruction: currentStep.description ?? '',
                                    distance: _debugDistance?.round() ?? 0,
                                  )
                                else if (_route != null)
                                  // 모든 step 완료
                                  Center(
                                    child: Text(
                                      '목적지에 도착했습니다!',
                                      style: AppTextStyles.labelRegular
                                          .copyWith(
                                            color: ColorCollection.point,
                                          ),
                                    ),
                                  ),
                                SizedBox(height: bottomPad),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            RouteDebugOverlay(
              pointSteps: _pointSteps,
              currentStepIndex: _currentStepIndex,
              distanceToStep: _debugDistance,
              outsideThreshold: _hasBeenOutsideThreshold,
              threshold: _arrivalThresholdMeters,
            ),
            if (_isRecalculating)
              Positioned(
                top: 0,
                left: 0,
                right: 0,
                child: Container(
                  color: ColorCollection.point.withValues(alpha: 0.9),
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: ColorCollection.point,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Text(
                        '경로 재탐색 중...',
                        style: AppTextStyles.labelBold.copyWith(
                          color: ColorCollection.point,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

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
      case 125: // 육교
      case 126: // 지하보도
        return DirectionType.crosswalk;
      default: // 11 직진, 233 직진 임시, 14 유턴, 기타
        return DirectionType.straight;
    }
  }
}
