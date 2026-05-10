import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 서비스 단위 권한 설명 시트
///
/// "권한 허용하기" 탭 시 카메라·위치·마이크·알림 시스템 팝업을 순서대로 띄운다.
/// 각 팝업을 거부해도 다음 팝업으로 진행하며, 모든 팝업이 끝나면 시트를 닫는다.
///
/// [needsLocation] true  → 카메라·위치가 모두 허용돼야 true 반환 (길찾기)
/// [needsLocation] false → 카메라만 허용되면 true 반환 (장애물 탐지)
class PermissionOnboardingSheet {
  const PermissionOnboardingSheet._();

  static const String _kShownKey = 'permission_onboarding_shown';

  /// 최초 1회만 권한 온보딩 시트를 표시한다 (SharedPreferences 기반).
  /// 이미 표시된 적 있으면 즉시 반환.
  static Future<void> showOnce(BuildContext context) async {
    final prefs = await SharedPreferences.getInstance();
    if (prefs.getBool(_kShownKey) ?? false) return;
    await prefs.setBool(_kShownKey, true);
    if (!context.mounted) return;
    await show(context);
  }

  /// 필수 권한이 이미 허용됐으면 즉시 true 반환 (시트 미표시).
  /// 그렇지 않으면 설명 시트를 표시하고 최종 허용 여부를 반환한다.
  static Future<bool> show(
    BuildContext context, {
    bool needsLocation = true,
  }) async {
    final camera = await Permission.camera.status;
    final location =
        needsLocation ? await Permission.locationWhenInUse.status : null;

    if (camera.isGranted && (location?.isGranted ?? true)) return true;

    if (!context.mounted) return false;

    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => _SheetContent(needsLocation: needsLocation),
    );
    return result ?? false;
  }

  /// 단순 거부 안내 — 영구 거부가 아닌 경우. "다시 시도" 안내로 충분.
  static Future<void> _showDenialNotice(
    BuildContext context, {
    required bool cameraDenied,
    required bool locationDenied,
  }) async {
    final items = [
      if (cameraDenied) '카메라',
      if (locationDenied) '위치',
    ];
    final permText = items.join('·');

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: ColorCollection.point, width: 2),
        ),
        title: Semantics(
          header: true,
          child: Text(
            '$permText 권한이 필요합니다',
            style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
          ),
        ),
        content: Text(
          '$permText 권한을 허용하지 않으면 해당 기능을 사용할 수 없습니다.\n'
          '권한을 허용하려면 아래 버튼을 다시 눌러주세요.',
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              '확인',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.main,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 기능 시작 버튼을 눌렀을 때 해당 기능에 필요한 권한만 확인한다.
  ///
  /// - 이미 허용된 권한은 건너뜀.
  /// - 미허용 권한은 시스템 팝업으로 요청 후 다시 확인.
  /// - 여전히 거부 상태이면 해당 권한에 특화된 AlertDialog를 표시하고 false 반환.
  /// - 모두 허용이면 true 반환 (다이얼로그 미표시).
  static Future<bool> requestForFeature(
    BuildContext context, {
    bool needsCamera = false,
    bool needsLocation = false,
    required String featureName,
  }) async {
    var camera = needsCamera
        ? await Permission.camera.status
        : PermissionStatus.granted;
    var location = needsLocation
        ? await Permission.locationWhenInUse.status
        : PermissionStatus.granted;

    if (needsCamera && !camera.isGranted && !camera.isPermanentlyDenied && !camera.isRestricted) {
      camera = await Permission.camera.request();
    }
    if (needsLocation && !location.isGranted && !location.isPermanentlyDenied && !location.isRestricted) {
      location = await Permission.locationWhenInUse.request();
    }

    if (!context.mounted) return false;

    final cameraDenied = needsCamera && !camera.isGranted;
    final locationDenied = needsLocation && !location.isGranted;
    if (!cameraDenied && !locationDenied) return true;

    final items = [
      if (cameraDenied) '카메라',
      if (locationDenied) '위치',
    ];
    final permText = items.join('·');

    final isPermanent =
        (cameraDenied && (camera.isPermanentlyDenied || camera.isRestricted)) ||
        (locationDenied && (location.isPermanentlyDenied || location.isRestricted));

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: ColorCollection.point, width: 2),
        ),
        title: Semantics(
          header: true,
          child: Text(
            '$permText 권한이 필요합니다',
            style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
          ),
        ),
        content: Text(
          isPermanent
              ? '$featureName을(를) 사용하려면 $permText 권한이 필요합니다.\n설정에서 권한을 허용해주세요.'
              : '$featureName을(를) 사용하려면 $permText 권한을 허용해야 합니다.',
          style: AppTextStyles.labelRegular.copyWith(color: ColorCollection.point),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              '닫기',
              style: AppTextStyles.labelBold.copyWith(color: ColorCollection.point),
            ),
          ),
          if (isPermanent)
            TextButton(
              onPressed: () {
                Navigator.pop(ctx);
                openAppSettings();
              },
              child: Text(
                '설정 열기',
                style: AppTextStyles.labelBold.copyWith(color: ColorCollection.main),
              ),
            ),
        ],
      ),
    );

    return false;
  }

  static Future<void> _showSettingsDialog(BuildContext context) async {
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: ColorCollection.background,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: const BorderSide(color: ColorCollection.point, width: 2),
        ),
        title: Text(
          '권한 설정 필요',
          style: AppTextStyles.bodyBold.copyWith(color: ColorCollection.point),
        ),
        content: Text(
          '일부 필수 권한이 거부되어 있습니다.\n'
          '설정에서 권한을 허용하면 모든 기능을 사용하실 수 있습니다.',
          style: AppTextStyles.labelRegular.copyWith(
            color: ColorCollection.point,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: Text(
              '닫기',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
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
}

// ─── 시트 본문 ────────────────────────────────────────────────────────────────

class _SheetContent extends StatefulWidget {
  final bool needsLocation;
  const _SheetContent({required this.needsLocation});

  @override
  State<_SheetContent> createState() => _SheetContentState();
}

class _SheetContentState extends State<_SheetContent> {
  bool _isRequesting = false;

  Future<void> _onTapAllow() async {
    setState(() => _isRequesting = true);

    // 시스템 팝업을 순서대로 띄운다. 각 항목 거부해도 다음으로 진행.

    var camera = await Permission.camera.status;
    if (!camera.isGranted) camera = await Permission.camera.request();

    var location = await Permission.locationWhenInUse.status;
    if (!location.isGranted) location = await Permission.locationWhenInUse.request();

    var mic = await Permission.microphone.status;
    if (!mic.isGranted && !mic.isPermanentlyDenied && !mic.isRestricted) {
      mic = await Permission.microphone.request();
    }

    var notif = await Permission.notification.status;
    if (!notif.isGranted && !notif.isPermanentlyDenied && !notif.isRestricted) {
      await Permission.notification.request();
    }

    if (!mounted) return;

    final cameraBlocked = camera.isPermanentlyDenied || camera.isRestricted;
    final locationBlocked = widget.needsLocation &&
        (location.isPermanentlyDenied || location.isRestricted);

    if (cameraBlocked || locationBlocked) {
      // 영구 거부: 설정 앱으로 안내
      await PermissionOnboardingSheet._showSettingsDialog(context);
      if (!mounted) return;
    } else {
      // 단순 거부: 시트 닫히기 전에 차단 다이얼로그로 안내
      final cameraDenied = !camera.isGranted;
      final locationDenied = widget.needsLocation && !location.isGranted;
      if (cameraDenied || locationDenied) {
        await PermissionOnboardingSheet._showDenialNotice(
          context,
          cameraDenied: cameraDenied,
          locationDenied: locationDenied,
        );
        if (!mounted) return;
      }
    }

    final granted =
        camera.isGranted && (!widget.needsLocation || location.isGranted);
    Navigator.pop(context, granted);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: ColorCollection.background,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: ColorCollection.point.withValues(alpha: 0.3),
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 28),

              Semantics(
                header: true,
                child: Text(
                  '안전한 보행 안내를 위해',
                  style: AppTextStyles.title2.copyWith(
                    color: ColorCollection.point,
                  ),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                '다음 권한이 필요합니다.',
                style: AppTextStyles.bodyRegular.copyWith(
                  color: ColorCollection.point,
                ),
              ),
              const SizedBox(height: 28),

              // ── 필수 ─────────────────────────────────────────────────────
              const _SectionLabel(label: '필수'),
              const SizedBox(height: 12),
              const _PermissionItem(
                icon: Icons.camera_alt_outlined,
                label: '카메라',
                description: '횡단보도·장애물 등 주변 환경 인식\n(장애물 탐지, 길찾기)',
              ),
              const SizedBox(height: 16),
              const _PermissionItem(
                icon: Icons.location_on_outlined,
                label: '위치',
                description: '현재 위치 기반 안전 경로 안내\n(길찾기)',
              ),
              const SizedBox(height: 24),

              // ── 선택 ─────────────────────────────────────────────────────
              const _SectionLabel(label: '선택', isOptional: true),
              const SizedBox(height: 12),
              const _PermissionItem(
                icon: Icons.mic_outlined,
                label: '마이크',
                description: '음성으로 목적지 입력\n(길찾기 음성 검색)',
                isOptional: true,
              ),
              const SizedBox(height: 16),
              const _PermissionItem(
                icon: Icons.notifications_outlined,
                label: '알림',
                description: '위험 상황 및 경로 안내 알림',
                isOptional: true,
              ),
              const SizedBox(height: 16),
              Text(
                '선택 권한을 거부해도 핵심 기능은 계속 이용할 수 있습니다.',
                style: AppTextStyles.labelRegular.copyWith(
                  color: ColorCollection.point.withValues(alpha: 0.5),
                ),
              ),
              const SizedBox(height: 28),

              Semantics(
                button: true,
                label: _isRequesting ? '권한 요청 중' : '권한 허용하기',
                excludeSemantics: true,
                child: GestureDetector(
                  onTap: _isRequesting ? null : _onTapAllow,
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    decoration: BoxDecoration(
                      color: _isRequesting
                          ? ColorCollection.main.withValues(alpha: 0.5)
                          : ColorCollection.main,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Center(
                      child: _isRequesting
                          ? const SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                                color: ColorCollection.background,
                              ),
                            )
                          : Text(
                              '권한 허용하기',
                              style: AppTextStyles.bodyBold.copyWith(
                                color: ColorCollection.background,
                              ),
                            ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ─── 섹션 레이블 ──────────────────────────────────────────────────────────────

class _SectionLabel extends StatelessWidget {
  final String label;
  final bool isOptional;

  const _SectionLabel({required this.label, this.isOptional = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: isOptional
            ? ColorCollection.point.withValues(alpha: 0.1)
            : ColorCollection.main.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: AppTextStyles.labelBold.copyWith(
          color: isOptional
              ? ColorCollection.point.withValues(alpha: 0.6)
              : ColorCollection.main,
          fontSize: 14,
        ),
      ),
    );
  }
}

// ─── 권한 항목 위젯 ───────────────────────────────────────────────────────────

class _PermissionItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final String description;
  final bool isOptional;

  const _PermissionItem({
    required this.icon,
    required this.label,
    required this.description,
    this.isOptional = false,
  });

  @override
  Widget build(BuildContext context) {
    final iconBgColor = isOptional
        ? ColorCollection.point.withValues(alpha: 0.08)
        : ColorCollection.main.withValues(alpha: 0.15);
    final iconColor = isOptional
        ? ColorCollection.point.withValues(alpha: 0.55)
        : ColorCollection.main;

    return Semantics(
      label: '$label 권한${isOptional ? ' (선택)' : ' (필수)'}: $description',
      excludeSemantics: true,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: iconBgColor,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: iconColor, size: 26),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: AppTextStyles.bodyBold.copyWith(
                    color: ColorCollection.point,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: AppTextStyles.labelRegular.copyWith(
                    color: ColorCollection.point.withValues(
                      alpha: isOptional ? 0.5 : 0.65,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}