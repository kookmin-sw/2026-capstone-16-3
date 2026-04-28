import 'package:safepath/features/navigation/navigation_step_card.dart';
import 'package:safepath/service/tts_service.dart';

class NavigationTtsService {
  static final NavigationTtsService _instance = NavigationTtsService._internal();
  factory NavigationTtsService() => _instance;
  NavigationTtsService._internal();

  // 현재 step에서 이미 읽은 임계값 추적 (step 전환 시 reset)
  final Set<String> _announcedThresholds = {};

  void reset() => _announcedThresholds.clear();

  // step 전환 시 — 다음 step 안내 (끊고 즉시 출력)
  void speakStep(DirectionType direction, String instruction) {
    reset();
    final msg = instruction.isNotEmpty
        ? instruction
        : (direction == DirectionType.straight ? '계속 직진하세요' : '${_actionText(direction)}하세요');
    TtsService().speak(msg, interrupt: true, channel: TtsChannel.navigation);
  }

  // 거리 임계값 도달 시 (≤40m → ≤20m → ≤8m)
  void speakDistance(DirectionType direction, int distance, String serverInstruction) {
    if (direction == DirectionType.straight) return;

    final action = _actionText(direction);

    if (distance <= 8 && !_announcedThresholds.contains('8')) {
      _announcedThresholds.addAll({'8', '20', '40'});
      TtsService().speak('$action하세요', interrupt: true, channel: TtsChannel.navigation);
    } else if (distance <= 20 && !_announcedThresholds.contains('20')) {
      _announcedThresholds.addAll({'20', '40'});
      TtsService().speak('잠시 후 $action하세요', channel: TtsChannel.navigation);
    } else if (distance <= 40 && !_announcedThresholds.contains('40')) {
      _announcedThresholds.add('40');
      final msg = serverInstruction.isNotEmpty ? serverInstruction : '잠시 후 $action하세요';
      TtsService().speak(msg, channel: TtsChannel.navigation);
    }
  }

  // 길안내 시작 시 overview 안내 (방향 + 예상 시간 + 첫 step)
  void speakStartOverview({
    required String? startDirection,
    required int totalTime,
    required int firstStepDistance,
    required DirectionType firstStepDirection,
    required String firstStepInstruction,
  }) {
    reset();
    final dirMsg = startDirection != null
        ? '$startDirection 방향으로 출발합니다'
        : '길안내를 시작합니다';

    final timeStr = totalTime >= 60
        ? '${totalTime ~/ 60}시간${totalTime % 60 == 0 ? '' : ' ${totalTime % 60}분'}'
        : '$totalTime분';

    final stepMsg = firstStepInstruction.isNotEmpty
        ? firstStepInstruction
        : '$firstStepDistance미터 앞에서 ${_actionText(firstStepDirection)}하세요';

    TtsService().speak(
      '$dirMsg. 약 $timeStr 소요됩니다. $stepMsg',
      interrupt: true,
      channel: TtsChannel.navigation,
    );
  }

  void speakArrival() {
    TtsService().speak('목적지에 도착했습니다', interrupt: true, channel: TtsChannel.navigation);
  }

  String _actionText(DirectionType direction) {
    return switch (direction) {
      DirectionType.left => '좌회전',
      DirectionType.right => '우회전',
      DirectionType.crosswalk => '횡단보도를 이용',
      DirectionType.straight => '직진',
    };
  }
}