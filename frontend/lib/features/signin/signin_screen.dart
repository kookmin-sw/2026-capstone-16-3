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

      Navigator.pushReplacementNamed(context, AppRouter.home);
    } on AuthException catch (e) {
      if (!mounted) return;
      await _showErrorDialog(e.message);
    } catch (e) {
      if (!mounted) return;
      await _showErrorDialog('로그인에 실패했습니다.\n다시 시도해주세요.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _showErrorDialog(String message) async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(color: ColorCollection.point, width: 2),
        ),
        title: Text(
          '오류',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        content: Text(
          message,
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              '확인',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.main,
              ),
            ),
          ),
        ],
      ),
    );
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
