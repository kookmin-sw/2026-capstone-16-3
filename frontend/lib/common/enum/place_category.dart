import 'package:flutter/material.dart';

enum PlaceCategory { home, work, school, restaurant, mart, hospital, etc }

extension PlaceCategoryExtension on PlaceCategory {
  // 카테고리 이름
  String get label {
    switch (this) {
      case PlaceCategory.home:
        return '집';
      case PlaceCategory.work:
        return '회사';
      case PlaceCategory.school:
        return '학교';
      case PlaceCategory.restaurant:
        return '식당/카페';
      case PlaceCategory.mart:
        return '마트/편의점';
      case PlaceCategory.hospital:
        return '병원/약국';
      case PlaceCategory.etc:
        return '기타';
    }
  }

  // 아이콘
  IconData get icon {
    switch (this) {
      case PlaceCategory.home:
        return Icons.home;
      case PlaceCategory.work:
        return Icons.work_rounded;
      case PlaceCategory.school:
        return Icons.school_rounded;
      case PlaceCategory.restaurant:
        return Icons.restaurant;
      case PlaceCategory.mart:
        return Icons.local_grocery_store_outlined;
      case PlaceCategory.hospital:
        return Icons.local_hospital_outlined;
      case PlaceCategory.etc:
        return Icons.more_outlined;
    }
  }
}
