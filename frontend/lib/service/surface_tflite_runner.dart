import 'dart:typed_data';
import 'package:flutter/services.dart';
import 'package:image/image.dart' as img;
import 'package:tflite_flutter/tflite_flutter.dart';

class SurfaceTfliteRunner {
  late final Interpreter _interpreter;
  final int inputSize;

  SurfaceTfliteRunner({this.inputSize = 640});

  Future<void> load() async {
    final options = InterpreterOptions()..threads = 4;
    _interpreter = await Interpreter.fromAsset(
      'assets/models/surface_model.tflite',
      options: options,
    );
  }

  Map<String, dynamic> getModelInfo() {
    final inputTensor = _interpreter.getInputTensor(0);
    final outputTensor0 = _interpreter.getOutputTensor(0);
    final outputTensor1 = _interpreter.getOutputTensor(1);

    return {
      'inputShape': inputTensor.shape,
      'inputType': inputTensor.type.toString(),
      'output0Shape': outputTensor0.shape,
      'output1Shape': outputTensor1.shape,
    };
  }

  Float32List _preprocess(img.Image image) {
    final resized = img.copyResize(
      image,
      width: inputSize,
      height: inputSize,
      interpolation: img.Interpolation.linear,
    );

    final buffer = Float32List(inputSize * inputSize * 3);
    int index = 0;

    for (int y = 0; y < inputSize; y++) {
      for (int x = 0; x < inputSize; x++) {
        final pixel = resized.getPixel(x, y);
        buffer[index++] = pixel.r / 255.0;
        buffer[index++] = pixel.g / 255.0;
        buffer[index++] = pixel.b / 255.0;
      }
    }
    return buffer;
  }

  Future<Map<String, dynamic>> runOnImageBytes(Uint8List bytes) async {
    final decoded = img.decodeImage(bytes);
    if (decoded == null) {
      throw Exception('이미지 디코딩 실패');
    }

    final input = _preprocess(decoded);

    // [1, H, W, 3]
    final inputTensor = input.reshape([1, inputSize, inputSize, 3]);

    final output0 = List.generate(
      1,
      (_) => List.generate(
        300,
        (_) => List.filled(38, 0.0),
      ),
    );

    final output1 = List.generate(
      1,
      (_) => List.generate(
        160,
        (_) => List.generate(
          160,
          (_) => List.filled(32, 0.0),
        ),
      ),
    );

    final outputs = <int, Object>{
      0: output0,
      1: output1,
    };

    final sw = Stopwatch()..start();
    _interpreter.runForMultipleInputs([inputTensor], outputs);
    sw.stop();

    return {
      'elapsedMs': sw.elapsedMilliseconds,
      'output0': output0,
      'output1': output1,
    };
  }

  void close() {
    _interpreter.close();
  }
}