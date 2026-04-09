import 'package:flutter/material.dart';

/// 시각장애인 보행 보조 앱 텍스트 스타일 정의
///
/// 접근성 기준:
/// - 최소 폰트 크기: 18px 이상
/// - 평균 폰트 굵기: Bold (w700) 이상
/// - NanumSquareNeo 폰트 사용
///
/// 사용 예시:
/// AppTextStyles.headline
/// AppTextStyles.bodyBold.copyWith(color: ColorCollection.main)
class AppTextStyles {
  AppTextStyles._();

  // ===========================
  // Headline - 화면 대제목
  // ===========================
  /// 30px / ExtraBold (w800)
  /// 용도: 메인 화면 제목, 중요 알림
  static const TextStyle headline = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w800,
    fontVariations: [FontVariation('wght', 800)],
    fontSize: 30,
    height: 1.3,
    letterSpacing: 0.4,
  );

  // ===========================
  // Title1 - 주요 제목
  // ===========================
  /// 25px / Heavy (w900)
  /// 용도: 섹션 제목, 경고 메시지
  static const TextStyle title1 = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w900,
    fontVariations: [FontVariation('wght', 900)],
    fontSize: 25,
    height: 1.3,
    letterSpacing: 0.4,
  );

  // ===========================
  // Title2 - 부제목
  // ===========================
  /// 22px / ExtraBold (w800)
  /// 용도: 카드 제목, 목록 항목 제목
  static const TextStyle title2 = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w800,
    fontVariations: [FontVariation('wght', 800)],
    fontSize: 22,
    height: 1.3,
    letterSpacing: 0.4,
  );

  // ===========================
  // Body - 본문
  // ===========================
  /// 20px / Bold (w700)
  /// 용도: 강조 본문, 중요 설명문
  static const TextStyle bodyBold = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w700,
    fontVariations: [FontVariation('wght', 700)],
    fontSize: 20,
    height: 1.4,
    letterSpacing: 0.4,
  );

  /// 20px / Regular (w500)
  /// 용도: 일반 본문, 설명문
  static const TextStyle bodyRegular = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w500,
    fontVariations: [FontVariation('wght', 500)],
    fontSize: 20,
    height: 1.4,
    letterSpacing: 0.4,
  );

  // ===========================
  // Label - 레이블/버튼
  // ===========================
  /// 18px / Bold (w700)
  /// 용도: 버튼 텍스트, 강조 레이블
  static const TextStyle labelBold = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w700,
    fontVariations: [FontVariation('wght', 700)],
    fontSize: 18,
    height: 1.4,
    letterSpacing: 0.4,
  );

  /// 18px / Regular (w500)
  /// 용도: 일반 레이블, 보조 텍스트
  static const TextStyle labelRegular = TextStyle(
    fontFamily: 'NanumSquareNeo',
    fontWeight: FontWeight.w500,
    fontVariations: [FontVariation('wght', 500)],
    fontSize: 18,
    height: 1.4,
    letterSpacing: 0.4,
  );
}

// ===========================
// 스타일 가이드
// ===========================
// headline     (30px/w800) - 화면 대제목, 중요 알림
// title1       (25px/w900) - 섹션 제목, 경고 메시지
// title2       (22px/w800) - 카드 제목, 목록 항목
// bodyBold     (20px/w700) - 강조 본문, 중요 설명문
// bodyRegular  (20px/w500) - 일반 본문, 설명문
// labelBold    (18px/w700) - 버튼, 강조 레이블
// labelRegular (18px/w500) - 일반 레이블, 보조 텍스트
//
// FontWeight 참조표
// w500 = Regular
// w700 = Bold
// w800 = ExtraBold
// w900 = Black (Heavy)
