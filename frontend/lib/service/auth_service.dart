import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:safepath/service/token_storage.dart';

/// =======================================================
/// AuthService
///
/// 역할:
///   - 카카오 SDK로 access token 획득
///   - BE POST /api/auth/kakao/login { kakaoAccessToken } 호출
///   - BE가 Kakao /v2/user/me로 검증 후 JWT 발급
///   - 토큰 저장/재발급/로그아웃
///
/// BASE_URL은 .env.json → dart-define으로 주입
/// =======================================================
class AuthService {
  static final AuthService _instance = AuthService._internal();
  factory AuthService() => _instance;
  AuthService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  // ─── 카카오 로그인 ────────────────────────────────────────────────────────

  /// 카카오 SDK로 access token 획득 후 BE JWT 발급
  ///
  /// [onKakaoSdkComplete] SDK 호출 완료(성공/실패 모두) 시 호출되는 콜백.
  /// 앱 복귀 감지와 조합해 취소 시 로딩 해제에 사용된다.
  Future<AuthResult> signInWithKakao({VoidCallback? onKakaoSdkComplete}) async {
    String kakaoAccessToken;
    try {
      final kakaoTalkInstalled = await isKakaoTalkInstalled();
      debugPrint('🟡 [Auth] 카카오톡 설치 여부: $kakaoTalkInstalled');

      try {
        if (kakaoTalkInstalled) {
          kakaoAccessToken =
              (await UserApi.instance.loginWithKakaoTalk()).accessToken;
          debugPrint('🟡 [Auth] 카카오톡 앱으로 로그인 성공');
        } else {
          kakaoAccessToken =
              (await UserApi.instance.loginWithKakaoAccount()).accessToken;
          debugPrint('🟡 [Auth] 카카오 계정(웹)으로 로그인 성공');
        }
      } catch (e) {
        debugPrint('🔴 [Auth] 카카오 로그인 실패: $e');
        if (_isKakaoCancellation(e)) throw const AuthCancelledException();
        throw AuthException('카카오 로그인 실패: $e');
      } finally {
        onKakaoSdkComplete?.call();
      }
    } on AuthException {
      rethrow;
    } catch (e) {
      debugPrint('🔴 [Auth] 예상치 못한 오류: $e');
      throw AuthException('카카오 로그인 실패: $e');
    }

    return await _fetchTokenFromServer(kakaoAccessToken);
  }

  bool _isKakaoCancellation(Object e) {
    final msg = e.toString().toLowerCase();
    return msg.contains('cancelled') || msg.contains('canceled');
  }

  Future<AuthResult> _fetchTokenFromServer(String kakaoAccessToken) async {
    final uri = Uri.parse('$_baseUrl/api/auth/kakao/login');
    debugPrint('🟡 [Auth] BE 요청: POST $uri');

    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'kakaoAccessToken': kakaoAccessToken}),
    );
    debugPrint('🟡 [Auth] BE 응답: ${response.statusCode} / ${response.body}');

    if (response.statusCode == 200 || response.statusCode == 201) {
      final body = jsonDecode(response.body);
      final data = body['data'] as Map<String, dynamic>;
      await TokenStorage().saveTokens(
        accessToken: data['accessToken'] as String,
        refreshToken: data['refreshToken'] as String,
      );
      final isNewUser = response.statusCode == 201;
      debugPrint('✅ [Auth] 토큰 저장 완료 / 신규 유저: $isNewUser');
      return AuthResult(isNewUser: isNewUser);
    }

    throw AuthException('서버 인증 실패: ${response.statusCode}');
  }

  // ─── 토큰 재발급 ──────────────────────────────────────────────────────────

  Future<bool> reissueToken() async {
    final refreshToken = await TokenStorage().refreshToken;
    if (refreshToken == null) return false;

    final uri = Uri.parse('$_baseUrl/api/auth/reissue');
    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refreshToken}),
    );

    if (response.statusCode == 200) {
      final body = jsonDecode(response.body);
      final data = body['data'] as Map<String, dynamic>;
      await TokenStorage().saveTokens(
        accessToken: data['accessToken'] as String,
        refreshToken: data['refreshToken'] as String,
      );
      return true;
    }

    return false;
  }

  // ─── 로그아웃 ─────────────────────────────────────────────────────────────

  Future<void> signOut() async {
    final accessToken = await TokenStorage().accessToken;

    if (accessToken != null) {
      try {
        final uri = Uri.parse('$_baseUrl/api/auth/logout');
        debugPrint('🟡 [Auth] BE 요청: POST $uri');
        final response = await http.post(
          uri,
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer $accessToken',
          },
        );
        debugPrint('🟡 [Auth] 로그아웃 응답: ${response.statusCode}');
      } catch (e) {
        debugPrint('🔴 [Auth] 로그아웃 API 실패 (로컬 토큰은 삭제): $e');
      }
    }

    await TokenStorage().clear();
    try {
      await UserApi.instance.logout();
    } catch (_) {}
  }

  // ─── 로컬 세션 정리 ──────────────────────────────────────

  /// 회원탈퇴 후 호출. logout API 없이 로컬 토큰 삭제 + Kakao 앱 연결 해제(unlink)
  Future<void> clearLocalSession() async {
    await TokenStorage().clear();
    try {
      await UserApi.instance.unlink();
      debugPrint('🟡 [Auth] Kakao 연결 해제(unlink) 완료');
    } catch (e) {
      debugPrint('🔴 [Auth] Kakao unlink 실패: $e');
    }
    debugPrint('🟡 [Auth] 로컬 세션 정리 완료');
  }
}

/// 로그인 결과
class AuthResult {
  /// true = 신규 유저 (회원가입), false = 기존 유저 (로그인)
  final bool isNewUser;
  const AuthResult({required this.isNewUser});
}

class AuthException implements Exception {
  final String message;
  const AuthException(this.message);

  @override
  String toString() => 'AuthException: $message';
}

/// 사용자가 카카오 인증을 취소했을 때 던져지는 예외.
/// 오류 다이얼로그 없이 조용히 로딩을 해제하는 데 사용된다.
class AuthCancelledException extends AuthException {
  const AuthCancelledException() : super('');
}
