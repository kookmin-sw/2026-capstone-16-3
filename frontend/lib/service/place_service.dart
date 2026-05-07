import 'dart:convert';
import 'package:flutter/cupertino.dart';
import 'package:http/http.dart' as http;
import 'package:safepath/service/token_storage.dart';

enum ReverseGeocodeSource {
  exactAddress,   // KAKAO_ROAD_ADDRESS, KAKAO_JIBUN_ADDRESS, NAVER_ROAD_ADDRESS, NAVER_JIBUN_ADDRESS
  nearestPlace,   // KAKAO_NEAREST_PLACE
  regionAddress,  // KAKAO_REGION_ADDRESS
}

class ReverseGeocodeResult {
  final String address;
  final ReverseGeocodeSource source;

  const ReverseGeocodeResult({required this.address, required this.source});
}

class PlaceService {
  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  /// 역지오코딩 API
  static Future<ReverseGeocodeResult?> reverseGeocode({
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
        if (data['success'] == true) {
          final d = data['data'];
          final address = d['address'] as String? ?? '';
          final sourceStr = d['source'] as String? ?? '';
          debugPrint('🟡 [Place] reverseGeocode source: $sourceStr');

          if (sourceStr == 'KAKAO_ADDRESS_NOT_FOUND' || address.isEmpty) return null;

          final source = switch (sourceStr) {
            'KAKAO_ROAD_ADDRESS' ||
            'KAKAO_JIBUN_ADDRESS' ||
            'NAVER_ROAD_ADDRESS' ||
            'NAVER_JIBUN_ADDRESS' => ReverseGeocodeSource.exactAddress,
            'KAKAO_NEAREST_PLACE' => ReverseGeocodeSource.nearestPlace,
            _ => ReverseGeocodeSource.regionAddress,
          };

          return ReverseGeocodeResult(address: address, source: source);
        }
      }

      return null;
    } catch (e) {
      debugPrint('🔴 [Place] reverse Geocoding error: $e');
      return null;
    }
  }

  /// 장소 검색 API
  /// 반환값: {'items': List<Map>, 'hasNext': bool}
  static Future<Map<String, dynamic>> searchPlaces({
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
          final pageData = data['data'];
          final items = (pageData['items'] as List).map<Map<String, dynamic>>((
            item,
          ) {
            return {
              'id': item['placeId'],
              'name': item['name'],
              'roadAddress': item['roadAddress'],
              'distance': item['distanceM'],
              'latitude': item['lat'],
              'longitude': item['lng'],
            };
          }).toList();

          return {'items': items, 'hasNext': pageData['hasNext'] as bool};
        }
      }

      return {'items': <Map<String, dynamic>>[], 'hasNext': false};
    } catch (e) {
      debugPrint('🔴 [Place] searchPlaces error: $e');
      return {'items': <Map<String, dynamic>>[], 'hasNext': false};
    }
  }

  /// 장소 상세 조회 API (호출 시 최근 검색 목록에 자동 저장)
  static Future<void> getPlaceDetail({required String placeId}) async {
    try {
      final token = await TokenStorage().accessToken;

      final uri = Uri.parse('$_baseUrl/api/places/$placeId');
      debugPrint('🟡 [Place] 장소 상세 조회 요청: GET $uri');

      final response = await http.get(
        uri,
        headers: {
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');
    } catch (e) {
      debugPrint('🔴 [Place] getPlaceDetail error: $e');
    }
  }

  /// 최근 검색 장소 조회 API
  /// 반환값: {'items': List<Map>, 'totalItems': int, 'totalPages': int}
  static Future<Map<String, dynamic>> getRecentPlaces({
    int page = 1,
    int size = 20,
  }) async {
    try {
      final token = await TokenStorage().accessToken;

      final uri = Uri.parse('$_baseUrl/api/users/me/recent').replace(
        queryParameters: {'page': page.toString(), 'size': size.toString()},
      );
      debugPrint('🟡 [Place] 최근 검색 장소 조회 요청: GET $uri');

      final response = await http.get(
        uri,
        headers: {if (token != null) 'Authorization': 'Bearer $token'},
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          final pageData = data['data'];
          final items = (pageData['items'] as List).map<Map<String, dynamic>>((
            item,
          ) {
            return {
              'id': item['id'],
              'placeId': item['placeId'],
              'name': item['name'],
              'address': item['address'],
              'lat': item['lat'],
              'lng': item['lng'],
              'searchedAt': item['searchedAt'],
            };
          }).toList();

          return {
            'items': items,
            'hasNext': pageData['hasNext'] as bool,
            'nextPage': (pageData['nextPage'] ?? 0) as int,
          };
        }
      }

      return {
        'items': <Map<String, dynamic>>[],
        'hasNext': false,
        'nextPage': 0,
      };
    } catch (e) {
      debugPrint('🔴 [Place] getRecentPlaces error: $e');
      return {
        'items': <Map<String, dynamic>>[],
        'hasNext': false,
        'nextPage': 0,
      };
    }
  }

  /// 최근 검색 장소 전체 삭제 API
  static Future<bool> deleteAllRecentPlaces() async {
    try {
      final token = await TokenStorage().accessToken;

      final uri = Uri.parse('$_baseUrl/api/users/me/recent');
      debugPrint('🟡 [Place] 최근 검색 장소 전체 삭제 요청: DELETE $uri');

      final response = await http.delete(
        uri,
        headers: {
          if (token != null) 'Authorization': 'Bearer $token',
        },
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['success'] == true;
      }

      return false;
    } catch (e) {
      debugPrint('🔴 [Place] deleteAllRecentPlaces error: $e');
      return false;
    }
  }

  /// 즐겨찾기 장소 조회 API
  /// 반환값: {'items': List<Map>, 'hasNext': bool, 'nextPage': int}
  static Future<Map<String, dynamic>> getFavorites({
    int page = 1,
    int size = 15,
  }) async {
    try {
      final token = await TokenStorage().accessToken;

      final uri = Uri.parse('$_baseUrl/api/users/me/favorites').replace(
        queryParameters: {'page': page.toString(), 'size': size.toString()},
      );
      debugPrint('🟡 [Place] 즐겨찾기 조회 요청: GET $uri');

      final response = await http.get(
        uri,
        headers: {if (token != null) 'Authorization': 'Bearer $token'},
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          final pageData = data['data'];
          final items = (pageData['items'] as List).map<Map<String, dynamic>>((
            item,
          ) {
            return {
              'id': item['id'],
              'placeId': item['placeId'],
              'name': item['name'],
              'alias': item['alias'],
              'address': item['address'],
              'lat': item['lat'],
              'lng': item['lng'],
              'category': item['category'],
              'createdAt': item['createdAt'],
            };
          }).toList();

          return {
            'items': items,
            'hasNext': pageData['hasNext'] as bool,
            'nextPage': (pageData['nextPage'] ?? 0) as int,
          };
        }
      }

      return {
        'items': <Map<String, dynamic>>[],
        'hasNext': false,
        'nextPage': 0,
      };
    } catch (e) {
      debugPrint('🔴 [Place] getFavorites error: $e');
      return {
        'items': <Map<String, dynamic>>[],
        'hasNext': false,
        'nextPage': 0,
      };
    }
  }

  /// 즐겨찾기 장소 삭제 API
  static Future<bool> deleteFavorite({required int id}) async {
    try {
      final token = await TokenStorage().accessToken;

      final uri = Uri.parse('$_baseUrl/api/users/me/favorites/$id');
      debugPrint('🟡 [Place] 즐겨찾기 삭제 요청: DELETE $uri');

      final response = await http.delete(
        uri,
        headers: {if (token != null) 'Authorization': 'Bearer $token'},
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 204) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          return data['data']['deleted'] == true;
        }
      }

      return false;
    } catch (e) {
      debugPrint('🔴 [Place] deleteFavorite error: $e');
      return false;
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
    required String category,
  }) async {
    try {
      final uri = Uri.parse('$_baseUrl/api/users/me/favorites');

      final body = jsonEncode({
        'placeId': placeId,
        'name': name,
        'alias': alias,
        'address': address,
        'lat': latitude,
        'lng': longitude,
        'category': category,
      });

      final token = await TokenStorage().accessToken;

      debugPrint('🟡 [Place] 즐겨찾기 추가 요청: POST $uri');
      debugPrint('📤 body: $body');

      final response = await http.post(
        uri,
        headers: {
          'Content-Type': 'application/json',
          if (token != null) 'Authorization': 'Bearer $token',
        },
        body: body,
      );

      debugPrint('📥 statusCode: ${response.statusCode}');
      debugPrint('📥 body: ${response.body}');

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          return data['data']['created'] == true;
        }
      }

      return false;
    } catch (e) {
      debugPrint('🔴 [Place] addFavorite error: $e');
      return false;
    }
  }
}
