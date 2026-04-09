import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/navigation/navigation_step_card.dart';
import 'package:safepath/features/navigation/navigation_voiceguide_card.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:safepath/features/navigation/navigation_overview_card.dart';

class NavigationIngScreen extends StatefulWidget {
  const NavigationIngScreen({super.key});

  @override
  State<NavigationIngScreen> createState() => _NavigationIngScreenState();
}

class _NavigationIngScreenState extends State<NavigationIngScreen> {
  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false, // 뒤로가기 버튼 막기
      child: Scaffold(
        appBar: CustomTitleBar(title: '길찾기'),
        body: SafeArea(
          bottom: false,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
            child: Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        NavigationOverviewCard(
                          distance: 850,
                          time: 12,
                          status: RouteStatus.safe,
                        ),
                        const SizedBox(height: 20),
                        NavigationStepCard(
                          direction: DirectionType.straight,
                          instruction: '100m 앞에서 우회전하세요.',
                          distance: 100,
                        ),
                        const SizedBox(height: 20),
                        NavigationVoiceGuideCard(
                          voiceGuide: '50미터 앞 오른쪽에 킥보드가 감지되었습니다.',
                        ),
                      ],
                    ),
                  ),
                ),
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.only(top: 16, bottom: 24),
                    child: ActionButton(
                      label: '안내 중지',
                      icon: Icons.stop_circle_outlined,
                      backgroundColor: ColorCollection.red,
                      onTap: () => Navigator.pop(context),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
