import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:safepath/service/api_client.dart';

class UserProfile {
  final int id;
  final String nickname;
  final String createdAt;

  const UserProfile({
    required this.id,
    required this.nickname,
    required this.createdAt,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
        id: json['id'] as int,
        nickname: json['nickname'] as String,
        createdAt: json['createdAt'] as String,
      );
}

class UserService {
  static final UserService _instance = UserService._internal();
  factory UserService() => _instance;
  UserService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');

  Future<UserProfile?> getProfile() async {
    final uri = Uri.parse('$_baseUrl/api/users/me/profile');
    debugPrint('🟡 [User] GET $uri');
    try {
      final response = await ApiClient().get(uri);
      debugPrint('🟡 [User] 응답: ${response.statusCode}');
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        return UserProfile.fromJson(body['data'] as Map<String, dynamic>);
      }
    } catch (e) {
      debugPrint('🔴 [User] 프로필 조회 실패: $e');
    }
    return null;
  }
}