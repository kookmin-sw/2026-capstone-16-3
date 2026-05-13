import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:safepath/service/api_client.dart';

class WeatherService {
  static final WeatherService _instance = WeatherService._internal();
  factory WeatherService() => _instance;
  WeatherService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  // OpenWeather API description → 한국어 매핑 (공식 description 전체 목록)
  static const Map<String, String> _descToKo = {
    // Thunderstorm
    'thunderstorm with light rain': '가벼운 비를 동반한 천둥번개',
    'thunderstorm with rain': '비를 동반한 천둥번개',
    'thunderstorm with heavy rain': '폭우를 동반한 천둥번개',
    'light thunderstorm': '약한 천둥번개',
    'thunderstorm': '천둥번개',
    'heavy thunderstorm': '강한 천둥번개',
    'ragged thunderstorm': '천둥번개',
    'thunderstorm with light drizzle': '이슬비를 동반한 천둥번개',
    'thunderstorm with drizzle': '이슬비를 동반한 천둥번개',
    'thunderstorm with heavy drizzle': '이슬비를 동반한 천둥번개',
    // Drizzle
    'light intensity drizzle': '가벼운 이슬비',
    'drizzle': '이슬비',
    'heavy intensity drizzle': '강한 이슬비',
    'light intensity drizzle rain': '가벼운 이슬비',
    'drizzle rain': '이슬비',
    'heavy intensity drizzle rain': '강한 이슬비',
    'shower rain and drizzle': '소나기와 이슬비',
    'heavy shower rain and drizzle': '강한 소나기와 이슬비',
    'shower drizzle': '이슬비',
    // Rain
    'light rain': '가벼운 비',
    'moderate rain': '비',
    'heavy intensity rain': '강한 비',
    'very heavy rain': '매우 강한 비',
    'extreme rain': '폭우',
    'freezing rain': '빙판 비',
    'light intensity shower rain': '가벼운 소나기',
    'shower rain': '소나기',
    'heavy intensity shower rain': '강한 소나기',
    'ragged shower rain': '소나기',
    // Snow
    'light snow': '약한 눈',
    'snow': '눈',
    'heavy snow': '폭설',
    'sleet': '진눈깨비',
    'light shower sleet': '약한 진눈깨비',
    'shower sleet': '진눈깨비',
    'light rain and snow': '비와 눈',
    'rain and snow': '비와 눈',
    'light shower snow': '약한 눈',
    'shower snow': '눈',
    'heavy shower snow': '폭설',
    // Atmosphere
    'mist': '안개',
    'smoke': '연기',
    'haze': '실안개',
    'sand/dust whirls': '황사',
    'fog': '짙은 안개',
    'sand': '황사',
    'dust': '먼지',
    'volcanic ash': '화산재',
    'squalls': '돌풍',
    'tornado': '토네이도',
    // Clear
    'clear sky': '맑음',
    // Clouds
    'few clouds': '구름 조금',
    'few clouds: 11-25%': '구름 조금',
    'scattered clouds': '구름 많음',
    'scattered clouds: 25-50%': '구름 많음',
    'broken clouds': '흐림',
    'broken clouds: 51-84%': '흐림',
    'overcast clouds': '흐림',
    'overcast clouds: 85-100%': '흐림',
  };

  Future<String?> fetchWeatherMessage({
    required double lat,
    required double lon,
  }) async {
    debugPrint('🌤 [Weather] API 호출: $_baseUrl/api/weather?lat=$lat&lon=$lon');
    try {
      final uri = Uri.parse('$_baseUrl/api/weather').replace(
        queryParameters: {'lat': lat.toString(), 'lon': lon.toString()},
      );
      final response = await ApiClient().get(uri);
      debugPrint('🌤 [Weather] 응답 status: ${response.statusCode}');
      debugPrint('🌤 [Weather] 응답 body: ${response.body}');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        if (body['success'] == true) {
          final data = body['data'] as Map<String, dynamic>;
          final description = data['description'] as String? ?? '';
          final temperature = (data['temperature'] as num?)?.toDouble() ?? 0;
          final windSpeed = (data['windSpeed'] as num?)?.toDouble() ?? 0;
          debugPrint('🌤 [Weather] description=$description, temp=$temperature, wind=$windSpeed');
          final message = _buildMessage(
            description: description,
            temperature: temperature,
            windSpeed: windSpeed,
          );
          debugPrint('🌤 [Weather] 생성된 멘트: $message');
          return message;
        } else {
          debugPrint('🌤 [Weather] success=false');
        }
      }
    } catch (e, st) {
      debugPrint('🌤 [Weather] API 오류: $e\n$st');
    }
    return null;
  }

  String _buildMessage({
    required String description,
    required double temperature,
    required double windSpeed,
  }) {
    final weatherKo = _descToKo[description.toLowerCase()] ?? description;

    final String tempStr;
    if (temperature < 0) {
      final abs = temperature.abs();
      final formatted = abs == abs.roundToDouble()
          ? abs.toInt().toString()
          : abs.toStringAsFixed(1);
      tempStr = '영하 $formatted도';
    } else {
      final formatted = temperature == temperature.roundToDouble()
          ? temperature.toInt().toString()
          : temperature.toStringAsFixed(1);
      tempStr = '$formatted도';
    }

    final buffer = StringBuffer('오늘 날씨는 $weatherKo, 기온은 $tempStr입니다.');

    // 날씨 상황별 조언
    final desc = description.toLowerCase();
    if (desc.contains('thunderstorm')) {
      buffer.write(' 천둥번개가 치고 있으니 야외 활동을 자제하세요.');
    } else if (desc.contains('rain') || desc.contains('drizzle')) {
      buffer.write(' 우산을 챙기세요.');
    } else if (desc.contains('snow') || desc.contains('sleet')) {
      buffer.write(' 눈길에 미끄럼을 주의하세요.');
    } else if (desc.contains('fog') || desc.contains('mist') || desc.contains('haze')) {
      buffer.write(' 시야가 좋지 않으니 주의하세요.');
    } else if (desc.contains('sand') || desc.contains('dust')) {
      buffer.write(' 마스크를 착용하세요.');
    }

    // 강풍 조언 (8m/s 이상일 때만)
    if (windSpeed >= 12) {
      buffer.write(' 강풍이 불고 있으니 외출 시 주의하세요.');
    } else if (windSpeed >= 8) {
      buffer.write(' 바람이 강하게 불고 있습니다.');
    }

    // 기온별 조언
    if (temperature <= 0) {
      buffer.write(' 따뜻하게 입고 외출하세요.');
    } else if (temperature >= 33) {
      buffer.write(' 폭염에 주의하고 수분을 충분히 섭취하세요.');
    } else if (temperature >= 28) {
      buffer.write(' 더위에 대비해 수분을 충분히 섭취하세요.');
    }

    return buffer.toString();
  }
}
