import 'package:safepath/features/detection/obstacle_card_widget.dart';

/// 서버 WebSocket으로부터 수신하는 탐지 이벤트
///
/// 수신 형식:
/// {
///   "status": "success",
///   "guideText": "1시 방향에 오토바이가 있습니다. 중앙을 유지하며 천천히 직진하세요.",
///   "primaryObjectClass": "motorcycle",
///   "clockDirection": "1시",
///   "distance": "near" | "mid" | "far",
///   "alertLevel": "high" | "medium" | "low"
/// }
class DetectionEvent {
  final String guideText;
  final String primaryObjectClass;
  final String clockDirection;
  final String distance; // "near" | "mid" | "far"
  final String alertLevel; // "high" | "medium" | "low"

  const DetectionEvent({
    required this.guideText,
    required this.primaryObjectClass,
    required this.clockDirection,
    required this.distance,
    required this.alertLevel,
  });

  factory DetectionEvent.fromJson(Map<String, dynamic> json) {
    return DetectionEvent(
      guideText: json['guideText'] as String? ?? '',
      primaryObjectClass: json['primaryObjectClass'] as String? ?? '',
      clockDirection: json['clockDirection'] as String? ?? '',
      distance: json['distance'] as String? ?? 'far',
      alertLevel: json['alertLevel'] as String? ?? 'low',
    );
  }

  /// distance 문자열 → ObstacleProximity
  ObstacleProximity get proximity => switch (distance) {
    'near' => ObstacleProximity.near,
    'mid' => ObstacleProximity.mid,
    _ => ObstacleProximity.far,
  };

  /// distance → 한국어 표시
  String get distanceLabel => switch (distance) {
    'near' => '가까움',
    'mid' => '중간',
    _ => '멂',
  };

  /// clockDirection → "N시 방향" 표시
  String get positionLabel => '$clockDirection 방향';

  /// alertLevel → 진동 피드백 설명
  String get vibrationLabel => switch (alertLevel) {
    'high' => '강한 진동 (연이은 진동)',
    'medium' => '중간 진동 (2회)',
    _ => '약한 진동 (1회)',
  };

  /// primaryObjectClass(영문) → 한국어 이름
  String get objectName =>
      _objectClassKo[primaryObjectClass] ?? primaryObjectClass;

  static const Map<String, String> _objectClassKo = {
    'motorcycle': '오토바이',
    'bicycle': '자전거',
    'person': '사람',
    'car': '자동차',
    'truck': '트럭',
    'bus': '버스',
    'traffic light': '신호등',
    'fire hydrant': '소화전',
    'stop sign': '정지 표지판',
    'bench': '벤치',
    'dog': '개',
    'cat': '고양이',
    'backpack': '가방',
    'umbrella': '우산',
    'handbag': '핸드백',
    'suitcase': '여행가방',
    'chair': '의자',
    'potted plant': '화분',
    'tv': 'TV',
    'laptop': '노트북',
  };
}
