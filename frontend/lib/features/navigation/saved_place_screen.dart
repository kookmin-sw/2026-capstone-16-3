import 'package:flutter/material.dart';
import 'package:safepath/common/theme/color_collection.dart';
import 'package:safepath/common/theme/text_styles.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/navigation/delete_dialog.dart';
import 'package:safepath/features/navigation/edit_button.dart';
import 'package:safepath/features/navigation/saved_place_widget.dart';
import 'package:safepath/models/saved_place.dart';
import 'package:safepath/routes/app_router.dart';
import 'package:safepath/service/place_service.dart';

class SavedPlaceScreen extends StatefulWidget {
  const SavedPlaceScreen({super.key});

  @override
  State<SavedPlaceScreen> createState() => _SavedPlaceScreenState();
}

class _SavedPlaceScreenState extends State<SavedPlaceScreen> {
  bool isEditMode = false;
  bool _isLoading = false;
  List<SavedPlace> _places = [];

  @override
  void initState() {
    super.initState();
    _loadFavorites();
  }

  Future<void> _loadFavorites() async {
    setState(() => _isLoading = true);
    try {
      final result = await PlaceService.getFavorites();
      final items = (result['items'] as List<Map<String, dynamic>>)
          .map((item) => SavedPlace.fromJSON(item))
          .toList();
      setState(() => _places = items);
    } catch (e) {
      debugPrint('🔴 즐겨찾기 로드 에러: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Widget _buildItem(int index) {
    if (isEditMode && index == _places.length) {
      return Padding(
        padding: const EdgeInsets.only(top: 10),
        child: EditButton(
          onTap: () async {
            final result = await Navigator.pushNamed(
              context,
              AppRouter.addplace,
            );

            if (result == true) {
              await _loadFavorites();
            }
          },
        ),
      );
    }

    final place = _places[index];

    return SavedPlaceWidget(
      label: place.label,
      location: place.location,
      category: place.category,
      isEditMode: isEditMode,
      onTap: () {
        if (!isEditMode) {
          Navigator.pop(context, place);
        }
      },
      onDelete: () {
        showDialog(
          context: context,
          builder: (context) => DeleteDialog(
            onDelete: () async {
              final success = await PlaceService.deleteFavorite(id: place.id);
              if (success) {
                setState(() {
                  _places.removeAt(index);
                });
              }
            },
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CustomTitleBar(title: '저장된 장소', showBackButton: true),
      body: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
          child: Column(
            children: [
              Expanded(
                child: _isLoading
                    ? Center(
                        child: Semantics(
                          label: '로딩 중',
                          child: const CircularProgressIndicator(),
                        ),
                      )
                    : _places.isEmpty && !isEditMode
                    ? Center(
                        child: Text(
                          '저장된 장소가 없습니다',
                          style: AppTextStyles.labelRegular.copyWith(
                            color: ColorCollection.point,
                          ),
                        ),
                      )
                    : ListView.separated(
                        padding: const EdgeInsets.only(bottom: 100),
                        itemCount: _places.length + (isEditMode ? 1 : 0),
                        itemBuilder: (context, index) => _buildItem(index),
                        separatorBuilder: (context, index) {
                          return const SizedBox(height: 21);
                        },
                      ),
              ),
              SafeArea(
                child: Padding(
                  padding: const EdgeInsets.only(top: 16, bottom: 24),
                  child: ActionButton(
                    label: isEditMode ? '저장하기' : '편집하기',
                    icon: isEditMode
                        ? Icons.check_circle_outline_rounded
                        : Icons.edit,
                    onTap: () {
                      setState(() {
                        isEditMode = !isEditMode;
                      });
                    },
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
