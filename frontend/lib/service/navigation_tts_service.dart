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

  // 길안내 시작 시 overview 안내: 총 N분 → 방향 출발 → N미터 앞에서 ~
  void speakStartOverview({
    required String? startDirection,
    required int totalTime,
    required int firstStepDistance,
    required DirectionType firstStepDirection,
    required String firstStepInstruction,
  }) {
    reset();
    // 현재 거리 내 임계값 마킹 → overview 후 speakDistance 중복 방지
    _markDistanceAnnounced(firstStepDistance);

    final timeStr = totalTime >= 60
        ? '${totalTime ~/ 60}시간${totalTime % 60 == 0 ? '' : ' ${totalTime % 60}분'}'
        : '$totalTime분';

    final dirMsg = startDirection != null
        ? '$startDirection 방향으로 출발하세요'
        : '길안내를 시작합니다';

    TtsService().speak(
      '총 $timeStr 소요됩니다. $dirMsg. $firstStepDistance미터 앞에서 ${_actionText(firstStepDirection)}하세요',
      interrupt: true,
      channel: TtsChannel.navigation,
    );
  }

  // overview 종료 후 step[0] 서버 안내문 자동 재생 (큐 삽입)
  void speakAfterOverview(String instruction) {
    if (instruction.isEmpty) return;
    // _isSpeaking = true (overview 재생 중) → pending 큐에 삽입 → overview 완료 시 자동 재생
    TtsService().speak(instruction, interrupt: false, channel: TtsChannel.navigation);
  }

  void _markDistanceAnnounced(int distance) {
    if (distance <= 8) {
      _announcedThresholds.addAll({'8', '20', '40'});
    } else if (distance <= 20) {
      _announcedThresholds.addAll({'20', '40'});
    } else if (distance <= 40) {
      _announcedThresholds.add('40');
    }
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