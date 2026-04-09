import 'dart:convert';
import 'package:flutter/cupertino.dart';
import 'package:http/http.dart' as http;

class PlaceService {
  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  /// 역지오코딩 API
  static Future<String?> reverseGeocode({
    required double lat,
    required double lng,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/api/places/reverse-geocode').replace(
        queryParameters: {'lat': lat.toString(), 'lng': lng.toString()},
      );
      debugPrint('🟡 [Place] BE 요청: GET $uri');

      final response = await http.get(uri);

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);

        if (data['success']) {
          return data['data']['roadAddress'];
        }
      }

      return null;
    } catch (e) {
      debugPrint('🔴 [Place] reverse Geocoding error: $e');
      return null;
    }
  }

  /// 장소 검색 API
  static Future<List<Map<String, dynamic>>> searchPlaces({
    required String query,
    required double lat,
    required double lng,
    int radius = 2000,
    int page = 1,
    int size = 15,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/api/places/search').replace(
        queryParameters: {
          'query': query,
          'lat': lat.toString(),
          'lng': lng.toString(),
          'radius': radius.toString(),
          'page': page.toString(),
          'size': size.toString(),
        },
      );
      debugPrint('🟡 [Place] BE 장소 검색 요청: GET $uri');

      final response = await http.get(uri);

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);

        if (data['success']) {
          final items = data['data']['items'] as List;

          return items.map<Map<String, dynamic>>((item) {
            return {
              'name': item['name'],
              'roadAddress': item['roadAddress'],
              'distance': item['distanceMeters'],
              'lat': item['latitude'],
              'lng': item['longitude'],
            };
          }).toList();
        }
      }

      return [];
    } catch (e) {
      debugPrint('🔴 [Place] searchPlaces error: $e');
      return [];
    }
  }

  /// 즐겨찾기 장소 추가 API
  static Future<bool> addFavorite({
    required String placeId,
    required String name,
    required String alias,
    required String address,
    required double latitude,
    required double longitude,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/api/users/me/favorites');

      final body = jsonEncode({
        'placeId': placeId,
        'name': name,
        'alias': alias,
        'address': address,
        'latitude': latitude,
        'longitude': longitude,
      });

      debugPrint('🟡 [Place] 즐겨찾기 추가 요청: POST $uri');
      debugPrint('📤 body: $body');

      final response = await http.post(
        uri,
        headers: {'Content-Type': 'application/json'},
        body: body,
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        return data['created'] == true;
      }

      return false;
    } catch (e) {
      debugPrint('🔴 [Place] addFavorite error: $e');
      return false;
    }
  }
}
