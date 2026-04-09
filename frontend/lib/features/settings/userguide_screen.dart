import 'package:flutter/material.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';

import 'package:safepath/features/settings/userguide_widget.dart';

class UserGuideScreen extends StatelessWidget {
  const UserGuideScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final guides = [
      (
        title: '기본 조작',
        description:
            '화면을 한 손가락으로 천천히 탐색하세요. 버튼에 닿으면 음성으로 설명이 나옵니다. 두 번 탭하여 선택하세요.',
      ),
      (
        title: '길찾기',
        description: '목적지를 음성으로 말하거나 직접 입력하세요. 실시간으로 경로와 주변 정보를 안내 받을 수 있습니다.',
      ),
      (
        title: '위험 탐지',
        description:
            '주변 장애물이나 위험 요소를 감지하면 즉시 음성과 진동으로 경고합니다. 안내에 따라 안전하게 이동하세요.',
      ),
      (
        title: '설정',
        description:
            '설정 메뉴에서 음성 안내 빈도, 진동 강도 등을 조절할 수 있습니다. 본인에게 맞는 설정으로 변경하세요.',
      ),
      (
        title: '음성 명령',
        description:
            '호출어 \'헤이 세이프\'를 부르거나 화면을 두 손가락으로 길게 누르면 음성 명령 모드가 활성화됩니다. 손을 쓰기 어려운 상황에서도 목소리만으로 모든 기능을 자유롭게 제어해 보세요.',
      ),
    ];

    return Scaffold(
      appBar: CustomTitleBar(title: '사용 방법', showBackButton: true),
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: ListView(
            children: [
              for (int i = 0; i < guides.length; i++) ...[
                UserGuideWidget(
                  title: guides[i].title,
                  description: guides[i].description,
                  number: i + 1,
                ),
                if (i != guides.length - 1) const SizedBox(height: 21),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
