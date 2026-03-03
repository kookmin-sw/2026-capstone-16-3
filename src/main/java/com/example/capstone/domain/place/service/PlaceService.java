package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.NearbyPlaceResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaceService {

    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);
    private static final int MAX_PAGE = 45;

    private final KakaoLocalClient kakaoLocalClient;

    public PlaceService(KakaoLocalClient kakaoLocalClient) {
        this.kakaoLocalClient = kakaoLocalClient;
    }

    /**
     * NOTE:
     * - 이 메서드는 "전체 정렬 리스트"를 만들어 반환합니다.
     * - pagination(page/size)은 Controller에서 슬라이싱해서 처리하세요.
     */
    public List<NearbyPlaceResponse> findNearbyByCategoryCodes(
            double lat,
            double lng,
            int radiusM,
            List<String> categoryCodes,
            int sizePerCategory,
            int maxItems
    ) {
        // (카테고리 코드별) 카카오 페이지를 끝까지(최대 45) 당겨서 합치기
        List<NearbyPlaceResponse> merged = Flux.fromIterable(categoryCodes)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .flatMap(code -> fetchAllPagesByCategory(code, lat, lng, radiusM, sizePerCategory))
                .flatMapIterable(resp -> resp.documents() == null ? List.of() : resp.documents())
                .map(this::toNearbyPlace)
                .collectList()
                .blockOptional()
                .orElse(List.of());

        // id 기준 중복 제거
        Map<String, NearbyPlaceResponse> dedup = new LinkedHashMap<>();
        for (NearbyPlaceResponse p : merged) {
            if (p != null && p.id() != null) {
                dedup.putIfAbsent(p.id(), p);
            }
        }

        // ✅ 거리 보정 + 반경 필터 + 정렬 안정성(distance 같으면 id tie-break)
        List<NearbyPlaceResponse> ranked = dedup.values().stream()
                .map(p -> p.distanceM() == null ? withDistance(p, lat, lng) : p)
                .filter(p -> p.distanceM() != null && p.distanceM() <= radiusM)
                .sorted(
                        Comparator.comparingLong(NearbyPlaceResponse::distanceM)
                                .thenComparing(NearbyPlaceResponse::id)
                )
                .toList();

        // (기존 호환용) maxItems 제한
        if (maxItems > 0 && ranked.size() > maxItems) {
            return ranked.subList(0, maxItems);
        }
        return ranked;
    }

    private Flux<KakaoCategorySearchResponse> fetchAllPagesByCategory(
            String code,
            double lat,
            double lng,
            int radiusM,
            int sizePerCategory
    ) {
        return Flux.range(1, MAX_PAGE)
                .concatMap(page ->
                        kakaoLocalClient.searchCategory(code, lat, lng, radiusM, sizePerCategory, page)
                                .doOnSubscribe(s -> log.info(
                                        "Kakao category start code={}, page={}, lat={}, lng={}, radius={}",
                                        code, page, lat, lng, radiusM
                                ))
                                .doOnNext(resp -> log.info(
                                        "Kakao category ok code={}, page={}, total={}, docs={}, isEnd={}",
                                        code,
                                        page,
                                        resp.meta() == null ? -1 : resp.meta().totalCount(),
                                        resp.documents() == null ? 0 : resp.documents().size(),
                                        resp.meta() != null && resp.meta().isEnd()
                                ))
                                .doOnError(e -> log.error("Kakao category fail code={}, page={}", code, page, e))
                                .onErrorResume(e -> Mono.<KakaoCategorySearchResponse>empty())
                )
                // is_end=true 응답을 받는 순간 이후 페이지 호출 중단
                .takeUntil(resp -> resp.meta() != null && resp.meta().isEnd());
    }

    private NearbyPlaceResponse toNearbyPlace(KakaoCategorySearchResponse.KakaoPlaceDocument d) {
        double placeLng = Double.parseDouble(d.x());
        double placeLat = Double.parseDouble(d.y());
        Long dist = (d.distance() == null || d.distance().isBlank()) ? null : Long.parseLong(d.distance());

        return new NearbyPlaceResponse(
                d.id(),
                d.placeName(),
                placeLat,
                placeLng,
                dist,
                d.categoryName(),
                d.roadAddressName(),
                d.addressName(),
                d.phone(),
                d.placeUrl(),
                "KAKAO"
        );
    }

    private NearbyPlaceResponse withDistance(NearbyPlaceResponse p, double originLat, double originLng) {
        long d = haversineMeters(originLat, originLng, p.lat(), p.lng());
        return new NearbyPlaceResponse(
                p.id(), p.name(), p.lat(), p.lng(), d,
                p.category(), p.roadAddress(), p.jibunAddress(),
                p.phone(), p.placeUrl(), p.provider()
        );
    }

    private static long haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (long) (R * c);
    }
}