import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/routes/app_router.dart';

class App extends StatelessWidget {
  const App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SafePath',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        fontFamily: 'NanumSquareNeo',
        colorScheme: ColorScheme.fromSeed(seedColor: ColorCollection.main),
        scaffoldBackgroundColor: ColorCollection.background,
      ),
      initialRoute: AppRouter.signin,
      onGenerateRoute: AppRouter.generateRoute,
    );
  }
}
