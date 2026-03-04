package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlaceCache {

    /**
     * “상세 조회”는 Kakao Local REST에 공식적인 placeId 단건 조회 API가 없어서,
     * 검색/nearby로 받은 결과를 짧게 캐싱해서 placeId로 재조회한다.
     */
    private static final long DEFAULT_TTL_MILLIS = 10 * 60 * 1000L; // 10분
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public void put(String placeId, KakaoCategorySearchResponse.KakaoPlaceDocument doc) {
        if (placeId == null || doc == null) return;
        store.put(placeId, new Entry(doc, System.currentTimeMillis()));
    }

    public Optional<KakaoCategorySearchResponse.KakaoPlaceDocument> get(String placeId) {
        Entry e = store.get(placeId);
        if (e == null) return Optional.empty();

        long age = System.currentTimeMillis() - e.savedAtMillis();
        if (age > DEFAULT_TTL_MILLIS) {
            store.remove(placeId);
            return Optional.empty();
        }
        return Optional.of(e.doc());
    }

    private record Entry(
            KakaoCategorySearchResponse.KakaoPlaceDocument doc,
            long savedAtMillis
    ) {}
}