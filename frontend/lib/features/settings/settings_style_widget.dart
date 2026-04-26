import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/service/tts_service.dart';
import 'package:safepath/service/user_settings_service.dart';

/// 안내 스타일 개인화 섹션 (슬라이더)
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
  // TTS 속도: 0.5배속~5.0배속 → 슬라이더 0.0~1.0으로 변환
  static const double _ttsMin = 0.5;
  static const double _ttsMax = 5.0;

  // 진동 강도: 백엔드 int 값 최대치 (Java Integer.MAX_VALUE)
  static const int _vibrationMax = 2147483647;

  late MessageLength _messageLength;
  late double _vibrationStrength; // 슬라이더 0.0~1.0
  late double _ttsSpeed; // 슬라이더 0.0~1.0

  @override
  void initState() {
    super.initState();
    _messageLength = widget.initialSettings.sentenceLength;
    _vibrationStrength =
        (widget.initialSettings.vibrationStrength / _vibrationMax).clamp(
          0.0,
          1.0,
        );
    final speed = widget.initialSettings.guidanceSpeed;
    _ttsSpeed =
        ((speed - _ttsMin) / (_ttsMax - _ttsMin)).clamp(0.0, 1.0);
  }

  double get _ttsSpeedValue => _ttsMin + _ttsSpeed * (_ttsMax - _ttsMin);

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
          _SliderRow(
            label: '안내 문장 길이',
            value: _messageLength.sliderValue,
            description: '안내 메시지의 상세도를 조절합니다.',
            divisions: 2,
            tickLabels: const ['간결', '보통', '상세'],
            valueFormatter: (_) => _messageLength.displayLabel,
            onChanged: (v) =>
                setState(() => _messageLength = MessageLength.fromSlider(v)),
            onChangeEnd: (_) =>
                widget.onSentenceLengthChanged?.call(_messageLength),
          ),
          const SizedBox(height: 4),
          Divider(
            color: ColorCollection.point.withValues(alpha: 0.2),
            thickness: 1,
            height: 28,
          ),
          _SliderRow(
            label: '안내 문장 빠르기',
            value: _ttsSpeed,
            description: '안내 메시지의 재생 속도를 조절합니다.',
            // 0.5~5.0배속, 0.1 단위 스냅: (5.0 - 0.5) / 0.1 = 45 divisions
            divisions: 45,
            valueFormatter: (v) {
              final speed = _ttsMin + v * (_ttsMax - _ttsMin);
              return '${speed.toStringAsFixed(1)}배속';
            },
            onChanged: (v) {
              setState(() => _ttsSpeed = v);
              TtsService().setSpeechRate(_ttsSpeedValue);
            },
            onChangeEnd: (_) =>
                widget.onGuidanceSpeedChanged?.call(_ttsSpeedValue),
          ),
          const SizedBox(height: 4),
          Divider(
            color: ColorCollection.point.withValues(alpha: 0.2),
            thickness: 1,
            height: 28,
          ),
          _SliderRow(
            label: '진동 강도',
            value: _vibrationStrength,
            description: '경고 진동의 강도를 조절합니다.',
            onChanged: (v) => setState(() => _vibrationStrength = v),
            onChangeEnd: (v) => widget.onVibrationChanged?.call(
              (v * _vibrationMax).round(),
            ),
          ),
        ],
      ),
    );
  }
}

class _SliderRow extends StatelessWidget {
  final String label;
  final double value;
  final String description;
  final ValueChanged<double> onChanged;
  final ValueChanged<double>? onChangeEnd;
  final String Function(double)? valueFormatter;
  final int? divisions;
  final List<String>? tickLabels;

  const _SliderRow({
    required this.label,
    required this.value,
    required this.description,
    required this.onChanged,
    this.onChangeEnd,
    this.valueFormatter,
    this.divisions,
    this.tickLabels,
  });

