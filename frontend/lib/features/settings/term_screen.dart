import 'package:flutter/material.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/data/term_dummy.dart';
import 'package:safepath/features/settings/policy_content.dart';

class TermScreen extends StatelessWidget {
  final bool isSummary;

  const TermScreen({super.key, this.isSummary = true});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(
        title: isSummary ? '이용 약관' : '전체 보기',
        showBackButton: true,
      ),
      body: SafeArea(
        bottom: false,
        child: PolicyContent(sections: termsData, isSummary: isSummary),
      ),
    );
  }
}
