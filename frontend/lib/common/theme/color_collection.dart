import 'package:flutter/material.dart';

/// 앱 전체 색상 컬렉션
///
/// 사용 예시:
/// ColorCollection.main
/// ColorCollection.main.withValues(alpha: 0.5)
class ColorCollection {
  ColorCollection._();

  // ===========================
  // App Colors (대표 색상 - 6가지)
  // ===========================

  static const Color main = Color(0xFFE67E22);
  static const Color background = Color(0xFF001F3F);
  static const Color point = Color(0xFFF4F3EF);
  static const Color red = Color(0xFFE2282B);
  static const Color green = Color(0xFF24B500);
  static const Color yellow = Color(0xFFFFC107);
}
