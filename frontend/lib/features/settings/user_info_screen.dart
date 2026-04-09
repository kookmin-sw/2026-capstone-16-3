import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/routes/app_router.dart';

class UserInfoScreen extends StatelessWidget {
  const UserInfoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '계정', showBackButton: true),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: Column(
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
                      onTap: () => Navigator.pushNamedAndRemoveUntil(
                        context,
                        AppRouter.signin,
                        (route) => false,
                      ), // TODO: 로그아웃 로직 연결
                    ),
                    const SizedBox(height: 20),
                    ActionButton(
                      label: '회원 탈퇴',
                      icon: Icons.person_remove_rounded,
                      backgroundColor: ColorCollection.red,
                      onTap: () => Navigator.pushNamedAndRemoveUntil(
                        context,
                        AppRouter.signin,
                        (route) => false,
                      ), // TODO : 회원 탈퇴 로직 연결
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _UserCard extends StatelessWidget {
  const _UserCard({super.key});

  @override
  Widget build(BuildContext context) {
    final String name = '홍길동';
    final String email = 'gildong@gmail.com';

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: ColorCollection.point.withOpacity(0.1),
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
