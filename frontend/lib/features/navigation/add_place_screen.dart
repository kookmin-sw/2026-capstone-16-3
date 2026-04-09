import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'dart:async';
import 'package:safepath/common/enum/place_category.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/navigation/address_section.dart';
import 'package:safepath/features/navigation/category_section.dart';
import 'package:safepath/features/navigation/place_name_section.dart';
import 'package:safepath/features/navigation/navigation_result_list.dart';
import 'package:safepath/models/saved_place.dart';
import 'package:safepath/service/place_service.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

class AddPlaceScreen extends StatefulWidget {
  const AddPlaceScreen({super.key});

  @override
  State<AddPlaceScreen> createState() => _AddPlaceScreenState();
}

class _AddPlaceScreenState extends State<AddPlaceScreen> {
  final TextEditingController destinationController = TextEditingController();
  final TextEditingController placeNameController = TextEditingController();
  final stt.SpeechToText speech = stt.SpeechToText();

  /// 텍스트 입력 여부 확인
  bool get _hasDestination => destinationController.text.trim().isNotEmpty;
  bool get _hasPlaceName => placeNameController.text.trim().isNotEmpty;

  /// 위치 변수
  String currentLocation = "위치를 검색하세요.";
  String? selectedPlaceId;
  double? latitude;
  double? longitude;
  Position? _currentPosition;

  /// 검색 관련 상태
  List<Map<String, dynamic>> searchResults = [];
  bool isLoading = false;
  bool _isSelecting = false;
  Timer? _debounce;

  /// 선택된 카테고리
  PlaceCategory? selectedCategory;

  @override
  void initState() {
    super.initState();
    speech.initialize();
    initCurrentLocation();
    destinationController.addListener(() {
      _onTextChanged();

      if (_debounce?.isActive ?? false) _debounce!.cancel();

      _debounce = Timer(const Duration(milliseconds: 300), () {
        // 선택 직후에는 검색 다시 안 하도록 방지
        if (_isSelecting) return;
        searchPlaces(destinationController.text);
      });
    });
    placeNameController.addListener(_onTextChanged);
  }

  void _onTextChanged() {
    setState(() {});
  }

  /// 현재 위치 가져오기
  Future<void> initCurrentLocation() async {
    try {
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      _currentPosition = position;
    } catch (e) {
      debugPrint('🔴 위치 가져오기 실패: $e');
    }
  }

  /// 장소 검색
  Future<void> searchPlaces(String query) async {
    if (query.trim().isEmpty) {
      setState(() {
        searchResults.clear();
      });
      return;
    }

    try {
      setState(() => isLoading = true);

      final results = await PlaceService.searchPlaces(
        query: query,
        lat: _currentPosition?.latitude ?? 37.5665,
        lng: _currentPosition?.longitude ?? 126.9780,
      );

      setState(() {
        searchResults = results;
        isLoading = false;
      });
    } catch (e) {
      debugPrint('🔴 장소 검색 에러: $e');
      setState(() {
        isLoading = false;
      });
    }
  }

  @override
  void dispose() {
    destinationController.removeListener(_onTextChanged);
    placeNameController.removeListener(_onTextChanged);
    _debounce?.cancel();
    destinationController.dispose();
    placeNameController.dispose();
    super.dispose();
  }

  /// 음성 텍스트 입력 함수
  void startSpeechInput(TextEditingController controller) async {
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
            controller.text = result.recognizedWords;
          });
        }
      },
    );
  }

  /// 장소 추가 함수
  Future<void> addPlace() async {
    if (selectedCategory == null ||
        selectedPlaceId == null ||
        latitude == null ||
        longitude == null)
      return;

    final success = await PlaceService.addFavorite(
      placeId: selectedPlaceId!,
      name: destinationController.text,
      alias: placeNameController.text,
      address: currentLocation,
      latitude: latitude!,
      longitude: longitude!,
    );

    if (success) {
      Navigator.pop(context, true);
    } else {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('즐겨찾기 추가 실패')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '장소 추가하기', showBackButton: true),
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: Stack(
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: SingleChildScrollView(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          AddressSection(
                            controller: destinationController,
                            currentLocation: currentLocation,
                            onSubmitted: searchPlaces,
                            onClear: () {
                              FocusManager.instance.primaryFocus?.unfocus();

                              // 검색 중 상태 초기화
                              _isSelecting = false;

                              setState(() {
                                destinationController.clear();
                                currentLocation = "위치를 검색하세요.";
                                searchResults.clear();
                                isLoading = false;
                              });
                            },
                            onMicTap: () =>
                                startSpeechInput(destinationController),
                          ),
                          const SizedBox(height: 30),
                          PlaceNameSection(
                            controller: placeNameController,
                            onMicTap: () =>
                                startSpeechInput(placeNameController),
                          ),
                          const SizedBox(height: 30),
                          CategorySection(
                            selectedCategory: selectedCategory,
                            onSelected: (category) {
                              setState(() {
                                if (selectedCategory == category) {
                                  selectedCategory = null; // 다시 누르면 해제
                                } else {
                                  selectedCategory = category; // 선택
                                }
                              });
                            },
                          ),
                        ],
                      ),
                    ),
                  ),
                  SafeArea(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 16, bottom: 24),
                      child: ActionButton(
                        label: '추가하기',
                        icon: Icons.check_circle_outline_rounded,
                        onTap:
                            (_hasDestination &&
                                _hasPlaceName &&
                                selectedCategory != null &&
                                selectedPlaceId != null)
                            ? addPlace
                            : null,
                      ),
                    ),
                  ),
                ],
              ),

              /// 검색 결과 overlay
              if (destinationController.text.trim().isNotEmpty && !_isSelecting)
                Positioned(
                  top: 90,
                  left: 0,
                  right: 0,
                  child: isLoading
                      ? const Padding(
                          padding: EdgeInsets.all(16.0),
                          child: Center(child: CircularProgressIndicator()),
                        )
                      : searchResults.isNotEmpty
                      ? ResultList(
                          results: searchResults,
                          onTap: (item) {
                            FocusManager.instance.primaryFocus?.unfocus();

                            _isSelecting = true;

                            setState(() {
                              destinationController.text = item['name'] ?? '';
                              currentLocation = item['roadAddress'] ?? '';

                              selectedPlaceId = item['id'];
                              latitude = item['latitude'];
                              longitude = item['longitude'];

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
                          padding: const EdgeInsets.all(12),
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
                              '검색 결과가 없습니다',
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
