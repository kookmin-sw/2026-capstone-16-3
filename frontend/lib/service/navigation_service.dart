import 'dart:convert';
import 'package:flutter/cupertino.dart';
import 'package:http/http.dart' as http;
import 'package:safepath/models/route_result.dart';
import 'package:safepath/service/token_storage.dart';

class NavigationService {
  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  /// 길찾기 API
  static Future<RouteResult> getRoute({
    required double startX,
    required double startY,
    required double endX,
    required double endY,
    required String startName,
    required String endName,
  }) async {
    final uri = Uri.parse('$_baseUrl/api/navigation/routes');
    final body = jsonEncode({
      'startX': startX.toString(),
      'startY': startY.toString(),
      'endX': endX.toString(),
      'endY': endY.toString(),
      'startName': startName,
      'endName': endName,
    });

    final token = await TokenStorage().accessToken;

    debugPrint('🟡 [Navigation] 길찾기 요청: POST $uri');
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

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      if (data['success'] == true) {
        return RouteResult.fromJson(data);
      }
    }

    throw Exception('길찾기 API 실패 (${response.statusCode})');
  }
}