import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/button_widget.dart';
import 'package:safepath/common/widgets/logo_widget.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:safepath/service/auth_service.dart';

class SignInScreen extends StatefulWidget {
  const SignInScreen({super.key});

  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  bool _isLoading = false;

  Future<void> _onKakaoLogin() async {
    setState(() => _isLoading = true);
    try {
      await AuthService().signInWithKakao();
      if (!mounted) return;

      // 신규/기존 유저 모두 홈으로 (카카오 회원가입은 SDK가 자동 처리)
      Navigator.pushReplacementNamed(context, AppRouter.home);
    } on AuthException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(e.message),
          backgroundColor: ColorCollection.red,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('로그인에 실패했습니다. 다시 시도해주세요.'),
          backgroundColor: ColorCollection.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            const SizedBox(height: 33),
            const Center(child: LogoWidget()),
            const SizedBox(height: 63),
            CustomButton(
              width: 344,
              height: 218,
              title: _isLoading ? '로그인 중...' : '시작하기',
              titleSubtitleSpacing: 17,
              subtitle: '카카오계정으로 시작',
              backgroundColor: ColorCollection.main,
              titleColor: ColorCollection.background,
              titleStyle: AppTextStyles.headline,
              subtitleColor: ColorCollection.background,
              subtitleStyle: AppTextStyles.bodyBold,
              borderColor: ColorCollection.main,
              onTap: _isLoading ? null : _onKakaoLogin,
            ),
            const SizedBox(height: 28),
            CustomButton(
              width: 344,
              height: 218,
              title: '사용 방법',
              titleSubtitleSpacing: 17,
              subtitle: '사용 가이드 보기',
              backgroundColor: ColorCollection.background,
              titleColor: ColorCollection.point,
              titleStyle: AppTextStyles.headline,
              subtitleColor: ColorCollection.point,
              subtitleStyle: AppTextStyles.bodyBold,
              borderColor: ColorCollection.point,
              onTap: () => Navigator.pushNamed(context, AppRouter.userguide),
            ),
          ],
        ),
      ),
    );
  }
}
