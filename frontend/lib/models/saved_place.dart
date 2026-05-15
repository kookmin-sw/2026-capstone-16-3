import 'package:safepath/common/enum/place_category.dart';

/// 저장된 장소 데이터 처리
class SavedPlace {
  final int id; // 즐겨찾기 ID
  final String label; // 장소 이름
  final String location; // 주소
  final PlaceCategory category; // 카테고리
  final double? lat;
  final double? lng;

  const SavedPlace({
    required this.id,
    required this.label,
    required this.location,
    required this.category,
    this.lat,
    this.lng,
  });

  /// BE 응답 JSON -> Model 변환
  factory SavedPlace.fromJSON(Map<String, dynamic> json) {
    return SavedPlace(
      id: json['id'] as int,
      label: json['alias'] as String,
      location: json['address'] as String,
      category: PlaceCategory.values.firstWhere(
        (e) => e.apiValue == json['category'],
        orElse: () => PlaceCategory.etc,
      ),
      lat: (json['lat'] as num?)?.toDouble(),
      lng: (json['lng'] as num?)?.toDouble(),
    );
  }

  /// Model → JSON 변환 (서버 전송용)
  Map<String, dynamic> toJson() {
    return {'label': label, 'location': location, 'category': category.name};
  }
}
