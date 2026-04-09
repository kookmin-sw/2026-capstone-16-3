import 'package:flutter/material.dart';
import 'package:kakao_flutter_sdk_user/kakao_flutter_sdk_user.dart';
import 'package:safepath/app.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  const kakaoNativeAppKey = String.fromEnvironment('KAKAO_NATIVE_APP_KEY');
  KakaoSdk.init(nativeAppKey: kakaoNativeAppKey);

  // TODO: 키 해시 확인 후 삭제
  final keyHash = await KakaoSdk.origin;
  debugPrint('🔑 KEY HASH: $keyHash');

  runApp(const App());
}
