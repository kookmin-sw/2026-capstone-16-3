import 'package:flutter/material.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/features/settings/policy_card.dart';
import 'package:safepath/features/settings/privacy_policy_screen.dart';
import 'package:safepath/features/settings/term_screen.dart';
import 'package:safepath/models/policy_section.dart';

class PolicyContent extends StatelessWidget {
  final List<PolicySection> sections;
  final bool isSummary;
  final bool isTerm; // true: 이용약관, false : 개인정보

  const PolicyContent({
    super.key,
    required this.sections,
    this.isSummary = true,
    this.isTerm = true,
  });

  @override
  Widget build(BuildContext context) {
    final displaySections = isSummary
        ? sections.take(1).toList()
        : sections.skip(1).toList();

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Expanded(
            child: ListView(
              children: displaySections
                  .map((e) => PolicyCard(section: e, isSummary: isSummary))
                  .toList(),
            ),
          ),

          if (isSummary)
            SafeArea(
              child: Padding(
                padding: const EdgeInsets.only(top: 16, bottom: 24),
                child: ActionButton(
                  label: '전체 보기',
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => isTerm
                          ? const TermScreen(isSummary: false)
                          : const PrivacyPolicyScreen(isSummary: false),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
