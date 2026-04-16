import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/features/navigation/navigation_step_card.dart';
import 'package:safepath/features/navigation/navigation_voiceguide_card.dart';
import 'package:safepath/features/navigation/navigation_overview_card.dart';
import 'dart:ui' show DisplayFeatureType;
import 'package:flutter/services.dart';

class NavigationIngScreen extends StatefulWidget {
  const NavigationIngScreen({super.key});

  @override
  State<NavigationIngScreen> createState() => _NavigationIngScreenState();
}

class _NavigationIngScreenState extends State<NavigationIngScreen> {
  @override
  void initState() {
    super.initState();
    // 가로 모드로 고정
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
  }

  @override
  void dispose() {
    // 다시 세로 모드 허용
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
    ]);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mq = MediaQuery.of(context);
    final vp = mq.viewPadding;
    final size = mq.size;

    // viewPadding이 0인 기기에서도 displayFeatures로 실제 카메라 cutout 영역을 감지
    double cutoutLeft = 0, cutoutTop = 0, cutoutRight = 0;
    for (final feature in mq.displayFeatures) {
      if (feature.type == DisplayFeatureType.cutout) {
        final b = feature.bounds;
        if (b.left <= 0) cutoutLeft = math.max(cutoutLeft, b.right);
        if (b.top <= 0) cutoutTop = math.max(cutoutTop, b.bottom);
        if (b.right >= size.width) {
          cutoutRight = math.max(cutoutRight, size.width - b.left);
        }
      }
    }

    final leftPad = math.max(vp.left, cutoutLeft) + 24;
    final topPad = math.max(vp.top, cutoutTop) + 24;
    final rightPad = math.max(vp.right, cutoutRight) + 24;
    final bottomPad = vp.bottom + 24;

    return PopScope(
      canPop: false, // 뒤로가기 버튼 막기
      child: Scaffold(
        body: Padding(
          padding: EdgeInsets.fromLTRB(leftPad, topPad, rightPad, 0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      child: SingleChildScrollView(
                        child: NavigationOverviewCard(
                          distance: 850,
                          time: 12,
                          status: RouteStatus.safe,
                        ),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(top: 16, bottom: bottomPad),
                      child: ActionButton(
                        label: '안내 중지',
                        icon: Icons.stop_circle_outlined,
                        backgroundColor: ColorCollection.red,
                        onTap: () {
                          SystemChrome.setPreferredOrientations([
                            DeviceOrientation.portraitUp,
                            DeviceOrientation.portraitDown,
                          ]);
                          Navigator.pop(context);
                        },
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 24),
              Expanded(
                child: Column(
                  children: [
                    Expanded(
                      child: SingleChildScrollView(
                        padding: const EdgeInsets.symmetric(horizontal: 2),
                        child: Column(
                          children: [
                            NavigationStepCard(
                              direction: DirectionType.straight,
                              instruction: '100m 앞에서 우회전하세요.',
                              distance: 100,
                            ),
                            const SizedBox(height: 20),
                            NavigationVoiceGuideCard(
                              voiceGuide: '50미터 앞 오른쪽에 킥보드가 감지되었습니다.',
                            ),
                            const SizedBox(height: 20),
                            NavigationVoiceGuideCard(
                              voiceGuide: '50미터 앞 오른쪽에 킥보드가 감지되었습니다.',
                            ),
                            SizedBox(height: bottomPad),
                          ],
                        ),
                      ),
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
