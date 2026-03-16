package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class PlaceCache {

    /**
     * “상세 조회”는 Kakao Local REST에 공식적인 placeId 단건 조회 API가 없어서,
     * 검색/nearby로 받은 결과를 짧게 캐싱해서 placeId로 재조회한다.
     */
    private static final int MAX_SIZE = 10_000;
    private static final Duration TTL = Duration.ofMinutes(10);

    private final Cache<String, KakaoCategorySearchResponse.KakaoPlaceDocument> cache =
            Caffeine.newBuilder()
                    .maximumSize(MAX_SIZE)
                    .expireAfterWrite(TTL)
                    .build();

    public void put(String placeId, KakaoCategorySearchResponse.KakaoPlaceDocument doc) {
        if (placeId == null || placeId.isBlank() || doc == null) {
            return;
        }
        cache.put(placeId, doc);
    }

    public Optional<KakaoCategorySearchResponse.KakaoPlaceDocument> get(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.getIfPresent(placeId));
    }

    public void evict(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return;
        }
        cache.invalidate(placeId);
    }

    public void clear() {
        cache.invalidateAll();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }
}