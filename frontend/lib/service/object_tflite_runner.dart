import 'dart:typed_data';
import 'package:image/image.dart' as img;
import 'package:tflite_flutter/tflite_flutter.dart';
import 'package:safepath/model/detection_result.dart';

/// YOLOv11 detection TFLite 런너
///
/// 출력 형식: [1, 4+nc, 8400]
///   dim 1 [0..3]    = cx, cy, w, h (정규화 0~1)
///   dim 1 [4..4+nc] = 클래스별 confidence score
///   dim 2 [0..8399] = anchor point 수 (80×80 + 40×40 + 20×20)
///
/// NMS 없이 클래스별 최고 점수 1개만 반환한다.
class ObjectTfliteRunner {
  final int inputSize;
  final List<String> labels;
  final double scoreThreshold;

  late final Interpreter _interpreter;

  ObjectTfliteRunner({
    this.inputSize = 640,
    required this.labels,
    this.scoreThreshold = 0.25,
  });

  Future<void> load() async {
    final options = InterpreterOptions()..threads = 4;
    _interpreter = await Interpreter.fromAsset(
      'assets/models/object_model.tflite',
      options: options,
    );
  }

  Map<String, dynamic> getModelInfo() {
    final inputTensor = _interpreter.getInputTensor(0);
    final outputTensor = _interpreter.getOutputTensor(0);
    return {
      'inputShape': inputTensor.shape,
      'inputType': inputTensor.type.toString(),
      'outputShape': outputTensor.shape,
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
    if (decoded == null) throw Exception('이미지 디코딩 실패');

    final input = _preprocess(decoded);
    final inputTensor = input.reshape([1, inputSize, inputSize, 3]);

    final shape = _interpreter.getOutputTensor(0).shape;
    final numChannels = shape[1]; // 4 + nc
    final numAnchors = shape[2];  // 8400

    final output = List.generate(
      1,
      (_) => List.generate(
        numChannels,
        (_) => List.filled(numAnchors, 0.0),
      ),
    );

    final outputs = <int, Object>{0: output};

    final sw = Stopwatch()..start();
    _interpreter.runForMultipleInputs([inputTensor], outputs);
    sw.stop();

    final detections = _parseOutput(output[0] as List, numAnchors, numChannels);

    return {
      'elapsedMs': sw.elapsedMilliseconds,
      'detections': detections,
    };
  }

  // 클래스별 최고 점수 anchor 1개만 선택 (greedy dedup)
  List<DetectionResult> _parseOutput(
    List<dynamic> out,
    int numAnchors,
    int numChannels,
  ) {
    final nc = numChannels - 4;
    final Map<int, DetectionResult> best = {};

    for (int i = 0; i < numAnchors; i++) {
      double bestScore = scoreThreshold;
      int bestClass = -1;

      for (int c = 0; c < nc; c++) {
        final score = ((out[4 + c] as List)[i] as num).toDouble();
        if (score > bestScore) {
          bestScore = score;
          bestClass = c;
        }
      }

      if (bestClass < 0) continue;

      final existing = best[bestClass];
      if (existing != null && existing.score >= bestScore) continue;

      final cx = ((out[0] as List)[i] as num).toDouble();
      final cy = ((out[1] as List)[i] as num).toDouble();
      final w  = ((out[2] as List)[i] as num).toDouble();
      final h  = ((out[3] as List)[i] as num).toDouble();

      final x1 = (cx - w / 2).clamp(0.0, 1.0);
      final y1 = (cy - h / 2).clamp(0.0, 1.0);
      final x2 = (cx + w / 2).clamp(0.0, 1.0);
      final y2 = (cy + h / 2).clamp(0.0, 1.0);

      final label = bestClass < labels.length ? labels[bestClass] : 'unknown';

      best[bestClass] = DetectionResult(
        label: label,
        classId: bestClass,
        score: bestScore,
        x1: x1,
        y1: y1,
        x2: x2,
        y2: y2,
      );
    }

    return best.values.toList()
      ..sort((a, b) => b.score.compareTo(a.score));
  }

  void close() {
    _interpreter.close();
  }
}