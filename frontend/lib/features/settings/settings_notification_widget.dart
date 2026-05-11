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

  PermissionStatus? _lastPermissionStatus;

  /// "설정 열기" 후 앱 복귀 시에만 ON 전환을 허용하는 플래그
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

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state != AppLifecycleState.resumed) return;
    if (_awaitingPermissionResult) {
      _awaitingPermissionResult = false;
      _recheckAfterSettings();
    } else {
      _syncPushAlertWithPermission();
    }
  }

  /// 권한이 거부로 바뀐 경우만 OFF로 보정한다.
  /// 권한이 허용되어 있어도 인앱에서 수동으로 OFF한 설정은 건드리지 않는다.
  Future<void> _syncPushAlertWithPermission() async {
    final status = await Permission.notification.status;
    if (!mounted) return;
    if (_lastPermissionStatus != null && status == _lastPermissionStatus) return;
    _lastPermissionStatus = status;

    if (!status.isGranted && _pushAlert) {
      setState(() => _pushAlert = false);
      widget.onPushNotificationChanged?.call(false);
    }
  }

  /// "설정 열기" 후 복귀 시 권한 결과에 따라 ON/OFF를 확정한다.
  Future<void> _recheckAfterSettings() async {
    final status = await Permission.notification.status;
    if (!mounted) return;
    _lastPermissionStatus = status;
    final granted = status.isGranted;
    if (_pushAlert != granted) {
      setState(() => _pushAlert = granted);
      widget.onPushNotificationChanged?.call(granted);
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

    // 권한 거부 → AlertDialog (UI는 ON 유지)
    await _showPermissionGuidance();
    if (!mounted) return;

    // 다이얼로그 닫힌 후 권한 재확인
    // 취소: 여전히 거부 → OFF 복귀
    // 설정 열기: 이탈 직후엔 아직 거부 → OFF 복귀, 복귀 후 _recheckAfterSettings가 ON 재보정
    final recheckStatus = await Permission.notification.status;
    if (!mounted) return;
    if (!recheckStatus.isGranted) {
      setState(() => _pushAlert = false);
      widget.onPushNotificationChanged?.call(false);
    }
  }

  Future<void> _showPermissionGuidance() async {
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
              _awaitingPermissionResult = true;
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