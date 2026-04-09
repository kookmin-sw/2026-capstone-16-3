import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

import 'package:safepath/model/detection_event.dart';
import 'package:safepath/service/token_storage.dart';

/// =======================================================
/// DetectionWsService
///
/// 역할:
///   - STOMP over WebSocket으로 서버에 연결
///   - /user/queue/guide 구독 → DetectionEvent 수신
///   - DetectionScreen에서 connect() / disconnect() 호출
///
/// 백엔드:
///   - endpoint  : ws://host/ws/websocket  (withSockJS() → raw WS는 /websocket 경로)
///   - subscribe : /user/queue/guide
///   - 인증      : HTTP 업그레이드 헤더에 Authorization 전달
/// =======================================================
class DetectionWsService {
  static final DetectionWsService _instance = DetectionWsService._internal();
  factory DetectionWsService() => _instance;
  DetectionWsService._internal();

  static const String _baseUrl = String.fromEnvironment('BASE_URL');
  static const String _wsPath = '/ws/websocket';
  static const String _subscribeDest = '/user/queue/guide';

  StompClient? _client;
  StreamController<DetectionEvent>? _controller;

  VoidCallback? _onConnectedCallback;
  VoidCallback? _onDisconnectedCallback;

  bool get isConnected => _client?.connected ?? false;

  Stream<DetectionEvent>? get eventStream => _controller?.stream;

  // ─── 연결 ─────────────────────────────────────────────────────────────────

  Future<void> connect({
    VoidCallback? onConnected,
    VoidCallback? onDisconnected,
  }) async {
    if (_client?.connected == true) return;

    _onConnectedCallback = onConnected;
    _onDisconnectedCallback = onDisconnected;

    final wsUrl = _baseUrl.replaceFirst(RegExp(r'^http'), 'ws') + _wsPath;
    final accessToken = await TokenStorage().accessToken;

    _controller = StreamController<DetectionEvent>.broadcast();

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        // webSocketConnectHeaders: HTTP 업그레이드 요청 헤더 → Spring Security가 이 단계에서 인증
        // stompConnectHeaders: STOMP CONNECT 프레임 헤더 → 업그레이드 이후 전달되어 인증 불가
        webSocketConnectHeaders: {
          if (accessToken != null) 'Authorization': 'Bearer $accessToken',
        },
        stompConnectHeaders: const {},
        onConnect: _onConnect,
        onDisconnect: (_) {
          debugPrint('🟡 [WS] 연결 해제');
          _onDisconnectedCallback?.call();
        },
        onStompError: (frame) {
          debugPrint('🔴 [WS] STOMP 에러: ${frame.body}');
          _onDisconnectedCallback?.call();
        },
        onWebSocketError: (e) {
          debugPrint('🔴 [WS] WebSocket 에러: $e');
          _onDisconnectedCallback?.call();
        },
        onDebugMessage: (msg) {
          if (kDebugMode) debugPrint('🔵 [WS] $msg');
        },
      ),
    );

    _client!.activate();
    debugPrint('🟢 [WS] 연결 시도: $wsUrl');
  }

  // ─── 해제 ─────────────────────────────────────────────────────────────────

  Future<void> disconnect() async {
    _onConnectedCallback = null;
    _onDisconnectedCallback = null;

    _client?.deactivate();
    _client = null;

    if (_controller != null && !_controller!.isClosed) {
      await _controller!.close();
    }
    _controller = null;

    debugPrint('🟡 [WS] 연결 종료');
  }

  // ─── 내부 ─────────────────────────────────────────────────────────────────

  void _onConnect(StompFrame frame) {
    debugPrint('🟢 [WS] STOMP 연결 완료 → $_subscribeDest 구독');
    _onConnectedCallback?.call();

    _client!.subscribe(destination: _subscribeDest, callback: _onMessage);
  }

  void _onMessage(StompFrame frame) {
    if (frame.body == null) return;

    try {
      final json = jsonDecode(frame.body!) as Map<String, dynamic>;
      if (json['status'] == 'success') {
        _controller?.add(DetectionEvent.fromJson(json));
        debugPrint(
          '🟢 [WS] 수신: ${json['primaryObjectClass']} / ${json['clockDirection']}',
        );
      }
    } catch (e) {
      debugPrint('🔴 [WS] 파싱 오류: $e / body: ${frame.body}');
    }
  }
}
