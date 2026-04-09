import 'package:flutter/material.dart';
import 'package:safepath/features/detection/detection_screen.dart';
import 'package:safepath/features/navigation/add_place_screen.dart';
import 'package:safepath/features/navigation/navigation_ing_screen.dart';
import 'package:safepath/features/navigation/navigation_screen.dart';
import 'package:safepath/features/navigation/saved_place_screen.dart';
import 'package:safepath/features/settings/app_info_screen.dart';
import 'package:safepath/features/settings/privacy_policy_screen.dart';
import 'package:safepath/features/settings/settings_screen.dart';
import 'package:safepath/features/settings/term_screen.dart';
import 'package:safepath/features/settings/user_info_screen.dart';
import 'package:safepath/features/settings/userguide_screen.dart';
import 'package:safepath/features/signin/signin_screen.dart';
import 'package:safepath/layout/layout.dart';

/// ========================================================================
/// 사용 예시:
///
/// 1. 다른 화면으로 이동
/// Navigator.pushNamed(context, AppRouter.settings);
///
/// 2. 이동 후 현재 화면 제거 (뒤로가기 불가)
/// Navigator.pushReplacementNamed(context, AppRouter.home);
///
/// 3. 이전 화면으로 돌아가기
/// Navigator.pop(context);
/// ========================================================================
///
///
/// ========================================================================
/// 새 화면 추가시 설정 방법
/// ========================================================================
///
/// 1. app_router.dart에 경로 추가
/// static const String navigation = '/navigation';
///
/// 2. switch문에 케이스 추가
/// case navigation:
///   return MaterialPageRoute(builder: (_) => const NavigationScreen());
///
/// ========================================================================
class AppRouter {
  static const String signin = '/';
  static const String home = '/home';
  static const String settings = '/settings';
  static const String userinfo = '/settings/userinfo';
  static const String userguide = '/settings/userguide';
  static const String appinfo = '/settings/appinfo';
  static const String term = '/settings/term';
  static const String policy = '/settings/policy';
  static const String navigation = '/navigation';
  static const String navigationing = '/navigation/ing';
  static const String savedplace = '/navigation/savedplace';
  static const String addplace = '/navigation/savedplace/add';
  static const String detection = '/detection';

  static Route<dynamic> generateRoute(RouteSettings settings) {
    switch (settings.name) {
      case signin:
        return MaterialPageRoute(builder: (_) => const SignInScreen());
      case home:
        return MaterialPageRoute(builder: (_) => const MainLayout());
      case AppRouter.settings:
        return MaterialPageRoute(builder: (_) => const SettingsScreen());
      case navigation:
        return MaterialPageRoute(builder: (_) => const NavigationScreen());
      case detection:
        return MaterialPageRoute(builder: (_) => const DetectionScreen());
      case userguide:
        return MaterialPageRoute(builder: (_) => const UserGuideScreen());
      case savedplace:
        return MaterialPageRoute(builder: (_) => const SavedPlaceScreen());
      case navigationing:
        return MaterialPageRoute(builder: (_) => const NavigationIngScreen());
      case addplace:
        return MaterialPageRoute(builder: (_) => const AddPlaceScreen());
      case appinfo:
        return MaterialPageRoute(builder: (_) => const AppInfoScreen());
      case term:
        return MaterialPageRoute(builder: (_) => const TermScreen());
      case policy:
        return MaterialPageRoute(builder: (_) => const PrivacyPolicyScreen());
      case userinfo:
        return MaterialPageRoute(builder: (_) => const UserInfoScreen());
      default:
        return MaterialPageRoute(
          builder: (_) =>
              const Scaffold(body: Center(child: Text('페이지를 찾을 수 없습니다'))),
        );
    }
  }
}
