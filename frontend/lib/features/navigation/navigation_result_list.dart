import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/service/sound_effect_service.dart';

class ResultList extends StatefulWidget {
  final List<Map<String, dynamic>> results;
  final Function(Map<String, dynamic>) onTap;
  final VoidCallback onLoadMore;
  final bool isLoadingMore;

  const ResultList({
    super.key,
    required this.results,
    required this.onTap,
    required this.onLoadMore,
    this.isLoadingMore = false,
  });

  @override
  State<ResultList> createState() => _ResultListState();
}

class _ResultListState extends State<ResultList> {
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 80) {
      widget.onLoadMore();
    }
  }

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
        controller: _scrollController,
        shrinkWrap: true,
        physics: const BouncingScrollPhysics(),
        itemCount: widget.results.length + (widget.isLoadingMore ? 1 : 0),
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (context, index) {
          if (index == widget.results.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Center(child: CircularProgressIndicator()),
            );
          }

          final item = widget.results[index];

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
            onTap: () {
              SoundEffectService().play(SoundEffect.buttonTap);
              widget.onTap(item);
            },
          );
        },
      ),
    );
  }
}