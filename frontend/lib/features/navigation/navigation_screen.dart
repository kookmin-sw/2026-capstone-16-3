import 'package:flutter/material.dart';
import 'dart:async';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/text_input_bar.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/navigation/current_place_widget.dart';
import 'package:safepath/features/navigation/more_button.dart';
import 'package:safepath/features/navigation/navigation_result_list.dart';
import 'package:safepath/features/navigation/saved_place_widget.dart';
import 'package:safepath/models/saved_place.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:geolocator/geolocator.dart';
import 'package:safepath/service/place_service.dart';
import 'package:safepath/service/navigation_service.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/vibration_service.dart';

class NavigationScreen extends StatefulWidget {
  const NavigationScreen({super.key});

  @override
  State<NavigationScreen> createState() => _NavigationScreenState();
}

class _NavigationScreenState extends State<NavigationScreen> {
  bool _isSelecting = false;
  final TextEditingController destinationController = TextEditingController();
  final stt.SpeechToText speech = stt.SpeechToText();

  /// 텍스트 입력 여부 확인
  bool get _hasDestination => destinationController.text.trim().isNotEmpty;

  /// 위치 변수
  String currentLocation = "현재 위치 불러오는 중...";
  String selectedLocation = "목적지를 선택해 주세요.";
  double? _selectedLat;
  double? _selectedLng;
  String _selectedName = '';
  List<Map<String, dynamic>> searchResults = [];
  bool _showSearchOverlay = false;
  bool _hasNext = false;
  int _currentPage = 1;
  bool _isLoadingMore = false;
  Timer? _debounce;
  Timer? _reverseGeocodeRetry;
  StreamSubscription<Position>? _positionStream;
  Position? _currentPosition;
  bool isLoading = false;
  List<SavedPlace> _savedPlaces = [];
  List<Map<String, dynamic>> _recentPlaces = [];

  @override
  void initState() {
    super.initState();
    speech.initialize();
    _loadSavedPlaces();
    _loadRecentPlaces();
    destinationController.addListener(() {
      setState(() {});

      if (_debounce?.isActive ?? false) _debounce!.cancel();

      _debounce = Timer(const Duration(milliseconds: 300), () {
        if (_isSelecting) return;
        searchPlaces();
      });
    });

    _initLocation();
  }

  Future<void> _loadRecentPlaces() async {
    try {
      final result = await PlaceService.getRecentPlaces();
      setState(() {
        _recentPlaces = List<Map<String, dynamic>>.from(result['items']);
      });
    } catch (e) {
      debugPrint('🔴 [Navigation] 최근 장소 로드 에러: $e');
    }
  }

  Future<void> _loadSavedPlaces() async {
    try {
      final result = await PlaceService.getFavorites();
      final items = (result['items'] as List<Map<String, dynamic>>)
          .map((item) => SavedPlace.fromJSON(item))
          .toList();
      setState(() => _savedPlaces = items);
    } catch (e) {
      debugPrint('🔴 [Navigation] 저장된 장소 로드 에러: $e');
    }
  }

  Future<void> _initLocation() async {
    try {
      final position = await getCurrentLocation();
      if (!mounted) return;
      _currentPosition = position;
      await _fetchAddress(position);

      // 초기 위치 획득 후 20m 이상 이동 시 위치·주소 갱신
      _positionStream?.cancel();
      _positionStream = Geolocator.getPositionStream(
        locationSettings: AndroidSettings(
          accuracy: LocationAccuracy.best,
          distanceFilter: 20,
        ),
      ).listen((pos) {
        if (!mounted) return;
        _currentPosition = pos;
        _fetchAddress(pos);
      });
    } catch (e) {
      debugPrint('🔴 위치 초기화 실패: $e');
      if (mounted) setState(() => currentLocation = '현재 위치 불러오는 중...');
    }
  }

  Future<void> _fetchAddress(Position position) async {
    _reverseGeocodeRetry?.cancel();

    final result = await PlaceService.reverseGeocode(
      lat: position.latitude,
      lng: position.longitude,
    );
    if (!mounted) return;

    if (_applyGeocodeResult(result)) return;

    // exactAddress 미획득 → 5초마다 재시도
    _reverseGeocodeRetry = Timer.periodic(const Duration(seconds: 5), (_) async {
      final retryResult = await PlaceService.reverseGeocode(
        lat: _currentPosition?.latitude ?? position.latitude,
        lng: _currentPosition?.longitude ?? position.longitude,
      );
      if (!mounted) {
        _reverseGeocodeRetry?.cancel();
        return;
      }
      if (_applyGeocodeResult(retryResult)) {
        _reverseGeocodeRetry?.cancel();
      }
    });
  }

