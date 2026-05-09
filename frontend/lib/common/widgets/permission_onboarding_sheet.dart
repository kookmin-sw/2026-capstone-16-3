import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

/// 서비스 단위 권한 설명 시트
///
/// "안전한 보행 안내" 맥락으로 카메라·위치(필수)·마이크·알림(선택) 권한을
/// 한 번에 설명하고 요청한다.
/// 필수 권한(카메라·위치)이 이미 허용된 경우 시트를 표시하지 않고 즉시 true를 반환한다.
class PermissionOnboardingSheet {
  const PermissionOnboardingSheet._();

  /// 필수 권한(카메라·위치)이 모두 허용됐으면 즉시 true 반환 (시트 미표시).
  /// 하나라도 미허용이면 설명 시트를 표시하고 최종 허용 여부를 반환한다.
  static Future<bool> show(BuildContext context) async {
    final camera = await Permission.camera.status;
    final location = await Permission.locationWhenInUse.status;

    if (camera.isGranted && location.isGranted) return true;

    if (!context.mounted) return false;

    // 필수 권한 영구 거부 / 기기 관리(MDM) → 시트 없이 바로 설정 안내
    final blocked = camera.isPermanentlyDenied ||
        camera.isRestricted ||
        location.isPermanentlyDenied ||
        location.isRestricted;
    if (blocked) {
      await _showSettingsDialog(context);
      return false;
    }

    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const _SheetContent(),
    );
    return result ?? false;
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
          '보행 안내 기능을 사용하려면 카메라·위치 권한이 필요합니다.\n'
          '설정에서 권한을 허용한 후 다시 시도해 주세요.',
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
  const _SheetContent();

  @override
  State<_SheetContent> createState() => _SheetContentState();
}

class _SheetContentState extends State<_SheetContent> {
  bool _isRequesting = false;
  String? _errorMessage;

  Future<void> _onTapAllow() async {
    setState(() {
      _isRequesting = true;
      _errorMessage = null;
    });

    // ── 필수 권한 요청 ──────────────────────────────────────────────────────
    var camera = await Permission.camera.status;
    if (!camera.isGranted) camera = await Permission.camera.request();

    var location = await Permission.locationWhenInUse.status;
    if (!location.isGranted) location = await Permission.locationWhenInUse.request();

    if (!mounted) return;

    final requiredGranted = camera.isGranted && location.isGranted;

    if (!requiredGranted) {
      final permanentlyBlocked = camera.isPermanentlyDenied ||
          camera.isRestricted ||
          location.isPermanentlyDenied ||
          location.isRestricted;
      if (permanentlyBlocked) {
        setState(() => _isRequesting = false);
        await PermissionOnboardingSheet._showSettingsDialog(context);
        if (mounted) Navigator.pop(context, false);
      } else {
        setState(() {
          _isRequesting = false;
          _errorMessage = '카메라·위치 권한이 거부되었습니다.\n버튼을 다시 눌러 권한을 허용해 주세요.';
        });
      }
      return;
    }

    // ── 선택 권한 요청 (거부해도 서비스 이용 가능) ──────────────────────────
    final mic = await Permission.microphone.status;
    if (!mic.isGranted && !mic.isPermanentlyDenied && !mic.isRestricted) {
      await Permission.microphone.request();
    }

    final notif = await Permission.notification.status;
    if (!notif.isGranted && !notif.isPermanentlyDenied && !notif.isRestricted) {
      await Permission.notification.request();
    }

    if (!mounted) return;
    Navigator.pop(context, true);
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
              // 드래그 핸들
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

              // 제목
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

              // ── 필수 섹션 ────────────────────────────────────────────────
              _SectionLabel(label: '필수'),
              const SizedBox(height: 12),
              _PermissionItem(
                icon: Icons.camera_alt_outlined,
                label: '카메라',
                description: '횡단보도·장애물 등 주변 환경 인식\n(장애물 탐지, 길찾기)',
              ),
              const SizedBox(height: 16),
              _PermissionItem(
                icon: Icons.location_on_outlined,
                label: '위치',
                description: '현재 위치 기반 안전 경로 안내\n(길찾기)',
              ),
              const SizedBox(height: 24),

              // ── 선택 섹션 ────────────────────────────────────────────────
              _SectionLabel(label: '선택', isOptional: true),
              const SizedBox(height: 12),
              _PermissionItem(
                icon: Icons.mic_outlined,
                label: '마이크',
                description: '음성으로 목적지 입력\n(길찾기 음성 검색)',
                isOptional: true,
              ),
              const SizedBox(height: 16),
              _PermissionItem(
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

              // 오류 메시지 (필수 권한 거부 후 재시도 안내)
              if (_errorMessage != null) ...[
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  decoration: BoxDecoration(
                    color: ColorCollection.point.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(
                      color: ColorCollection.point.withValues(alpha: 0.25),
                    ),
                  ),
                  child: Text(
                    _errorMessage!,
                    style: AppTextStyles.labelRegular.copyWith(
                      color: ColorCollection.point,
                    ),
                  ),
                ),
              ],

              const SizedBox(height: 28),

              // 권한 허용 버튼
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