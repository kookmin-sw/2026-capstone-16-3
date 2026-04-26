import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/settings/settings_info_widget.dart';
import 'package:safepath/features/settings/settings_notification_widget.dart';
import 'package:safepath/features/settings/settings_profile_widget.dart';
import 'package:safepath/features/settings/settings_style_widget.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:safepath/service/sound_effect_service.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/user_settings_service.dart';

class SettingsScreen extends StatefulWidget {
  final ScrollController? scrollController;

  const SettingsScreen({super.key, this.scrollController});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  UserSettings? _settings;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final s = await UserSettingsService().fetch();
    if (mounted) {
      setState(() => _settings = s);
      TtsService().setVoiceGuidanceEnabled(s.voiceGuidanceEnabled);
      SoundEffectService().setEnabled(s.soundEffectEnabled);
    }
  }

  Future<void> _patch(UserSettings updated) async {
    setState(() => _settings = updated);
    UserSettingsService().patch(updated);
  }

  @override
  Widget build(BuildContext context) {
    final s = _settings;

    return Scaffold(
      appBar: const CustomTitleBar(title: '설정'),
      body: SafeArea(
        child: SingleChildScrollView(
          controller: widget.scrollController,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 유저 프로필
              SettingsProfileWidget(
                name: '홍길동',
                onTap: () => Navigator.pushNamed(context, AppRouter.userinfo),
              ),
              const SizedBox(height: 32),

              // 안내 스타일 개인화
              _SectionTitle(title: '안내 스타일 개인화'),
              const SizedBox(height: 12),
              if (s != null)
                SettingsStyleWidget(
                  initialSettings: s,
                  onSentenceLengthChanged: (v) =>
                      _patch(s.copyWith(sentenceLength: v)),
                  onVibrationChanged: (v) =>
                      _patch(s.copyWith(vibrationStrength: v)),
                  onGuidanceSpeedChanged: (v) =>
                      _patch(s.copyWith(guidanceSpeed: v)),
                )
              else
                const _SettingsLoadingPlaceholder(height: 220),
              const SizedBox(height: 32),

              // 알림 및 소리
              _SectionTitle(title: '알림 및 소리'),
              const SizedBox(height: 12),
              if (s != null)
                SettingsNotificationWidget(
                  initialPushNotificationEnabled: s.pushNotificationEnabled,
                  initialVoiceGuidanceEnabled: s.voiceGuidanceEnabled,
                  initialSoundEffectEnabled: s.soundEffectEnabled,
                  onPushNotificationChanged: (v) =>
                      _patch(s.copyWith(pushNotificationEnabled: v)),
                  onVoiceGuidanceChanged: (v) {
                    TtsService().setVoiceGuidanceEnabled(v);
                    _patch(s.copyWith(voiceGuidanceEnabled: v));
                  },
                  onSoundEffectChanged: (v) {
                    SoundEffectService().setEnabled(v);
                    _patch(s.copyWith(soundEffectEnabled: v));
                  },
                )
              else
                const _SettingsLoadingPlaceholder(height: 160),
              const SizedBox(height: 32),

              // 앱 정보
              _SectionTitle(title: '앱 정보'),
              const SizedBox(height: 12),
              SettingsInfoWidget(
                items: [
                  SettingsInfoItem(
                    icon: Icons.info_outline,
                    label: '앱 정보',
                    onTap: () =>
                        Navigator.pushNamed(context, AppRouter.appinfo),
                  ),
                  SettingsInfoItem(
                    icon: Icons.help_outline,
                    label: '사용 방법',
                    onTap: () =>
                        Navigator.pushNamed(context, AppRouter.userguide),
                  ),
                  SettingsInfoItem(
                    icon: Icons.description_outlined,
                    label: '이용 약관',
                    onTap: () => Navigator.pushNamed(context, AppRouter.term),
                  ),
                  SettingsInfoItem(
                    icon: Icons.shield_outlined,
                    label: '개인정보 처리 방침',
                    onTap: () => Navigator.pushNamed(context, AppRouter.policy),
                  ),
                ],
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  const _SectionTitle({required this.title});

  @override
  Widget build(BuildContext context) {
    return Text(
      title,
      style: AppTextStyles.title2.copyWith(color: ColorCollection.point),
    );
  }
}

class _SettingsLoadingPlaceholder extends StatelessWidget {
  final double height;
  const _SettingsLoadingPlaceholder({required this.height});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: Center(
        child: CircularProgressIndicator(
          color: ColorCollection.point,
          strokeWidth: 2,
        ),
      ),
    );
  }
}