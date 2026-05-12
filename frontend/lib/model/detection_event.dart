import 'package:safepath/features/detection/obstacle_card_widget.dart';

/// 서버 WebSocket으로부터 수신하는 탐지 이벤트
///
/// 수신 형식:
/// {
///   "primaryObjectId": "string",   ← 장애물 식별자 (위젯 생명주기 관리 키)
///   "status": "success" | "exit",  ← "exit" 이면 해당 위젯 제거
///   "guideText": "string",
///   "primaryObjectClass": "string",
///   "clockDirection": "string",
///   "distance": "near" | "mid" | "far",
///   "alertLevel": "high" | "medium" | "low"
/// }
class DetectionEvent {
  final String primaryObjectId;
  final String status;
  final String guideText;
  final String primaryObjectClass;
  final String clockDirection;
  final String distance; // "near" | "mid" | "far"
  final String alertLevel; // "high" | "medium" | "low"

  const DetectionEvent({
    required this.primaryObjectId,
    required this.status,
    required this.guideText,
    required this.primaryObjectClass,
    required this.clockDirection,
    required this.distance,
    required this.alertLevel,
  });

  /// 활성 탐지 이벤트 여부 (false 이면 해당 primaryObjectId 위젯 제거)
  bool get isActive => status == 'success';

  factory DetectionEvent.fromJson(Map<String, dynamic> json) {
    return DetectionEvent(
      primaryObjectId:
          (json['primaryObjectId'] ?? json['primary_object_id'] ?? '')
              as String,
      status: (json['status'] ?? '') as String,
      guideText: (json['guideText'] ?? json['guide_text'] ?? '') as String,
      primaryObjectClass:
          (json['primaryObjectClass'] ?? json['primary_object_class'] ?? '')
              as String,
      clockDirection:
          (json['clockDirection'] ?? json['clock_direction'] ?? '') as String,
      distance: (json['distance'] ?? 'far') as String,
      alertLevel:
          (json['alertLevel'] ?? json['alert_level'] ?? 'low') as String,
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
    "wheelchair": "휠체어",
    "truck": "트럭",
    "tree_trunk": "나무",
    "traffic_sign": "교통표지판",
    "traffic_light": "신호등",
    "traffic_light_controller": "신호제어기",
    "table": "탁자",
    "stroller": "유모차",
    "stop": "정지표지판",
    "scooter": "스쿠터",
    "potted_plant": "화분",
    "power_controller": "전력제어함",
    "pole": "기둥",
    "person": "사람",
    "parking_meter": "주차정산기",
    "movable_signage": "이동식 안내표지",
    "motorcycle": "오토바이",
    "motorbike": "오토바이",
    "kiosk": "키오스크",
    "fire_hydrant": "소화전",
    "dog": "개",
    "chair": "의자",
    "cat": "고양이",
    "carrier": "운반수레",
    "car": "차량",
    "vehicle": "차량",
    "bus": "버스",
    "bollard": "볼라드",
    "bicycle": "자전거",
    "bike": "자전거",
    "bench": "벤치",
    "barricade": "바리케이드",
    "crosswalk": "횡단보도",
    "tactile_block": "점자블록 끊김",
    "unknown": "장애물",
  };
}
