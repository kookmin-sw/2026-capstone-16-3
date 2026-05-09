class RouteResult {
  final int totalDistance; // 총 거리 (미터)
  final int totalTime; // 총 소요 시간 (초)
  final List<RouteStep> steps;

  const RouteResult({
    required this.totalDistance,
    required this.totalTime,
    required this.steps,
  });

  factory RouteResult.fromJson(Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    return RouteResult(
      totalDistance: data['totalDistance'] as int,
      totalTime: data['totalTime'] as int,
      steps: (data['steps'] as List)
          .map((s) => RouteStep.fromJson(s as Map<String, dynamic>))
          .toList(),
    );
  }
}

class RouteStep {
  final String type; // "POINT" or "LINE"

  // POINT 전용
  final double? latitude;
  final double? longitude;
  final String? description;
  final int? turnType;
  final String? pointType; // "SP"=출발, "EP"=도착
  final int? facilityType; // 15 = 횡단보도

  // LINE 전용
  final List<LatLng>? path;

  const RouteStep({
    required this.type,
    this.latitude,
    this.longitude,
    this.description,
    this.turnType,
    this.pointType,
    this.facilityType,
    this.path,
  });

  factory RouteStep.fromJson(Map<String, dynamic> json) {
    final type = json['type'] as String;
    if (type == 'LINE') {
      final pathList = (json['path'] as List)
          .map((p) => LatLng(
                latitude: (p['latitude'] as num).toDouble(),
                longitude: (p['longitude'] as num).toDouble(),
              ))
          .toList();
      return RouteStep(type: type, path: pathList);
    }
    return RouteStep(
      type: type,
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      description: json['description'] as String?,
      turnType: json['turnType'] as int?,
      pointType: json['pointType'] as String?,
      facilityType: int.tryParse(json['facilityType']?.toString() ?? ''),
    );
  }
}

class LatLng {
  final double latitude;
  final double longitude;

  const LatLng({required this.latitude, required this.longitude});
}