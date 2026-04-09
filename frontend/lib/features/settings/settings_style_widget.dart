import 'package:flutter/material.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 안내 스타일 개인화 섹션 (슬라이더)
class SettingsStyleWidget extends StatefulWidget {
  const SettingsStyleWidget({super.key});

  @override
  State<SettingsStyleWidget> createState() => _SettingsStyleWidgetState();
}

class _SettingsStyleWidgetState extends State<SettingsStyleWidget> {
  double _messageLength = 0.9;
  double _vibrationStrength = 0.5;

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
            value: _messageLength,
            description: '안내 메시지의 상세도를 조절합니다.',
            onChanged: (v) => setState(() => _messageLength = v),
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

  const _SliderRow({
    required this.label,
    required this.value,
    required this.description,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final percent = '${(value * 100).round()}%';
    return Semantics(
      label: '$label $percent. $description',
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
                percent,
                style: AppTextStyles.labelBold.copyWith(
                  color: ColorCollection.main,
                ),
              ),
            ],
          ),
          SizedBox(height: 14),
          SliderTheme(
            data: SliderTheme.of(context).copyWith(
              trackHeight: 13,
              trackShape: _GradientTrackShape(),
              thumbShape: const _RoundedRectThumbShape(),
              overlayShape: SliderComponentShape.noOverlay,
              inactiveTrackColor: ColorCollection.point.withValues(alpha: 0.15),
            ),
            child: Slider(value: value, onChanged: onChanged),
          ),
          SizedBox(height: 8),
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

// ─── 가로형 흰색 Thumb ────────────────────────────────────────────────────────

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
