import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:safepath/models/crosswalk_info.dart';

class CrosswalkService {
  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  /// 횡단보도 음성신호기 정보 조회 (공개 엔드포인트 — Authorization 헤더 불필요)
  /// 거리순 정렬 결과 중 0번째 항목만 사용
  static Future<CrosswalkInfo?> getNearby({
    required double lat,
    required double lng,
    double radiusMeters = 50,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/api/crosswalks/nearby').replace(
        queryParameters: {
          'latitude': lat.toString(),
          'longitude': lng.toString(),
          'radiusMeters': radiusMeters.toString(),
        },
      );
      debugPrint('🟡 [Crosswalk] 요청: GET $uri');

      final response = await http.get(uri);

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          final list = data['data'] as List?;
          if (list == null || list.isEmpty) return null;

          final item = list[0] as Map<String, dynamic>;
          final acousticSignal = item['acousticSignal'] as Map<String, dynamic>?;
          final guidance = item['guidance'] as Map<String, dynamic>?;

          return CrosswalkInfo(
            acousticSignalInstalled: acousticSignal?['installed'] as bool? ?? false,
            guidanceSummary: guidance?['summary'] as String? ?? '',
          );
        }
      }

      return null;
    } catch (e) {
      debugPrint('🔴 [Crosswalk] getNearby error: $e');
      return null;
    }
  }
}