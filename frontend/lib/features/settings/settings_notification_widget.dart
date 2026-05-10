import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:permission_handler/permission_handler.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/vibration_service.dart';

/// 알림 및 소리 섹션 (토글)
class SettingsNotificationWidget extends StatefulWidget {
  final bool initialPushNotificationEnabled;
  final bool initialVoiceGuidanceEnabled;
  final bool initialSoundEffectEnabled;

  final void Function(bool)? onPushNotificationChanged;
  final void Function(bool)? onVoiceGuidanceChanged;
  final void Function(bool)? onSoundEffectChanged;

  const SettingsNotificationWidget({
    super.key,
    required this.initialPushNotificationEnabled,
    required this.initialVoiceGuidanceEnabled,
    required this.initialSoundEffectEnabled,
    this.onPushNotificationChanged,
    this.onVoiceGuidanceChanged,
    this.onSoundEffectChanged,
  });

  @override
  State<SettingsNotificationWidget> createState() =>
      _SettingsNotificationWidgetState();
}

class _SettingsNotificationWidgetState extends State<SettingsNotificationWidget>
    with WidgetsBindingObserver {
  late bool _pushAlert;
  late bool _soundEffect;
  late bool _voiceGuide;

  /// "설정 열기" 후 앱 복귀 시 권한을 재확인해야 함을 나타내는 플래그
  bool _awaitingPermissionResult = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _pushAlert = widget.initialPushNotificationEnabled;
    _soundEffect = widget.initialSoundEffectEnabled;
    _voiceGuide = widget.initialVoiceGuidanceEnabled;
    _syncPushAlertWithPermission();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// 앱 복귀 시 "설정 열기"를 통해 이동했었다면 권한을 재확인한다.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && _awaitingPermissionResult) {
      _awaitingPermissionResult = false;
      _recheckAndSyncPushAlert();
    }
  }

  /// 앱 진입 시 저장된 ON 상태가 실제 권한과 불일치하면 OFF로 보정한다.
  Future<void> _syncPushAlertWithPermission() async {
    if (!_pushAlert) return;
    final status = await Permission.notification.status;
    if (!status.isGranted && mounted) {
      setState(() => _pushAlert = false);
      widget.onPushNotificationChanged?.call(false);
    }
  }

  /// 권한 상태에 따라 토글 UI를 확정한다 (설정 복귀 후 또는 취소 후 호출).
  Future<void> _recheckAndSyncPushAlert() async {
    final status = await Permission.notification.status;
    if (!mounted) return;
    if (status.isGranted) {
      setState(() => _pushAlert = true);
      widget.onPushNotificationChanged?.call(true);
    } else {
      setState(() => _pushAlert = false);
      widget.onPushNotificationChanged?.call(false);
    }
  }

  // ─── 푸시 알림 토글 ──────────────────────────────────────────────────────────

  Future<void> _onPushAlertChanged(bool value) async {
    if (!value) {
      setState(() => _pushAlert = false);
      widget.onPushNotificationChanged?.call(false);
      return;
    }

    // 낙관적 UI: 즉시 ON으로 표시
    setState(() => _pushAlert = true);

    final status = await Permission.notification.status;
    if (!mounted) return;

    if (status.isGranted) {
      widget.onPushNotificationChanged?.call(true);
      return;
    }

    // 권한 거부 → AlertDialog 표시 (UI는 ON 유지)
    final openedSettings = await _showPermissionGuidance();
    if (!mounted) return;

    if (openedSettings) {
      // 앱 복귀 시 didChangeAppLifecycleState가 재확인 처리
      _awaitingPermissionResult = true;
    } else {
      // 취소 → 권한 재확인 후 결정
      await _recheckAndSyncPushAlert();
    }
  }

  /// AlertDialog를 표시하고 "설정 열기"를 눌렀는지 여부를 반환한다.
  Future<bool> _showPermissionGuidance() async {
    bool openedSettings = false;
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(color: ColorCollection.point, width: 2),
        ),
        title: Text(
          '알림 권한 필요',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        content: Text(
          '알림 권한이 허용되지 않았습니다.\n시스템 설정에서 알림을 허용해주세요.',
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              '취소',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              openedSettings = true;
              Navigator.pop(ctx);
              openAppSettings();
            },
            child: Text(
              '설정 열기',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.main,
              ),
            ),
          ),
        ],
      ),
    );
    return openedSettings;
  }

  // ─── 빌드 ────────────────────────────────────────────────────────────────────

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
            onChanged: _onPushAlertChanged,
          ),
          _divider(),
          _ToggleRow(
            icon: Icons.volume_up_outlined,
            label: '효과음',
            description: '버튼 및 동작 효과음',
            value: _soundEffect,
            onChanged: (v) {
              setState(() => _soundEffect = v);
              if (v) {
                // OFF→ON 전환 시: 서비스 먼저 활성화 후 효과음 재생 (UX 피드백)
                SoundEffectService().setEnabled(true);
                SoundEffectService().play(SoundEffect.buttonTap);
              VibrationService().vibrate(VibrationEffect.buttonTap);
              }
              widget.onSoundEffectChanged?.call(v);
            },
          ),
          _divider(),
          _ToggleRow(
            icon: Icons.mic_outlined,
            label: '음성 안내',
            description: 'TTS 음성 안내 기능',
            value: _voiceGuide,
            onChanged: (v) {
              setState(() => _voiceGuide = v);
              widget.onVoiceGuidanceChanged?.call(v);
            },
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

// ─── 토글 행 ──────────────────────────────────────────────────────────────────

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
      toggled: value,
      label: '$label, $description',
      onTap: () {
        final newValue = !value;
        SoundEffectService().play(SoundEffect.buttonTap);
        VibrationService().vibrate(VibrationEffect.buttonTap);
        SemanticsService.announce('$label ${newValue ? '켜짐' : '꺼짐'}', TextDirection.ltr);
        onChanged(newValue);
      },
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
              onChanged: (v) {
                SoundEffectService().play(SoundEffect.buttonTap);
                VibrationService().vibrate(VibrationEffect.buttonTap);
                SemanticsService.announce('$label ${v ? '켜짐' : '꺼짐'}', TextDirection.ltr);
                onChanged(v);
              },
              activeThumbColor: Colors.white,
              activeTrackColor: ColorCollection.main,
              inactiveThumbColor: Colors.white,
              inactiveTrackColor: Colors.white.withValues(alpha: 0.5),
              trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
              thumbIcon: WidgetStateProperty.all(const Icon(null)),
            ),
          ],
        ),
      ),
    );
  }
}