  /// 결과 적용 후 retry 종료 여부 반환
  bool _applyGeocodeResult(ReverseGeocodeResult? result) {
    if (result == null) return false;

    if (result.source == ReverseGeocodeSource.exactAddress) {
      setState(() => currentLocation = result.address);
      return true;
    }

    if (result.source == ReverseGeocodeSource.nearestPlace) {
      setState(() => currentLocation = result.address);
    }

    // nearestPlace는 표시하되 exactAddress를 받을 때까지 retry 유지
    // regionAddress → 표시 안 하고 계속 retry
    return false;
  }

  /// destinationController의 listener 해제
  @override
  void dispose() {
    destinationController.dispose();
    _debounce?.cancel();
    _reverseGeocodeRetry?.cancel();
    _positionStream?.cancel();
    super.dispose();
  }

  /// 음성 텍스트 입력 함수
  void startSpeechInput() async {
    // 음성 인식 중복 실행 방지 - 다시 한 번 더 누르면 중지
    if (speech.isListening) {
      await speech.stop();
      return;
    }

    bool available = await speech.initialize(
      onStatus: (status) => debugPrint('status: $status'),
      onError: (error) => debugPrint('error: $error'),
    );

    if (!available) {
      debugPrint("STT 사용 불가");
      return;
    }

    speech.listen(
      listenOptions: stt.SpeechListenOptions(
        listenMode: stt.ListenMode.dictation, // 문장 단위 인식
        partialResults: true, // 중간 결과 허용
        cancelOnError: true,
      ),
      pauseFor: const Duration(seconds: 4), // 4초 동안 말이 없으면 자동으로 듣기 종료
      listenFor: const Duration(seconds: 20), // 최대 듣기 시간
      localeId: "ko_KR",
      onResult: (result) {
        if (result.recognizedWords.isNotEmpty) {
          setState(() {
            destinationController.text = result.recognizedWords;
          });
        }
      },
    );
  }

  /// 길찾기 시작 함수
  void startNavigation() async {
    if (_currentPosition == null ||
        _selectedLat == null ||
        _selectedLng == null) {
      return;
    }

    FocusManager.instance.primaryFocus?.unfocus();
    SoundEffectService().play(SoundEffect.actionStart);
    VibrationService().vibrate(VibrationEffect.actionStart);

    _positionStream?.cancel();
    setState(() => isLoading = true);

    try {
      final result = await NavigationService.getRoute(
        startX: _currentPosition!.longitude,
        startY: _currentPosition!.latitude,
        endX: _selectedLng!,
        endY: _selectedLat!,
        startName: currentLocation,
        endName: _selectedName,
      );

      if (!mounted) return;

      await Navigator.pushNamed(
        context,
        AppRouter.navigationing,
        arguments: {
          'route': result,
          'endX': _selectedLng!,
          'endY': _selectedLat!,
          'endName': _selectedName,
        },
      );

      // 돌아왔을 때 입력값 초기화 및 위치 갱신
      destinationController.clear();
      FocusManager.instance.primaryFocus?.unfocus();
      setState(() {
        selectedLocation = "목적지를 선택해 주세요.";
        _selectedLat = null;
        _selectedLng = null;
        _selectedName = '';
        currentLocation = "현재 위치 불러오는 중...";
      });
      _initLocation();
    } catch (e) {
      debugPrint('🔴 [Navigation] 길찾기 오류: $e');
    } finally {
      if (mounted) setState(() => isLoading = false);
    }
  }

