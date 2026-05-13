import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/widgets/permission_onboarding_sheet.dart';
import 'package:safepath/features/detection/detection_screen.dart';
import 'package:safepath/features/home/home_screen.dart';
import 'package:safepath/features/navigation/navigation_screen.dart';
import 'package:safepath/features/settings/settings_screen.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/vibration_service.dart';
import 'package:safepath/service/weather_service.dart';

class MainLayout extends StatefulWidget {
  const MainLayout({super.key});

  @override
  State<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends State<MainLayout> {
  int _currentIndex = 0;
  bool _isDetecting = false;

  final _settingsScrollController = ScrollController();
  final _navigationKey = GlobalKey<NavigationScreenState>();
  final _integratedKey = GlobalKey<NavigationScreenState>();

  late final List<Widget> _pages = [
    HomeScreen(onTabChange: _onTap),
    DetectionScreen(
      onDetectingChanged: (v) => setState(() => _isDetecting = v),
    ),
    NavigationScreen(
      key: _navigationKey,
      withObstacleDetection: false,
      onBeforeNavigationStart: () async =>
          _integratedKey.currentState?.cancelPositionStream(),
    ),
    NavigationScreen(
      key: _integratedKey,
      withObstacleDetection: true,
      onBeforeNavigationStart: () async =>
          _navigationKey.currentState?.cancelPositionStream(),
    ),
    SettingsScreen(scrollController: _settingsScrollController),
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      debugPrint('🌤 [Weather] 온보딩 시작');
      // 최초 로그인 후 1회만 권한 온보딩 표시
      await PermissionOnboardingSheet.showOnce(context);
      debugPrint('🌤 [Weather] 온보딩 완료, mounted: $mounted');
      // 온보딩 완료 후 위치 초기화 (권한 허용 여부와 무관하게 시도 — 내부에서 denied 처리)
      // 앱 시작 시 길찾기 탭만 초기화 — 통합 탭은 탭 전환 시 초기화
      // (두 화면 동시 초기화 시 geolocator 스트림 충돌 발생)
      if (mounted) _navigationKey.currentState?.initLocationIfNeeded();
      _speakWeather();
    });
  }

  Future<void> _speakWeather() async {
    debugPrint('🌤 [Weather] _speakWeather 시작');
    try {
      final permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        debugPrint('🌤 [Weather] 위치 권한 없음 — 스킵');
        return;
      }
      Position? position = await Geolocator.getLastKnownPosition()
          .timeout(const Duration(seconds: 5))
          .catchError((_) => null);
      position ??= await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.low,
      ).timeout(const Duration(seconds: 10));
      debugPrint(
        '🌤 [Weather] 위치: ${position.latitude}, ${position.longitude}',
      );
      final description = await WeatherService().fetchWeatherMessage(
        lat: position.latitude,
        lon: position.longitude,
      );
      debugPrint(
        '🌤 [Weather] description 수신: $description, mounted: $mounted',
      );
      if (description != null && description.isNotEmpty && mounted) {
        debugPrint('🌤 [Weather] TTS 호출');
        await TtsService().speak(
          description,
          interrupt: false,
          channel: TtsChannel.navigation,
        );
      } else {
        debugPrint('🌤 [Weather] TTS 스킵 — description 없음 또는 unmounted');
      }
    } catch (e) {
      debugPrint('🌤 [Weather] 위치 조회 실패: $e');
    }
  }

  @override
  void dispose() {
    _settingsScrollController.dispose();
    super.dispose();
  }

  void _onTap(int index) {
    // 설정 탭으로 진입할 때 스크롤 top으로 리셋
    if (index == 4 && _settingsScrollController.hasClients) {
      _settingsScrollController.jumpTo(0);
    }
    if (index == 2) {
      _navigationKey.currentState?.initLocationIfNeeded();
    }
    if (index == 3) {
      _integratedKey.currentState?.initLocationIfNeeded();
    }
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (_isDetecting) return; // 탐지 중이면 뒤로가기 완전 차단
        if (_currentIndex != 0) {
          setState(() => _currentIndex = 0);
        }
        // 홈 탭에서는 뒤로가기 무시 (로그인으로 안 돌아감)
      },
      child: Scaffold(
        body: IndexedStack(index: _currentIndex, children: _pages),
        bottomNavigationBar: _isDetecting
            ? null
            : Container(
                decoration: const BoxDecoration(
                  border: Border(
                    top: BorderSide(color: ColorCollection.point, width: 1.0),
                  ),
                ),
                child: BottomNavigationBar(
                  currentIndex: _currentIndex,
                  onTap: (index) {
                    SoundEffectService().play(SoundEffect.buttonTap);
                    VibrationService().vibrate(VibrationEffect.buttonTap);
                    _onTap(index);
                  },
                  type: BottomNavigationBarType.fixed,
                  backgroundColor: ColorCollection.background,
                  selectedItemColor: ColorCollection.main,
                  selectedLabelStyle: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    fontFamily: 'NanumSquareNeo',
                  ),
                  unselectedLabelStyle: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    fontFamily: 'NanumSquareNeo',
                  ),
                  unselectedItemColor: ColorCollection.point,
                  items: const [
                    BottomNavigationBarItem(icon: Icon(Icons.home), label: '홈'),
                    BottomNavigationBarItem(
                      icon: Icon(Icons.remove_red_eye_outlined),
                      label: '탐지',
                    ),
                    BottomNavigationBarItem(
                      icon: Icon(Icons.navigation),
                      label: '길찾기',
                    ),
                    BottomNavigationBarItem(
                      icon: Icon(Icons.directions_walk_rounded),
                      label: '통합',
                    ),
                    BottomNavigationBarItem(
                      icon: Icon(Icons.settings),
                      label: '설정',
                    ),
                  ],
                ),
              ),
      ),
    );
  }
}
