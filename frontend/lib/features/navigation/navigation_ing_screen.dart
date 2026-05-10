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
import 'package:safepath/features/navigation/navigation_start_overview_card.dart';
import 'package:safepath/models/route_result.dart';
import 'package:safepath/common/widgets/camera_debug_overlay.dart';
import 'package:safepath/features/navigation/navigation_voiceguide_card.dart';
import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/camera_service.dart';
import 'package:safepath/service/detection_ws_service.dart';
import 'package:safepath/models/crosswalk_info.dart';
import 'package:safepath/service/crosswalk_service.dart';
import 'package:safepath/service/navigation_service.dart';
import 'package:safepath/service/navigation_tts_service.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/vibration_service.dart';
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

  static const double _arrivalThresholdMeters = 15;
  static const double _passThroughOffsetMeters = 8;
  bool _hasBeenOutsideThreshold = false;
  double _minDistanceToStep = double.infinity;
  double? _debugDistance;
  bool _hasArrived = false;
  bool _showStartOverview = false;
  bool _hasShownStartOverview = false;
  String? _startDirection;
  String? _destinationDirection;

  // 경로 이탈 감지
  static const double _deviationThresholdMeters = 20.0;
  static const int _deviationCountThreshold = 3;
  int _deviationCount = 0;
  bool _isRecalculating = false;
  int _currentPathIndex = 0;
  double? _lastSegmentDist;

  // 횡단보도 음성신호기 캐시 (step index → CrosswalkInfo?)
  Map<int, CrosswalkInfo?> _crosswalkCache = {};
  int _prefetchGeneration = 0;

  // 장애물 탐지 (카메라 + WS)
  StreamSubscription<DetectionEvent>? _wsSub;
  final List<DetectionEvent> _obstacles = [];
  RouteStatus _routeStatus = RouteStatus.safe;

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
        _startObstacleDetection();
        _routeLoaded = true;
      }
    }
  }

  // ─── 장애물 탐지 시작 ────────────────────────────────────────────────────────

  Future<void> _startObstacleDetection() async {
    await CameraService().start(CameraMode.navigation);
    await DetectionWsService().connect();
    _wsSub = DetectionWsService().eventStream?.listen(_onObstacleEvent);
  }

  void _onObstacleEvent(DetectionEvent event) {
    setState(() {
      _obstacles.insert(0, event);
      if (_obstacles.length > 3) _obstacles.removeLast();
      _routeStatus = _computeRouteStatus();
    });

    VibrationService().vibrate(switch (event.alertLevel) {
      'high' => VibrationEffect.obstacleLevel3,
      'medium' => VibrationEffect.obstacleLevel2,
      _ => VibrationEffect.obstacleLevel1,
    });

    if (event.guideText.isNotEmpty) {
      TtsService().speak(
        event.guideText,
        interrupt: event.alertLevel == 'high',
        channel: TtsChannel.detection,
      );
    }
  }

  RouteStatus _computeRouteStatus() {
    if (_obstacles.any((e) => e.alertLevel == 'high')) return RouteStatus.danger;
    if (_obstacles.any((e) => e.alertLevel == 'medium')) return RouteStatus.warning;
    return RouteStatus.safe;
  }

  String _obstacleText(DetectionEvent event) {
    final particle = _hasTrailingConsonant(event.objectName) ? '이' : '가';
    return '${event.clockDirection} 방향에 ${event.objectName}$particle 있습니다.';
  }

  bool _hasTrailingConsonant(String text) {
    if (text.isEmpty) return false;
    final code = text.codeUnitAt(text.length - 1);
    if (code < 0xAC00 || code > 0xD7A3) return false;
    return (code - 0xAC00) % 28 != 0;
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

    // 경로 데이터로 방향 계산 (GPS 오차 무관)
    if (_routePath.length >= 2) {
      final startBearing = Geolocator.bearingBetween(
        _routePath[0].latitude, _routePath[0].longitude,
        _routePath[1].latitude, _routePath[1].longitude,
      );
      _startDirection = _bearingToDirection(startBearing);

      final destBearing = Geolocator.bearingBetween(
        _routePath[_routePath.length - 2].latitude,
        _routePath[_routePath.length - 2].longitude,
        _routePath[_routePath.length - 1].latitude,
        _routePath[_routePath.length - 1].longitude,
      );
      _destinationDirection = _bearingToDirection(destBearing);
    }

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

    _prefetchCrosswalks();
  }

  Future<void> _prefetchCrosswalks() async {
    final myGeneration = ++_prefetchGeneration;
    for (int i = 0; i < _pointSteps.length; i++) {
      if (!mounted || _prefetchGeneration != myGeneration) return;
      final step = _pointSteps[i];
      if (step.facilityType == 15 &&
          step.latitude != null &&
          step.longitude != null) {
        final info = await CrosswalkService.getNearby(
          lat: step.latitude!,
          lng: step.longitude!,
        );
        if (!mounted || _prefetchGeneration != myGeneration) return;
        _crosswalkCache[i] = info;
        debugPrint(
          '🚦 [Crosswalk] step[$i] → acousticSignal=${info?.acousticSignalInstalled}, summary=${info?.guidanceSummary}',
        );
      }
    }
  }

  void _startLocationTracking() {
    _positionSub = Geolocator.getPositionStream(
      locationSettings: AndroidSettings(
        accuracy: LocationAccuracy.best,
        distanceFilter: 0,
        intervalDuration: Duration.zero,
      ),
    ).listen(_onPositionUpdate);
  }

  void _onPositionUpdate(Position position) {
    if (position.accuracy > 20) return;

    if (_currentStepIndex >= _pointSteps.length) {
      _trackDestination(position);
      if (!_hasArrived && (_debugDistance == null || _debugDistance! >= 50)) {
        _checkDeviation(position);
      }
      return;
    }

    final step = _pointSteps[_currentStepIndex];
    if (step.latitude == null || step.longitude == null) return;

    final distance = Geolocator.distanceBetween(
      position.latitude,
      position.longitude,
      step.latitude!,
      step.longitude!,
    );

    final direction = _turnTypeToDirection(step.turnType);

    if (!_showStartOverview &&
        !_hasBeenOutsideThreshold &&
        !_hasShownStartOverview) {
      _hasShownStartOverview = true;
      setState(() => _showStartOverview = true);
      if (_route != null) {
        NavigationTtsService().speakStartOverview(
          startDirection: _startDirection,
          totalTime: (_route!.totalTime / 60).ceil(),
          firstStepDistance: distance.round(),
          firstStepDirection: direction,
          firstStepInstruction: step.description ?? '',
        );
        // overview 종료 후 step[0] 서버 안내문 자동 재생
        NavigationTtsService().speakAfterOverview(step.description ?? '');
      }
      Future.delayed(const Duration(seconds: 5), () {
        if (mounted) setState(() => _showStartOverview = false);
      });
    }

    // minDist를 먼저 업데이트해야 inPassThroughZone 판단이 정확함
    if (distance < _minDistanceToStep) _minDistanceToStep = distance;

    // 10m 이내 진입 이후 → pass-through zone
    // 이 구간에서는 거리가 다시 증가해도 UI/TTS에 반영하지 않음
    final inPassThroughZone =
        _hasBeenOutsideThreshold && _minDistanceToStep < _arrivalThresholdMeters;

    if (!inPassThroughZone || distance <= (_debugDistance ?? double.infinity)) {
      setState(() => _debugDistance = distance);
    }

    debugPrint(
      '📍 [Nav] step $_currentStepIndex/${_pointSteps.length - 1} | '
      '거리: ${distance.toStringAsFixed(1)}m | '
      '최근접: ${_minDistanceToStep == double.infinity ? "-" : _minDistanceToStep.toStringAsFixed(1)}m | '
      'pass-through: ${_minDistanceToStep < _arrivalThresholdMeters ? "${(distance - _minDistanceToStep).toStringAsFixed(1)}/${_passThroughOffsetMeters}m" : "대기"} | '
      '${_hasBeenOutsideThreshold ? "진행 중" : "대기 중"}',
    );

    // pass-through 완수: 10m 이내 진입 후 최근접 지점에서 8m 이상 멀어지면 완수
    // (distance >= 10m 분기보다 먼저 체크해야 함 — minDist + 8m >= 10m 인 경우를 커버)
    if (inPassThroughZone &&
        distance > _minDistanceToStep + _passThroughOffsetMeters) {
      setState(() {
        _currentStepIndex++;
        _hasBeenOutsideThreshold = false;
        _minDistanceToStep = double.infinity;
        _debugDistance = null;
      });
      _speakNextStepOrDestination();
      _checkDeviation(position);
      return;
    }

    if (distance >= _arrivalThresholdMeters) {
      _hasBeenOutsideThreshold = true;
      // pass-through zone에서 거리가 다시 10m 이상으로 튀어도 TTS 차단
      if (!inPassThroughZone) {
        NavigationTtsService().speakDistance(direction, distance.round(), step.description ?? '');
      }
    } else if (_hasBeenOutsideThreshold) {
      // pass-through zone — 8m 이하 행동 지시 허용 (dedup은 speakDistance 내부에서 처리)
      NavigationTtsService().speakDistance(direction, distance.round(), step.description ?? '');
    } else {
      // 처음부터 _arrivalThresholdMeters 이내 → pass-through zone 즉시 활성화
      _hasBeenOutsideThreshold = true;
    }

    _checkDeviation(position);
  }

  void _trackDestination(Position position) {
    if (_hasArrived ||
        _destinationStep?.latitude == null ||
        _destinationStep?.longitude == null) {
      return;
    }

    final distance = Geolocator.distanceBetween(
      position.latitude,
      position.longitude,
      _destinationStep!.latitude!,
      _destinationStep!.longitude!,
    );

    setState(() => _debugDistance = distance);
    debugPrint('📍 [Nav] 목적지까지: ${distance.toStringAsFixed(1)}m ($_destinationDirection)');

    if (distance < _arrivalThresholdMeters) {
      setState(() => _hasArrived = true);
      NavigationTtsService().speakArrival();
    }
  }

  void _speakNextStepOrDestination() {
    if (_currentStepIndex < _pointSteps.length) {
      final next = _pointSteps[_currentStepIndex];
      final crosswalk = _crosswalkCache[_currentStepIndex];
      final acousticSuffix =
          (crosswalk?.acousticSignalInstalled == true &&
                  crosswalk!.guidanceSummary.isNotEmpty)
              ? ' ${crosswalk.guidanceSummary}'
              : '';
      NavigationTtsService().speakStep(
        _turnTypeToDirection(next.turnType),
        '${next.description ?? ''}$acousticSuffix'.trim(),
      );
    } else {
      // 모든 step 완료 → 목적지 직진 안내
      NavigationTtsService().reset();
      final msg = _destinationDirection != null
          ? '$_destinationDirection 방향으로 직진하세요'
          : '목적지 방향으로 직진하세요';
      TtsService().speak(msg, interrupt: true, channel: TtsChannel.navigation);
    }
  }

  // 점 P에서 선분 AB까지의 최단 거리 (미터)
  double _distanceToSegment(
    double lat, double lng,
    double lat1, double lng1,
    double lat2, double lng2,
  ) {
    const double metersPerDegLat = 111320.0;
    final double metersPerDegLng =
        111320.0 * math.cos(lat * math.pi / 180);

    final double px = (lng - lng1) * metersPerDegLng;
    final double py = (lat - lat1) * metersPerDegLat;
    final double ax = (lng2 - lng1) * metersPerDegLng;
    final double ay = (lat2 - lat1) * metersPerDegLat;

    final double segLenSq = ax * ax + ay * ay;
    if (segLenSq == 0) {
      return Geolocator.distanceBetween(lat, lng, lat1, lng1);
    }

    final double t = math.max(0, math.min(1, (px * ax + py * ay) / segLenSq));
    final double dx = px - ax * t;
    final double dy = py - ay * t;
    return math.sqrt(dx * dx + dy * dy);
  }

  void _checkDeviation(Position position) {
    if (_routePath.length < 2 || _isRecalculating) return;

    double minDist = double.infinity;
    int closestSegIndex = _currentPathIndex;

    for (int i = _currentPathIndex; i < _routePath.length - 1; i++) {
      final d = _distanceToSegment(
        position.latitude, position.longitude,
        _routePath[i].latitude, _routePath[i].longitude,
        _routePath[i + 1].latitude, _routePath[i + 1].longitude,
      );
      if (d < minDist) {
        minDist = d;
        closestSegIndex = i;
      }
    }

    // 경로 인덱스는 앞으로만 전진
    if (closestSegIndex > _currentPathIndex) {
      _currentPathIndex = closestSegIndex;
    }
    _lastSegmentDist = minDist;

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
      final results = await Future.wait([
        NavigationService.getRoute(
          startX: position.longitude,
          startY: position.latitude,
          endX: _endX!,
          endY: _endY!,
          startName: '현재 위치',
          endName: _endName,
        ),
        Future.delayed(const Duration(milliseconds: 800)),
      ]);

      if (!mounted) return;
      final result = results[0] as RouteResult;

      setState(() {
        _route = result;
        _currentStepIndex = 0;
        _hasBeenOutsideThreshold = false;
        _minDistanceToStep = double.infinity;
        _debugDistance = null;
        _hasArrived = false;
        _isRecalculating = false;
        _crosswalkCache = {};
        _currentPathIndex = 0;
      });
      _loadRoute(result);
      // 재탐색 후 overview 카드 없이 새 경로 첫 step 바로 안내
      if (_pointSteps.isNotEmpty) {
        final first = _pointSteps[0];
        NavigationTtsService().speakStep(
          _turnTypeToDirection(first.turnType),
          first.description ?? '',
        );
      }
      debugPrint('✅ [Nav] 경로 재탐색 완료 - 새 step 수: ${_pointSteps.length}');
    } catch (e) {
      debugPrint('🔴 [Nav] 경로 재탐색 실패: $e');
      if (mounted) setState(() => _isRecalculating = false);
    }
  }

  @override
  void dispose() {
    NavigationTtsService().reset();
    _positionSub?.cancel();
    _wsSub?.cancel();
    CameraService().stop();
    DetectionWsService().disconnect();
    TtsService().stop();
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
                            status: _routeStatus,
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
                              VibrationService().vibrate(VibrationEffect.actionStop);
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
                  // ── 우측: 길찾기 안내(고정) + 장애물 목록(스크롤) ──────────
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // 길찾기 안내 카드 — 고정
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 2),
                          child: () {
                            if (_showStartOverview && _route != null) {
                              return NavigationStartOverviewCard(
                                totalDistance: _route!.totalDistance,
                                totalTime: (_route!.totalTime / 60).ceil(),
                                firstStepDistance:
                                    _debugDistance?.round() ?? 0,
                                firstStepDirection: currentStep != null
                                    ? _turnTypeToDirection(
                                        currentStep.turnType,
                                      )
                                    : DirectionType.straight,
                                startDirection: _startDirection,
                              );
                            } else if (currentStep != null) {
                              final crosswalk = _crosswalkCache[_currentStepIndex];
                              final acousticSignal =
                                  (crosswalk?.acousticSignalInstalled == true &&
                                          crosswalk!.guidanceSummary.isNotEmpty)
                                      ? crosswalk.guidanceSummary
                                      : null;
                              return NavigationStepCard(
                                direction: _turnTypeToDirection(
                                  currentStep.turnType,
                                ),
                                instruction: currentStep.description ?? '',
                                distance: _debugDistance?.round() ?? 0,
                                isApproaching: _hasBeenOutsideThreshold,
                                acousticSignal: acousticSignal,
                              );
                            } else if (_hasArrived) {
                              return Center(
                                child: Column(
                                  children: [
                                    Text(
                                      '목적지에 도착했습니다.',
                                      style: AppTextStyles.labelRegular
                                          .copyWith(
                                            color: ColorCollection.point,
                                          ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      '경로 안내를 종료합니다.',
                                      style: AppTextStyles.labelRegular
                                          .copyWith(
                                            color: ColorCollection.point,
                                          ),
                                    ),
                                  ],
                                ),
                              );
                            } else if (_route != null) {
                              return NavigationStepCard(
                                direction: DirectionType.straight,
                                instruction: _destinationDirection != null
                                    ? '$_destinationDirection 방향으로 직진하세요'
                                    : '목적지 방향으로 직진하세요',
                                distance: _debugDistance?.round(),
                                isApproaching: false,
                              );
                            }
                            return const SizedBox.shrink();
                          }(),
                        ),
                        // 장애물 안내 목록 — 스크롤 (최신 순)
                        if (_obstacles.isNotEmpty) ...[
                          const SizedBox(height: 12),
                          Expanded(
                            child: ListView.separated(
                              padding: EdgeInsets.only(
                                left: 2,
                                top: 2,
                                right: 2,
                                bottom: bottomPad,
                              ),
                              itemCount: _obstacles.length,
                              separatorBuilder: (_, __) =>
                                  const SizedBox(height: 10),
                              itemBuilder: (_, index) =>
                                  NavigationVoiceGuideCard(
                                    voiceGuide:
                                        _obstacleText(_obstacles[index]),
                                  ),
                            ),
                          ),
                        ] else
                          SizedBox(height: bottomPad),
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
              minDistanceToStep: _minDistanceToStep,
              segmentDist: _lastSegmentDist,
              deviationCount: _deviationCount,
              pathIndex: _currentPathIndex,
            ),
            const CameraDebugOverlay(anchorLeft: true),
            if (_isRecalculating)
              Positioned(
                top: 0,
                left: 0,
                right: 0,
                child: Container(
                  color: ColorCollection.main.withValues(alpha: 0.9),
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Semantics(
                    label: '경로 재탐색 중',
                    excludeSemantics: true,
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
              ),
          ],
        ),
      ),
    );
  }

  String _bearingToDirection(double bearing) {
    final b = (bearing + 360) % 360;
    if (b < 22.5 || b >= 337.5) return '북쪽';
    if (b < 67.5) return '북동쪽';
    if (b < 112.5) return '동쪽';
    if (b < 157.5) return '남동쪽';
    if (b < 202.5) return '남쪽';
    if (b < 247.5) return '남서쪽';
    if (b < 292.5) return '서쪽';
    return '북서쪽';
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