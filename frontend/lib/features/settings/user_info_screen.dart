import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:safepath/service/api_client.dart';
import 'package:safepath/service/auth_service.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/vibration_service.dart';

class UserInfoScreen extends StatefulWidget {
  const UserInfoScreen({super.key});

  @override
  State<UserInfoScreen> createState() => _UserInfoScreenState();
}

class _UserInfoScreenState extends State<UserInfoScreen> {
  bool _isLoading = false;

  Future<void> _onLogout() async {
    setState(() => _isLoading = true);
    try {
      await AuthService().signOut();
    } finally {
      if (mounted) {
        Navigator.pushNamedAndRemoveUntil(
          context,
          AppRouter.signin,
          (route) => false,
        );
      }
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

  Future<void> _onDeleteAccount() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        title: Text(
          '회원 탈퇴',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        content: Text(
          '탈퇴하면 모든 데이터가 삭제되며\n복구할 수 없습니다. 계속하시겠습니까?',
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () {
              SoundEffectService().play(SoundEffect.buttonTap);
              VibrationService().vibrate(VibrationEffect.buttonTap);
              Navigator.pop(ctx, false);
            },
            child: Text('취소', style: TextStyle(color: ColorCollection.point)),
          ),
          TextButton(
            onPressed: () {
              SoundEffectService().play(SoundEffect.buttonTap);
              VibrationService().vibrate(VibrationEffect.buttonTap);
              Navigator.pop(ctx, true);
            },
            child: Text('탈퇴', style: TextStyle(color: ColorCollection.red)),
          ),
        ],
      ),
    );

    if (confirmed != true || !mounted) return;

    setState(() => _isLoading = true);
    try {
      final uri = Uri.parse(
        '${const String.fromEnvironment('BASE_URL')}/api/users/me/profile',
      );
      debugPrint('🔴 [UserInfo] 회원탈퇴 요청: DELETE $uri');
      final response = await ApiClient().delete(uri);
      debugPrint(
        '🔴 [UserInfo] 회원탈퇴 응답: ${response.statusCode} / ${response.body}',
      );
      if (response.statusCode != 200 && response.statusCode != 204) {
        if (mounted) {
          await _showErrorDialog('회원 탈퇴에 실패했습니다.\n다시 시도해주세요.');
        }
        return;
      }
      await AuthService().clearLocalSession();
      if (mounted) {
        Navigator.pushNamedAndRemoveUntil(
          context,
          AppRouter.signin,
          (route) => false,
        );
      }
    } catch (e) {
      if (mounted) {
        await _showErrorDialog('회원 탈퇴에 실패했습니다.\n다시 시도해주세요.');
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '계정', showBackButton: true),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: Stack(
            children: [
              Column(
                children: [
                  _UserCard(),
                  const Spacer(),
                  Padding(
                    padding: const EdgeInsets.only(top: 16, bottom: 24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        ActionButton(
                          label: '로그아웃',
                          icon: Icons.logout,
                          onTap: _isLoading ? null : _onLogout,
                        ),
                        const SizedBox(height: 20),
                        ActionButton(
                          label: '회원 탈퇴',
                          icon: Icons.person_remove_rounded,
                          backgroundColor: ColorCollection.red,
                          onTap: _isLoading ? null : _onDeleteAccount,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              if (_isLoading) const Center(child: CircularProgressIndicator()),
            ],
          ),
        ),
      ),
    );
  }
}

class _UserCard extends StatelessWidget {
  const _UserCard();

  @override
  Widget build(BuildContext context) {
    final String name = '홍길동';
    final String email = 'gildong@gmail.com';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(width: 2, color: ColorCollection.point),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Icon(
                Icons.account_circle_rounded,
                color: ColorCollection.point,
                size: 50,
              ),
              const SizedBox(width: 13),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: AppTextStyles.bodyBold.copyWith(
                      color: ColorCollection.point,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    email,
                    style: AppTextStyles.labelRegular.copyWith(
                      color: ColorCollection.point,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 10),
          Divider(color: ColorCollection.point, thickness: 1),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Image.asset(
                'assets/images/kakao-logo.png',
                width: 30,
                height: 30,
              ),
              const SizedBox(width: 10),
              Text(
                'Kakao 소셜 로그인 중',
                style: AppTextStyles.labelRegular.copyWith(
                  color: ColorCollection.point,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
