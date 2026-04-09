import 'package:safepath/models/saved_place.dart';
import 'package:safepath/common/enum/place_category.dart';

final savedPlaces = [
  SavedPlace(
    label: '집',
    location: '서울특별시 성북구 솔샘로8길',
    category: PlaceCategory.home,
  ),
  SavedPlace(
    label: '학교',
    location: '서울특별시 성북구 정릉로 77',
    category: PlaceCategory.school,
  ),
  SavedPlace(
    label: '회사',
    location: '서울특별시 성북구 보국문로 10',
    category: PlaceCategory.work,
  ),
  SavedPlace(
    label: '병원',
    location: '서울특별시 성북구 보국문로 92',
    category: PlaceCategory.hospital,
  ),
  SavedPlace(
    label: '카페',
    location: '서울특별시 성북구 아리랑로 2',
    category: PlaceCategory.restaurant,
  ),
  SavedPlace(
    label: '마트',
    location: '서울특별시 성북구 정릉로 5',
    category: PlaceCategory.mart,
  ),
];
