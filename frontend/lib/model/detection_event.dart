import 'package:safepath/features/detection/obstacle_card_widget.dart';

/// 탐지 이벤트 모델
///
/// 온디바이스(TFLite) 추론 결과를 담는다.
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

  /// 영문 클래스명 → 한국어 이름 (정적 조회)
  static String koName(String className) =>
      _objectClassKo[className] ?? className;

  ObstacleProximity get proximity => switch (distance) {
    'near' => ObstacleProximity.near,
    'mid' => ObstacleProximity.mid,
    _ => ObstacleProximity.far,
  };

  String get distanceLabel => switch (distance) {
    'near' => '가까움',
    'mid' => '중간',
    _ => '멂',
  };

  String get positionLabel => '$clockDirection 방향';

  String get vibrationLabel => switch (alertLevel) {
    'high' => '강한 진동 (연이은 진동)',
    'medium' => '중간 진동 (2회)',
    _ => '약한 진동 (1회)',
  };

  String get objectName => koName(primaryObjectClass);

  static const Map<String, String> _objectClassKo = {
    // 차량류
    'car': '차량',
    'bus': '버스',
    'truck': '트럭',
    'motorcycle': '오토바이',
    'scooter': '스쿠터',
    'bicycle': '자전거',
    // 사람·이동 보조
    'person': '보행자',
    'wheelchair': '휠체어',
    'stroller': '유모차',
    'carrier': '수레',
    // 고정 장애물
    'bollard': '볼라드',
    'pole': '기둥',
    'barricade': '바리케이드',
    'bench': '벤치',
    'table': '탁자',
    'chair': '의자',
    'fire_hydrant': '소화전',
    'tree_trunk': '가로수',
    'potted_plant': '화분',
    'parking_meter': '주차 미터기',
    'kiosk': '키오스크',
    'stop': '정지판',
    // 표지·신호
    'traffic_light': '신호등',
    'traffic_sign': '교통 표지판',
    'traffic_light_controller': '신호 제어기',
    'movable_signage': '이동식 표지판',
    'power_controller': '전력 제어기',
    // 동물
    'dog': '개',
    'cat': '고양이',
    // 기타 (구 COCO 호환)
    'stop_sign': '정지 표지판',
    'backpack': '가방',
    'umbrella': '우산',
    'handbag': '핸드백',
    'suitcase': '여행가방',
    'potted plant': '화분',
    'tv': 'TV',
    'laptop': '노트북',
  };
}
