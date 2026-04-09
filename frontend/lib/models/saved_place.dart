import 'package:safepath/common/enum/place_category.dart';

/// 저장된 장소 데이터 처리
class SavedPlace {
  final String label; // 장소 이름
  final String location; // 주소
  final PlaceCategory category; // 카테고리

  const SavedPlace({
    required this.label,
    required this.location,
    required this.category,
  });

  /// JSON -> Model(SavedPlace 객체) 변환
  factory SavedPlace.fromJSON(Map<String, dynamic> json) {
    return SavedPlace(
      label: json['label'],
      location: json['location'],
      category: PlaceCategory.values.firstWhere(
        (e) => e.name == json['category'],
        orElse: () => PlaceCategory.etc,
      ),
    );
  }

  /// Model → JSON 변환 (서버 전송용)
  Map<String, dynamic> toJson() {
    return {'label': label, 'location': location, 'category': category.name};
  }
}
