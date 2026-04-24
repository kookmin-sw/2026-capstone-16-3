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
  int _currentStepIndex = 0;
  StreamSubscription<Position>? _positionSub;
  bool _routeLoaded = false;

  static const double _arrivalThresholdMeters = 10;
  bool _hasBeenOutsideThreshold = false;
  double? _debugDistance;

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
      _route = ModalRoute.of(context)?.settings.arguments as RouteResult?;
      if (_route != null) {
        _pointSteps = _route!.steps
            .where(
              (s) =>
                  s.type == 'POINT' &&
                  s.pointType != 'SP' &&
                  s.pointType != 'EP',
            )
            .toList();
        _startLocationTracking();
        _routeLoaded = true;
      }
    }
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

    if (distance >= _arrivalThresholdMeters) {
      _hasBeenOutsideThreshold = true;
    } else if (_hasBeenOutsideThreshold) {
      // 멀어졌다가 다시 가까워질 때만 다음 step으로 진행
      setState(() {
        _currentStepIndex++;
        _hasBeenOutsideThreshold = false;
      });
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
                                    distance: _route?.totalDistance ?? 0,
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