  /// 현재 위치 위도,경도 가져오기
  Future<Position> getCurrentLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    // 위치 서비스 활성화 확인
    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      throw Exception('위치 서비스가 꺼져 있습니다.');
    }

    // 권한 확인
    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }

    if (permission == LocationPermission.denied) {
      throw Exception('위치 권한이 거부되었습니다.');
    }

    if (permission == LocationPermission.deniedForever) {
      throw Exception('위치 권한이 영구적으로 거부되었습니다.');
    }

    // 정확도 20m 이하인 첫 번째 위치 반환 (최대 10초 대기, 초과 시 폴백)
    try {
      return await Geolocator.getPositionStream(
        locationSettings: AndroidSettings(
          accuracy: LocationAccuracy.best,
          distanceFilter: 0,
        ),
      ).firstWhere((p) => p.accuracy <= 20).timeout(const Duration(seconds: 10));
    } catch (_) {
      final last = await Geolocator.getLastKnownPosition();
      if (last != null) return last;
      return await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.best,
      );
    }
  }


  /// 장소 검색 함수 (새 쿼리 입력 시 초기화)
  void searchPlaces() async {
    if (destinationController.text.trim().isEmpty) {
      setState(() {
        searchResults.clear();
        _showSearchOverlay = false;
        _hasNext = false;
        _currentPage = 1;
      });
      return;
    }

    try {
      if (_currentPosition == null) {
        _currentPosition = await getCurrentLocation();
      }

      setState(() {
        isLoading = true;
        _showSearchOverlay = true;
        _currentPage = 1;
      });

      final result = await PlaceService.searchPlaces(
        query: destinationController.text,
        lat: _currentPosition!.latitude,
        lng: _currentPosition!.longitude,
        page: 1,
      );

      setState(() {
        searchResults = List<Map<String, dynamic>>.from(result['items']);
        _hasNext = result['hasNext'] as bool;
        isLoading = false;
      });
    } catch (e) {
      debugPrint('🔴 [Place] 장소 검색 에러 : $e');
      setState(() {
        isLoading = false;
      });
    }
  }

  /// 추가 페이지 로드 (무한 스크롤)
  Future<void> _loadMorePlaces() async {
    if (!_hasNext || _isLoadingMore) return;

    try {
      setState(() => _isLoadingMore = true);

      final nextPage = _currentPage + 1;
      final result = await PlaceService.searchPlaces(
        query: destinationController.text,
        lat: _currentPosition!.latitude,
        lng: _currentPosition!.longitude,
        page: nextPage,
      );

      setState(() {
        searchResults.addAll(List<Map<String, dynamic>>.from(result['items']));
        _hasNext = result['hasNext'] as bool;
        _currentPage = nextPage;
        _isLoadingMore = false;
      });
    } catch (e) {
      debugPrint('🔴 [Place] 추가 장소 로드 에러: $e');
      setState(() => _isLoadingMore = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '길찾기'),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: Stack(
            children: [
              SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    /// 목적지 입력 섹션
                    TextInputBar(
                      controller: destinationController,
                      onClear: () {
                        FocusManager.instance.primaryFocus?.unfocus();
                        _isSelecting = false;
                        _debounce?.cancel();

                        setState(() {
                          destinationController.clear();
                          selectedLocation = "목적지를 선택해 주세요.";
                          searchResults.clear();
                          _showSearchOverlay = false;
                          isLoading = false;
                          _hasNext = false;
                          _currentPage = 1;
                        });
                      },
                      hintText: '목적지를 입력하세요.',
                      micTap: startSpeechInput,
                      onSubmitted: (_) {
                        searchPlaces();
                      },
                    ),
                    const SizedBox(height: 16),
                    CurrentPlaceWidget(
                      label: '현재 위치',
                      location: currentLocation,
                    ),
                    const SizedBox(height: 16),
                    CurrentPlaceWidget(
                      label: '목적지 위치',
                      location: selectedLocation,
                    ),
                    const SizedBox(height: 16),

                    ActionButton(
                      label: '안내 시작',
                      icon: Icons.play_circle_outline_outlined,
                      onTap: _hasDestination ? startNavigation : null,
                    ),
                    const SizedBox(height: 30),

                    /// 저장된 장소 섹션
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '저장된 장소',
                          style: AppTextStyles.title2.copyWith(
                            color: ColorCollection.point,
                            fontSize: 20,
                          ),
                        ),
                        MoreButton(
                          onTap: () async {
                            final place = await Navigator.pushNamed(
                              context,
                              AppRouter.savedplace,
                            );

                            _loadSavedPlaces();
                            if (place != null) {
                              FocusManager.instance.primaryFocus?.unfocus();
                              _isSelecting = true;
                              final p = place as SavedPlace;
                              setState(() {
                                destinationController.text = p.label;
                                selectedLocation = p.location;
                                _selectedName = p.label;
                                _selectedLat = p.lat;
                                _selectedLng = p.lng;
                                _showSearchOverlay = false;
                              });
                              _debounce?.cancel();
                              _isSelecting = false;
                            }
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 25),
                    if (_savedPlaces.isEmpty)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 25),
                        child: Center(
                          child: Text(
                            '저장된 장소가 없습니다',
                            style: AppTextStyles.labelRegular.copyWith(
                              color: ColorCollection.point,
                            ),
                          ),
                        ),
                      )
                    else
                      ..._savedPlaces.take(2).map((place) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 25),
                          child: SavedPlaceWidget(
                            label: place.label,
                            location: place.location,
                            category: place.category,
                            onTap: () {
                              FocusManager.instance.primaryFocus?.unfocus();
                              _isSelecting = true;
                              setState(() {
                                destinationController.text = place.label;
                                selectedLocation = place.location;
                                _selectedName = place.label;
                                _selectedLat = place.lat;
                                _selectedLng = place.lng;
                                _showSearchOverlay = false;
                              });
                              _debounce?.cancel();
                              _isSelecting = false;
                            },
                          ),
                        );
                      }),

                    /// 최근 장소 섹션
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        Text(
                          '최근 장소',
                          style: AppTextStyles.title2.copyWith(
                            color: ColorCollection.point,
                            fontSize: 20,
                          ),
                        ),
                        if (_recentPlaces.isNotEmpty)
                          _ClearButton(
                            onTap: () async {
                              final success =
                                  await PlaceService.deleteAllRecentPlaces();
                              if (success) {
                                setState(() => _recentPlaces.clear());
                              }
                            },
                          ),
                      ],
                    ),
                    const SizedBox(height: 25),
                    if (_recentPlaces.isEmpty)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 25),
                        child: Center(
                          child: Text(
                            '최근 검색한 장소가 없습니다',
                            style: AppTextStyles.labelRegular.copyWith(
                              color: ColorCollection.point,
                            ),
                          ),
                        ),
                      )
                    else
                      ..._recentPlaces.take(4).map((place) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 25),
                          child: SavedPlaceWidget(
                            label: place['name'] as String,
                            location: place['address'] as String,
                            onTap: () {
                              FocusManager.instance.primaryFocus?.unfocus();
                              _isSelecting = true;
                              setState(() {
                                destinationController.text =
                                    place['name'] as String;
                                selectedLocation = place['address'] as String;
                                _selectedName = place['name'] as String;
                                _selectedLat = (place['lat'] as num?)
                                    ?.toDouble();
                                _selectedLng = (place['lng'] as num?)
                                    ?.toDouble();
                                _showSearchOverlay = false;
                              });
                              _debounce?.cancel();
                              _isSelecting = false;
                            },
                          ),
                        );
                      }),
                  ],
                ),
              ),

              /// 검색 결과 overlay
              if (_showSearchOverlay && !_isSelecting)
                Positioned(
                  top: 60,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  child: GestureDetector(
                    onTap: () {
                      FocusManager.instance.primaryFocus?.unfocus();
                      setState(() {
                        searchResults.clear();
                      });
                    },
                    child: Container(color: Colors.transparent),
                  ),
                ),
              if (_showSearchOverlay && !_isSelecting)
                Positioned(
                  top: 50,
                  left: 0,
                  right: 0,
                  child: isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : searchResults.isNotEmpty
                      ? ResultList(
                          results: searchResults,
                          isLoadingMore: _isLoadingMore,
                          onLoadMore: _loadMorePlaces,
                          onTap: (item) {
                            FocusManager.instance.primaryFocus?.unfocus();
                            _isSelecting = true;

                            setState(() {
                              destinationController.text = item['name'];
                              selectedLocation = item['roadAddress'];
                              _selectedName = item['name'] as String;
                              _selectedLat = item['latitude'] as double?;
                              _selectedLng = item['longitude'] as double?;
                              searchResults.clear();
                              _showSearchOverlay = false;
                            });

                            _debounce?.cancel();
                            _isSelecting = false;

                            PlaceService.getPlaceDetail(
                              placeId: item['id'],
                            ).then((_) => _loadRecentPlaces());
                          },
                        )
                      : Container(
                          margin: const EdgeInsets.only(top: 8),
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: ColorCollection.background,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: ColorCollection.point,
                              width: 2,
                            ),
                          ),
                          child: Center(
                            child: Text(
                              "검색 결과가 없습니다",
                              style: AppTextStyles.labelRegular.copyWith(
                                color: ColorCollection.point,
                              ),
                            ),
                          ),
                        ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ClearButton extends StatelessWidget {
  final VoidCallback onTap;
  const _ClearButton({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(10),
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: () {
          SoundEffectService().play(SoundEffect.buttonTap);
          VibrationService().vibrate(VibrationEffect.buttonTap);
          onTap();
        },
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(
            color: ColorCollection.point.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: ColorCollection.point, width: 1),
          ),
          child: Row(
            children: [
              Icon(
                Icons.delete_outline_rounded,
                size: 18,
                color: ColorCollection.point,
              ),
              const SizedBox(width: 4),
              Text(
                '전체 삭제',
                style: AppTextStyles.labelRegular.copyWith(
                  color: ColorCollection.point,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
