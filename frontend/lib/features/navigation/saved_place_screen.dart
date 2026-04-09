import 'package:flutter/material.dart';
import 'package:safepath/common/widgets/action_button_widget.dart';
import 'package:safepath/common/widgets/title_bar_widget.dart';
import 'package:safepath/features/navigation/delete_dialog.dart';
import 'package:safepath/features/navigation/edit_button.dart';
import 'package:safepath/features/navigation/saved_place_widget.dart';
import 'package:safepath/data/saved_place_dummy.dart';
import 'package:safepath/models/saved_place.dart';
import 'package:safepath/routes/app_router.dart';

class SavedPlaceScreen extends StatefulWidget {
  const SavedPlaceScreen({super.key});

  @override
  State<SavedPlaceScreen> createState() => _SavedPlaceScreenState();
}

class _SavedPlaceScreenState extends State<SavedPlaceScreen> {
  bool isEditMode = false;

  Widget _buildItem(int index) {
    if (isEditMode && index == savedPlaces.length) {
      return Padding(
        padding: EdgeInsets.only(top: 10),
        child: EditButton(
          onTap: () async {
            final result = await Navigator.pushNamed(
              context,
              AppRouter.addplace,
            );

            if (result != null) {
              setState(() {
                savedPlaces.add(result as SavedPlace);
              });
            }
          },
        ),
      );
    }
    final place = savedPlaces[index];

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
            onDelete: () {
              setState(() {
                savedPlaces.removeAt(index);
              });
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
                child: ListView.separated(
                  padding: const EdgeInsets.only(bottom: 100),
                  itemCount: savedPlaces.length + (isEditMode ? 1 : 0),
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
