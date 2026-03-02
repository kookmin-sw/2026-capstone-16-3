package com.example.capstone.domain.place.service;

import com.example.capstone.domain.place.dto.response.NearbyPlaceResponse;
import com.example.capstone.domain.place.dto.response.kakao.KakaoCategorySearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;


@Service
public class PlaceService {

    private final KakaoLocalClient kakaoLocalClient;
    private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

    public PlaceService(KakaoLocalClient kakaoLocalClient) {
        this.kakaoLocalClient = kakaoLocalClient;
    }

    public List<NearbyPlaceResponse> findNearbyByCategoryCodes(
            double lat,
            double lng,
            int radiusM,
            List<String> categoryCodes,
            int sizePerCategory,
            int maxItems
    ) {
        // 병렬 호출 + 실패는 무시(빈 결과로 대체)
        List<NearbyPlaceResponse> merged = Flux.fromIterable(categoryCodes)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .flatMap(code ->
                        Flux.range(1, 3) // size=15면 최대 45개까지
                                .concatMap(page -> kakaoLocalClient.searchCategory(code, lat, lng, radiusM, sizePerCategory, page)
                                        .doOnSubscribe(s -> log.info("Kakao start code={}, page={}, lat={}, lng={}, radius={}",
                                                code, page, lat, lng, radiusM))
                                        .doOnNext(resp -> log.info("Kakao ok code={}, page={}, total={}, docs={}",
                                                code, page,
                                                resp.meta() == null ? -1 : resp.meta().totalCount(),
                                                resp.documents() == null ? 0 : resp.documents().size()))
                                        .doOnError(e -> log.error("Kakao fail code={}, page={}", code, page, e))
                                        .onErrorResume(e -> Mono.empty())
                                )
                                .takeUntil(resp -> resp.meta() != null && resp.meta().isEnd())
                )
                .flatMapIterable(resp -> resp.documents() == null ? List.of() : resp.documents())
                .map(doc -> toNearbyPlace(doc))
                .collectList()
                .blockOptional()
                .orElse(List.of());

        // id 기준 중복 제거(공급자 여러 호출 합쳐지므로)
        Map<String, NearbyPlaceResponse> dedup = new LinkedHashMap<>();
        for (NearbyPlaceResponse p : merged) {
            dedup.putIfAbsent(p.id(), p);
        }

        // 거리(distance)가 null이면 직접 계산(혹시 대비)
        List<NearbyPlaceResponse> ranked = dedup.values().stream()
                .map(p -> p.distanceM() == null ? withDistance(p, lat, lng) : p)
                .filter(p -> p.distanceM() != null && p.distanceM() <= radiusM) // ✅ 반경 필터
                .sorted(Comparator.comparingLong(NearbyPlaceResponse::distanceM))
                .limit(maxItems)
                .toList();

        return ranked;
    }

    private NearbyPlaceResponse toNearbyPlace(KakaoCategorySearchResponse.KakaoPlaceDocument d) {
        double lng = Double.parseDouble(d.x());
        double lat = Double.parseDouble(d.y());
        Long dist = (d.distance() == null || d.distance().isBlank()) ? null : Long.parseLong(d.distance());

        return new NearbyPlaceResponse(
                d.id(),
                d.placeName(),
                lat,
                lng,
                dist,
                d.categoryName(),
                d.roadAddressName(),
                d.addressName(),
                d.phone(),
                d.placeUrl(),
                "KAKAO"
        );
    }

    private NearbyPlaceResponse withDistance(NearbyPlaceResponse p, double lat, double lng) {
        long d = haversineMeters(lat, lng, p.lat(), p.lng());
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
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return (long) (R * c);
    }
}