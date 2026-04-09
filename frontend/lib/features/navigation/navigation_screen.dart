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
import 'package:safepath/data/saved_place_dummy.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;
import 'package:geolocator/geolocator.dart';
import 'package:safepath/service/place_service.dart';

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
  List<Map<String, dynamic>> searchResults = [];
  Timer? _debounce;
  Position? _currentPosition;
  bool isLoading = false;

  @override
  void initState() {
    super.initState();
    speech.initialize();
    destinationController.addListener(() {
      setState(() {});

      if (_debounce?.isActive ?? false) _debounce!.cancel();

      _debounce = Timer(const Duration(milliseconds: 300), () {
        // 🔥 선택 직후에는 검색 다시 안 하도록 방지
        if (_isSelecting) return;
        searchPlaces();
      });
    });

    initCurrentPosition();
    loadLocation();
  }

  Future<void> initCurrentPosition() async {
    try {
      _currentPosition = await getCurrentLocation();
    } catch (e) {
      debugPrint('🔴 위치 초기화 실패: $e');
    }
  }

  /// destinationController의 listener 해제
  @override
  void dispose() {
    destinationController.dispose();
    _debounce?.cancel();
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
      onStatus: (status) => print('status: $status'),
      onError: (error) => print('error: $error'),
    );

    if (!available) {
      print("STT 사용 불가");
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
  void startNavigation() {
    // 키보드 먼저 닫기
    FocusManager.instance.primaryFocus?.unfocus();

    Navigator.pushNamed(context, AppRouter.navigationing).then((_) {
      // 돌아왔을 때 입력값 초기화
      destinationController.clear();
      FocusManager.instance.primaryFocus?.unfocus();
      selectedLocation = "목적지를 선택해 주세요.";
    });
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

    // 현재 위치 가져오기
    return await Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
    );
  }

  /// 현재 위치 주소 가져오기
  void loadLocation() async {
    try {
      final position = await getCurrentLocation();

      final address = await PlaceService.reverseGeocode(
        lat: position.latitude,
        lng: position.longitude,
      );

      setState(() {
        currentLocation = address ?? "현재 위치 로딩 실패";
      });
    } catch (e) {
      setState(() {
        currentLocation = "현재 위치 불러오기 실패";
      });
    }
  }

  /// 장소 검색 함수
  void searchPlaces() async {
    if (destinationController.text.trim().isEmpty) {
      setState(() {
        searchResults.clear();
      });
      return;
    }

    try {
      if (_currentPosition == null) {
        _currentPosition = await getCurrentLocation();
      }

      setState(() => isLoading = true);

      final results = await PlaceService.searchPlaces(
        query: destinationController.text,
        lat: _currentPosition!.latitude,
        lng: _currentPosition!.longitude,
      );

      setState(() {
        searchResults = results;
        isLoading = false;
      });
    } catch (e) {
      debugPrint('🔴 [Place] 장소 검색 에러 : $e');
      setState(() {
        isLoading = false;
      });
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

                        // 검색 중 상태 초기화
                        _isSelecting = false;

                        setState(() {
                          destinationController.clear();
                          selectedLocation = "목적지를 선택해 주세요.";
                          searchResults.clear();
                          isLoading = false;
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

                            if (place != null) {
                              FocusManager.instance.primaryFocus?.unfocus();
                              setState(() {
                                destinationController.text =
                                    (place as dynamic).label;
                                selectedLocation = (place as dynamic).location;
                              });
                            }
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 25),
                    ...savedPlaces.take(2).map((place) {
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 25),
                        child: SavedPlaceWidget(
                          label: place.label,
                          location: place.location,
                          category: place.category,
                          onTap: () {
                            FocusManager.instance.primaryFocus?.unfocus();
                            setState(() {
                              destinationController.text = place.label;
                              selectedLocation = place.location;
                            });
                          },
                        ),
                      );
                    }).toList(),

                    /// 최근 장소 섹션
                    Text(
                      '최근 장소',
                      style: AppTextStyles.title2.copyWith(
                        color: ColorCollection.point,
                        fontSize: 20,
                      ),
                    ),
                    const SizedBox(height: 25),
                    SavedPlaceWidget(
                      label: '회사',
                      location: '서울특별시 성북구 솔샘로98길',
                      onTap: () {
                        FocusManager.instance.primaryFocus?.unfocus();
                        setState(() {
                          destinationController.text = '회사';
                          selectedLocation = '서울특별시 성북구 솔샘로98길';
                        });
                      },
                    ),

                    const SizedBox(height: 25),
                  ],
                ),
              ),

              /// 검색 결과 overlay
              if (destinationController.text.trim().isNotEmpty && !_isSelecting)
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
              if (destinationController.text.trim().isNotEmpty && !_isSelecting)
                Positioned(
                  top: 50,
                  left: 0,
                  right: 0,
                  child: isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : searchResults.isNotEmpty
                      ? ResultList(
                          results: searchResults,
                          onTap: (item) {
                            FocusManager.instance.primaryFocus?.unfocus();

                            _isSelecting = true;

                            setState(() {
                              destinationController.text = item['name'];
                              selectedLocation = item['roadAddress'];
                              searchResults.clear();
                            });

                            // 잠깐 후 다시 검색 가능하도록
                            Future.delayed(
                              const Duration(milliseconds: 300),
                              () {
                                _isSelecting = false;
                              },
                            );
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
