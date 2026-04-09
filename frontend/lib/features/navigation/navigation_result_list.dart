import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';

class ResultList extends StatelessWidget {
  final List<Map<String, dynamic>> results;
  final Function(Map<String, dynamic>) onTap;

  const ResultList({super.key, required this.results, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        color: ColorCollection.background,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: ColorCollection.point, width: 2),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      constraints: const BoxConstraints(maxHeight: 600),
      child: ListView.separated(
        shrinkWrap: true,
        physics: const BouncingScrollPhysics(),
        itemCount: results.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final item = results[index];

          return ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 16),
            title: Text(
              item['name'] ?? '',
              style: AppTextStyles.labelBold.copyWith(
                color: ColorCollection.point,
              ),
            ),
            subtitle: Text(
              item['roadAddress'] ?? '',
              style: AppTextStyles.labelRegular.copyWith(
                color: ColorCollection.point,
              ),
            ),
            trailing: Text(
              item['distance'] == null
                  ? ''
                  : (item['distance'] < 1000
                        ? '${item['distance']}m'
                        : '${(item['distance'] / 1000).toStringAsFixed(1)}km'),
              style: AppTextStyles.labelRegular.copyWith(
                color: ColorCollection.point,
              ),
            ),
            onTap: () => onTap(item),
          );
        },
      ),
    );
  }
}
