import 'package:flutter/material.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/data/privacy_policy_dummy.dart';
import 'package:safepath/features/settings/policy_content.dart';

class PrivacyPolicyScreen extends StatelessWidget {
  final bool isSummary;

  const PrivacyPolicyScreen({super.key, this.isSummary = true});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(
        title: isSummary ? '개인정보 처리 방침' : '전체 보기',
        showBackButton: true,
      ),
      body: SafeArea(
        bottom: false,
        child: PolicyContent(
          sections: privacyData,
          isSummary: isSummary,
          isTerm: false,
        ),
      ),
    );
  }
}
