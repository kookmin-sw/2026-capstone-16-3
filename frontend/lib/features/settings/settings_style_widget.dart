import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/user_settings_service.dart';
import 'package:safepath/service/vibration_service.dart';

/// 안내 스타일 개인화 섹션 (접근성 개선 버전)
///
/// 슬라이더 대신 탭 한 번으로 조작 가능한 버튼 UI 사용:
///  - 안내 문장 길이 → 3개 세그먼트 버튼 (간결 / 보통 / 상세)
///  - 안내 문장 빠르기 → − / + 스테퍼 (0.5배속 단위)
///  - 진동 강도 → − / + 스테퍼 (10% 단위)
class SettingsStyleWidget extends StatefulWidget {
  final UserSettings initialSettings;
  final void Function(MessageLength)? onSentenceLengthChanged;
  final void Function(int)? onVibrationChanged;
  final void Function(double)? onGuidanceSpeedChanged;

  const SettingsStyleWidget({
    super.key,
    required this.initialSettings,
    this.onSentenceLengthChanged,
    this.onVibrationChanged,
    this.onGuidanceSpeedChanged,
  });

  @override
  State<SettingsStyleWidget> createState() => _SettingsStyleWidgetState();
}

class _SettingsStyleWidgetState extends State<SettingsStyleWidget> {
  static const double _ttsMin = 0.5;
  static const double _ttsMax = 5.0;
  static const double _ttsStep = 0.5;

  static const int _vibMax = 100;
  static const int _vibStep = 10;

  late MessageLength _messageLength;
  late double _ttsSpeed; // 0.5 ~ 5.0, step 0.5
  late int _vibration; // 0 ~ 100, step 10

  @override
  void initState() {
    super.initState();
    _messageLength = widget.initialSettings.sentenceLength;

    final rawSpeed = widget.initialSettings.guidanceSpeed.clamp(_ttsMin, _ttsMax);
    _ttsSpeed = (((rawSpeed - _ttsMin) / _ttsStep).round() * _ttsStep + _ttsMin)
        .clamp(_ttsMin, _ttsMax);

    final rawVib = widget.initialSettings.vibrationStrength.clamp(0, _vibMax);
    _vibration = ((rawVib / _vibStep).round() * _vibStep).clamp(0, _vibMax);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
      decoration: BoxDecoration(
        color: ColorCollection.point.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: ColorCollection.point, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _SegmentedRow<MessageLength>(
            label: '안내 문장 길이',
            description: '안내 메시지의 상세도를 조절합니다.',
            options: MessageLength.values,
            selected: _messageLength,
            labelOf: (m) => m.displayLabel,
            onSelected: (m) {
              VibrationService().vibrate(VibrationEffect.buttonTap);
              setState(() => _messageLength = m);
              widget.onSentenceLengthChanged?.call(m);
            },
          ),
          _divider(),
          _StepperRow(
            label: '안내 문장 빠르기',
            description: '안내 메시지의 재생 속도를 조절합니다.',
            displayValue: '${_ttsSpeed.toStringAsFixed(1)}배속',
            canDecrease: _ttsSpeed > _ttsMin,
            canIncrease: _ttsSpeed < _ttsMax,
            onDecrease: _decreaseSpeed,
            onIncrease: _increaseSpeed,
          ),
          _divider(),
          _StepperRow(
            label: '진동 강도',
            description: '경고 진동의 강도를 조절합니다.',
            displayValue: '$_vibration%',
            canDecrease: _vibration > 0,
            canIncrease: _vibration < _vibMax,
            onDecrease: _decreaseVibration,
            onIncrease: _increaseVibration,
          ),
        ],
      ),
    );
  }

  Widget _divider() => Divider(
    color: ColorCollection.point.withValues(alpha: 0.2),
    thickness: 1,
    height: 28,
  );

  void _decreaseSpeed() {
    final next = (_ttsSpeed - _ttsStep).clamp(_ttsMin, _ttsMax);
    setState(() => _ttsSpeed = next);
    TtsService().setSpeechRate(next);
    TtsService().speak('${next.toStringAsFixed(1)}배속', interrupt: true);
    widget.onGuidanceSpeedChanged?.call(next);
  }

  void _increaseSpeed() {
    final next = (_ttsSpeed + _ttsStep).clamp(_ttsMin, _ttsMax);
    setState(() => _ttsSpeed = next);
    TtsService().setSpeechRate(next);
    TtsService().speak('${next.toStringAsFixed(1)}배속', interrupt: true);
    widget.onGuidanceSpeedChanged?.call(next);
  }

  void _decreaseVibration() {
    final next = (_vibration - _vibStep).clamp(0, _vibMax);
    setState(() => _vibration = next);
    VibrationService().setStrength(next);
    VibrationService().vibrate(VibrationEffect.buttonTap);
    widget.onVibrationChanged?.call(next);
  }

  void _increaseVibration() {
    final next = (_vibration + _vibStep).clamp(0, _vibMax);
    setState(() => _vibration = next);
    VibrationService().setStrength(next);
    VibrationService().vibrate(VibrationEffect.buttonTap);
    widget.onVibrationChanged?.call(next);
  }
}

