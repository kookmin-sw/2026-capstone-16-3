class DetectionResult {
  final String label;
  final int classId;
  final double score;
  final double x1;
  final double y1;
  final double x2;
  final double y2;

  DetectionResult({
    required this.label,
    required this.classId,
    required this.score,
    required this.x1,
    required this.y1,
    required this.x2,
    required this.y2,
  });

  @override
  String toString() {
    return 'DetectionResult(label: $label, score: ${score.toStringAsFixed(3)}, '
        'box: [$x1, $y1, $x2, $y2])';
  }
}