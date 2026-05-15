import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/widgets/logo_widget.dart';
import 'package:safepath/features/signin/signin_screen.dart';
import 'package:safepath/layout/layout.dart';
import 'package:safepath/service/auth_service.dart';
import 'package:safepath/service/token_storage.dart';

/// 앱 시작 시 인증 상태에 따라 SignInScreen 또는 MainLayout을 직접 렌더링.
///
/// Navigator.push를 사용하지 않고 위젯 트리 안에서 조건부 렌더링하므로
/// 페이지 전환 애니메이션 없이 즉시 목적 화면이 나타난다.
///
/// 흐름:
///   로딩 중  → 로고 화면
///   토큰 없음 → SignInScreen (직접 렌더링)
///   reissue 성공 → MainLayout (직접 렌더링)
///   reissue 실패 → SignInScreen (직접 렌더링)
class AuthGate extends StatefulWidget {
  const AuthGate({super.key});

  @override
  State<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<AuthGate> {
  late final Future<bool> _authFuture;

  @override
  void initState() {
    super.initState();
    _authFuture = _checkAuth();
  }

  Future<bool> _checkAuth() async {
    final hasTokens = await TokenStorage().hasTokens;
    if (!hasTokens) return false;
    return AuthService().reissueToken();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _authFuture,
      builder: (context, snapshot) {
        final Widget child;

        if (snapshot.connectionState != ConnectionState.done) {
          child = const _SplashContent(key: ValueKey('splash'));
        } else if (snapshot.data == true) {
          child = const MainLayout(key: ValueKey('home'));
        } else {
          child = const SignInScreen(key: ValueKey('signin'));
        }

        return AnimatedSwitcher(
          duration: const Duration(milliseconds: 150),
          child: child,
        );
      },
    );
  }
}

class _SplashContent extends StatelessWidget {
  const _SplashContent({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: ColorCollection.background,
      body: Center(child: LogoWidget()),
    );
  }
}