  String get _displayValue =>
      valueFormatter != null ? valueFormatter!(value) : '${(value * 100).round()}%';

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$label $_displayValue. $description',
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                label,
                style: AppTextStyles.labelBold.copyWith(
                  color: ColorCollection.point,
                  fontWeight: FontWeight.w800,
                ),
              ),
              Text(
                _displayValue,
                style: AppTextStyles.labelBold.copyWith(
                  color: ColorCollection.main,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          SliderTheme(
            data: SliderTheme.of(context).copyWith(
              trackHeight: 13,
              trackShape: _GradientTrackShape(),
              thumbShape: const _RoundedRectThumbShape(),
              overlayShape: SliderComponentShape.noOverlay,
              inactiveTrackColor: ColorCollection.point.withValues(alpha: 0.15),
            ),
            child: Slider(
              value: value,
              onChanged: onChanged,
              onChangeEnd: onChangeEnd,
              divisions: divisions,
            ),
          ),
          if (tickLabels != null) ...[
            const SizedBox(height: 5),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: tickLabels!
                    .map(
                      (t) => Text(
                        t,
                        style: AppTextStyles.labelRegular.copyWith(
                          color: ColorCollection.point,
                          fontWeight: FontWeight.w600,
                          fontSize: 16,
                        ),
                      ),
                    )
                    .toList(),
              ),
            ),
            const SizedBox(height: 8),
          ],
          const SizedBox(height: 8),
          Text(
            description,
            style: AppTextStyles.labelRegular.copyWith(
              color: ColorCollection.point,
              fontWeight: FontWeight.w400,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }
}

// ─── 그라데이션 트랙 ──────────────────────────────────────────────────────────

class _GradientTrackShape extends SliderTrackShape {
  static const _gradient = LinearGradient(
    colors: [Color(0xFFFFB06B), Color(0xFFE67E22)],
    stops: [0.0, 0.4],
  );

  @override
  Rect getPreferredRect({
    required RenderBox parentBox,
    Offset offset = Offset.zero,
    required SliderThemeData sliderTheme,
    bool isEnabled = false,
    bool isDiscrete = false,
  }) {
    final trackHeight = sliderTheme.trackHeight ?? 14;
    final trackTop = offset.dy + (parentBox.size.height - trackHeight) / 2;
    return Rect.fromLTWH(
      offset.dx,
      trackTop,
      parentBox.size.width,
      trackHeight,
    );
  }

  @override
  void paint(
    PaintingContext context,
    Offset offset, {
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required Animation<double> enableAnimation,
    required Offset thumbCenter,
    Offset? secondaryOffset,
    bool isEnabled = false,
    bool isDiscrete = false,
    required TextDirection textDirection,
  }) {
    final canvas = context.canvas;
    final rect = getPreferredRect(
      parentBox: parentBox,
      offset: offset,
      sliderTheme: sliderTheme,
    );
    final radius = Radius.circular(rect.height / 2);

    // 비활성 트랙 (전체 배경)
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, radius),
      Paint()
        ..color =
            sliderTheme.inactiveTrackColor ??
            ColorCollection.point.withValues(alpha: 0.15),
    );

    // 활성 트랙 (그라데이션)
    final activeRect = Rect.fromLTRB(
      rect.left,
      rect.top,
      thumbCenter.dx,
      rect.bottom,
    );
    if (activeRect.width > 0) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(activeRect, radius),
        Paint()..shader = _gradient.createShader(activeRect),
      );
    }
  }
}

// ─── 가로형 Thumb ─────────────────────────────────────────────────────────────

class _RoundedRectThumbShape extends SliderComponentShape {
  const _RoundedRectThumbShape();

  static const double _width = 20;
  static const double _height = 13;

  @override
  Size getPreferredSize(bool isEnabled, bool isDiscrete) =>
      const Size(_width, _height);

  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required Animation<double> activationAnimation,
    required Animation<double> enableAnimation,
    required bool isDiscrete,
    required TextPainter labelPainter,
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required TextDirection textDirection,
    required double value,
    required double textScaleFactor,
    required Size sizeWithOverflow,
  }) {
    final canvas = context.canvas;
    final rect = Rect.fromCenter(
      center: center,
      width: _width,
      height: _height,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, const Radius.circular(6)),
      Paint()..color = ColorCollection.point,
    );
  }
}
