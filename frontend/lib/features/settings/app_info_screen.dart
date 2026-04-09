import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/logo_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';

class AppInfoScreen extends StatelessWidget {
  const AppInfoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '앱 정보', showBackButton: true),
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisAlignment: MainAxisAlignment.center,
              children: const [
                Center(child: LogoWidget()),
                SizedBox(height: 40),
                _AppInfoContent(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _AppInfoContent extends StatelessWidget {
  const _AppInfoContent({super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          'Version 1.0',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        const SizedBox(height: 14),
        Text(
          'Copyright © Retriever Corp.\nAll rights reserved.',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }
}
