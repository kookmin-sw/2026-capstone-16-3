import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 알림 및 소리 섹션 (토글)
class SettingsNotificationWidget extends StatefulWidget {
  const SettingsNotificationWidget({super.key});

  @override
  State<SettingsNotificationWidget> createState() =>
      _SettingsNotificationWidgetState();
}

class _SettingsNotificationWidgetState
    extends State<SettingsNotificationWidget> {
  bool _pushAlert = true;
  bool _soundEffect = false;
  bool _voiceGuide = true;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: ColorCollection.point, width: 1),
      ),
      child: Column(
        children: [
          _ToggleRow(
            icon: Icons.notifications_outlined,
            label: '푸시 알림',
            description: '중요 안내 및 업데이트 알림',
            value: _pushAlert,
            onChanged: (v) => setState(() => _pushAlert = v),
          ),
          _divider(),
          _ToggleRow(
            icon: Icons.volume_up_outlined,
            label: '효과음',
            description: '버튼 및 동작 효과음',
            value: _soundEffect,
            onChanged: (v) => setState(() => _soundEffect = v),
          ),
          _divider(),
          _ToggleRow(
            icon: Icons.mic_outlined,
            label: '음성 안내',
            description: 'TTS 음성 안내 기능',
            value: _voiceGuide,
            onChanged: (v) => setState(() => _voiceGuide = v),
          ),
        ],
      ),
    );
  }

  Widget _divider() => Divider(
    color: ColorCollection.point.withValues(alpha: 0.2),
    thickness: 1,
    height: 1,
    indent: 20,
    endIndent: 20,
  );
}

class _ToggleRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String description;
  final bool value;
  final ValueChanged<bool> onChanged;

  const _ToggleRow({
    required this.icon,
    required this.label,
    required this.description,
    required this.value,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$label: ${value ? '켜짐' : '꺼짐'}. $description',
      excludeSemantics: true,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Row(
          children: [
            Container(
              width: 43,
              height: 43,
              decoration: BoxDecoration(
                color: ColorCollection.main,
                borderRadius: BorderRadius.circular(5),
              ),
              child: Icon(icon, color: ColorCollection.point, size: 30),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: AppTextStyles.bodyBold.copyWith(
                      color: ColorCollection.point,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    description,
                    style: AppTextStyles.labelRegular.copyWith(
                      color: ColorCollection.point,
                    ),
                  ),
                ],
              ),
            ),
            Switch(
              value: value,
              onChanged: onChanged,
              activeThumbColor: Colors.white,
              activeTrackColor: ColorCollection.main,
              inactiveThumbColor: Colors.white,
              inactiveTrackColor: Colors.white.withValues(alpha: 0.5),
              trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
              // M3에서 inactive thumb이 작아지는 현상 방지 — thumbIcon 제공 시 양쪽 동일 크기로 고정됨
              thumbIcon: WidgetStateProperty.all(const Icon(null)),
            ),
          ],
        ),
      ),
    );
  }
}