// ─── 세그먼트 선택 버튼 (안내 문장 길이) ──────────────────────────────────────────

class _SegmentedRow<T> extends StatelessWidget {
  final String label;
  final String description;
  final List<T> options;
  final T selected;
  final String Function(T) labelOf;
  final void Function(T) onSelected;

  const _SegmentedRow({
    required this.label,
    required this.description,
    required this.options,
    required this.selected,
    required this.labelOf,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ExcludeSemantics(
          child: Text(
            label,
            style: AppTextStyles.labelBold.copyWith(
              color: ColorCollection.point,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: options.map((option) {
            final isSelected = option == selected;
            final optLabel = labelOf(option);
            return Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Semantics(
                  label: '$label: $optLabel',
                  selected: isSelected,
                  button: true,
                  excludeSemantics: true,
                  child: GestureDetector(
                    onTap: () => onSelected(option),
                    child: Container(
                      height: 52,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: isSelected
                            ? ColorCollection.main
                            : ColorCollection.point.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected
                              ? ColorCollection.main
                              : ColorCollection.point.withValues(alpha: 0.3),
                          width: 2,
                        ),
                      ),
                      child: Text(
                        optLabel,
                        style: AppTextStyles.labelBold.copyWith(
                          color: isSelected ? Colors.white : ColorCollection.point,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
        const SizedBox(height: 8),
        ExcludeSemantics(
          child: Text(
            description,
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
              fontWeight: FontWeight.w400,
              fontSize: 16,
            ),
          ),
        ),
      ],
    );
  }
}

// ─── + / − 스테퍼 (빠르기·진동 강도) ─────────────────────────────────────────────

class _StepperRow extends StatelessWidget {
  final String label;
  final String description;
  final String displayValue;
  final bool canDecrease;
  final bool canIncrease;
  final VoidCallback onDecrease;
  final VoidCallback onIncrease;

  const _StepperRow({
    required this.label,
    required this.description,
    required this.displayValue,
    required this.canDecrease,
    required this.canIncrease,
    required this.onDecrease,
    required this.onIncrease,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ExcludeSemantics(
          child: Text(
            label,
            style: AppTextStyles.labelBold.copyWith(
              color: ColorCollection.point,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            _StepButton(
              icon: Icons.remove,
              semanticsLabel: '$label 줄이기',
              enabled: canDecrease,
              onTap: onDecrease,
            ),
            Expanded(
              child: Semantics(
                label: '$label: $displayValue',
                child: Container(
                  height: 52,
                  alignment: Alignment.center,
                  margin: const EdgeInsets.symmetric(horizontal: 8),
                  decoration: BoxDecoration(
                    color: ColorCollection.point.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: ColorCollection.main, width: 2),
                  ),
                  child: ExcludeSemantics(
                    child: Text(
                      displayValue,
                      style: AppTextStyles.labelBold.copyWith(
                        color: ColorCollection.main,
                        fontWeight: FontWeight.w800,
                        fontSize: 20,
                      ),
                    ),
                  ),
                ),
              ),
            ),
            _StepButton(
              icon: Icons.add,
              semanticsLabel: '$label 늘리기',
              enabled: canIncrease,
              onTap: onIncrease,
            ),
          ],
        ),
        const SizedBox(height: 8),
        ExcludeSemantics(
          child: Text(
            description,
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
              fontWeight: FontWeight.w400,
              fontSize: 16,
            ),
          ),
        ),
      ],
    );
  }
}

class _StepButton extends StatelessWidget {
  final IconData icon;
  final String semanticsLabel;
  final bool enabled;
  final VoidCallback onTap;

  const _StepButton({
    required this.icon,
    required this.semanticsLabel,
    required this.enabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: semanticsLabel,
      button: true,
      enabled: enabled,
      excludeSemantics: true,
      child: GestureDetector(
        onTap: enabled ? onTap : null,
        child: Container(
          width: 56,
          height: 52,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: enabled
                ? ColorCollection.main.withValues(alpha: 0.15)
                : ColorCollection.point.withValues(alpha: 0.05),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: enabled
                  ? ColorCollection.main
                  : ColorCollection.point.withValues(alpha: 0.2),
              width: 2,
            ),
          ),
          child: Icon(
            icon,
            color: enabled
                ? ColorCollection.main
                : ColorCollection.point.withValues(alpha: 0.3),
            size: 28,
          ),
        ),
      ),
    );
  }
}