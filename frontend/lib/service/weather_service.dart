import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:safepath/service/api_client.dart';

class WeatherService {
  static final WeatherService _instance = WeatherService._internal();
  factory WeatherService() => _instance;
  WeatherService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  Future<String?> fetchDescription({
    required double lat,
    required double lon,
  }) async {
    debugPrint('🌤 [Weather] API 호출: $_baseUrl/api/weather?lat=$lat&lon=$lon');
    try {
      final uri = Uri.parse('$_baseUrl/api/weather').replace(
        queryParameters: {
          'lat': lat.toString(),
          'lon': lon.toString(),
        },
      );
      final response = await ApiClient().get(uri);
      debugPrint('🌤 [Weather] 응답 status: ${response.statusCode}');
      debugPrint('🌤 [Weather] 응답 body: ${response.body}');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        if (body['success'] == true) {
          final description = body['data']['description'] as String?;
          debugPrint('🌤 [Weather] description: $description');
          return description;
        } else {
          debugPrint('🌤 [Weather] success=false');
        }
      }
    } catch (e, st) {
      debugPrint('🌤 [Weather] API 오류: $e\n$st');
    }
    return null;
  }
}
