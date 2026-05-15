import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:safepath/service/auth_service.dart';
import 'package:safepath/service/token_storage.dart';

/// Authorization 헤더 자동 첨부 및 토큰 재발급을 지원하는 HTTP 클라이언트.
///
/// - Authorization 헤더 자동 첨부
/// - 401 응답 시 reissue 후 1회 재시도
/// - reissue 실패 시 signOut() 호출 (강제 로그아웃)
///
/// 기존 서비스(auth_service, camera_service 등)는 http 패키지를 직접 사용 중.
/// 인증이 필요한 신규 API 호출에 이 클라이언트를 사용할 것.
class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;
  ApiClient._internal();

  Future<http.Response> get(Uri uri) => _send('GET', uri);

  Future<http.Response> post(Uri uri, {Object? body}) =>
      _send('POST', uri, body: body);

  Future<http.Response> put(Uri uri, {Object? body}) =>
      _send('PUT', uri, body: body);

  Future<http.Response> patch(Uri uri, {Object? body}) =>
      _send('PATCH', uri, body: body);

  Future<http.Response> delete(Uri uri) => _send('DELETE', uri);

  Future<http.Response> _send(
    String method,
    Uri uri, {
    Object? body,
    bool isRetry = false,
  }) async {
    final token = await TokenStorage().accessToken;
    final headers = {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };

    final response = await _execute(method, uri, headers, body);

    if (response.statusCode == 401 && !isRetry) {
      debugPrint('🟡 [ApiClient] 401 → 토큰 재발급 시도');
      final refreshed = await AuthService().reissueToken();
      if (refreshed) {
        return _send(method, uri, body: body, isRetry: true);
      }
      debugPrint('🔴 [ApiClient] 재발급 실패 → 강제 로그아웃');
      await AuthService().signOut();
    }

    return response;
  }

  Future<http.Response> _execute(
    String method,
    Uri uri,
    Map<String, String> headers,
    Object? body,
  ) {
    final encoded = body != null ? jsonEncode(body) : null;
    switch (method) {
      case 'GET':
        return http.get(uri, headers: headers);
      case 'POST':
        return http.post(uri, headers: headers, body: encoded);
      case 'PUT':
        return http.put(uri, headers: headers, body: encoded);
      case 'PATCH':
        return http.patch(uri, headers: headers, body: encoded);
      case 'DELETE':
        return http.delete(uri, headers: headers);
      default:
        throw UnsupportedError('Unsupported HTTP method: $method');
    }
  }
}
