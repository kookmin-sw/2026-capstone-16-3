package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.entity.AcousticSignal;
import com.example.capstone.domain.crosswalk.enums.DataSourceType;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.domain.crosswalk.repository.AcousticSignalRepository;
import com.example.capstone.domain.crosswalk.service.sync.PublicDataCollector;
import com.example.capstone.global.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class SeoulAcousticSignalCollector implements PublicDataCollector {

    private static final String SERVICE_NAME = "trafficSafetyA073PInfo";
    private static final int PAGE_SIZE = 100;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final AcousticSignalRepository acousticSignalRepository;

    public SeoulAcousticSignalCollector(
            @Qualifier("opendataWebClient") WebClient webClient,
            AcousticSignalRepository acousticSignalRepository
    ) {
        this.webClient = webClient;
        this.acousticSignalRepository = acousticSignalRepository;
    }

    @Value("${opendata.app-key}")
    private String seoulOpenDataAppKey;

    @Value("${opendata.base-url:http://openapi.seoul.go.kr:8088}")
    private String seoulOpenDataBaseUrl;

    @Override
    public void collectAndSave() {
        int startIndex = 1;
        int endIndex = PAGE_SIZE;
        int totalCount = Integer.MAX_VALUE;

        while (startIndex <= totalCount) {
            JsonNode serviceNode = requestServiceNode(startIndex, endIndex);

            String resultCode = serviceNode.path("RESULT").path("CODE").asText();
            String resultMessage = serviceNode.path("RESULT").path("MESSAGE").asText();

            if ("INFO-200".equals(resultCode)) {
                log.info("[SEOUL ACOUSTIC SIGNAL COLLECT] no more data. startIndex={}, endIndex={}", startIndex, endIndex);
                break;
            }

            if (!"INFO-000".equals(resultCode)) {
                log.error("[SEOUL ACOUSTIC SIGNAL COLLECT] open api error. code={}, message={}", resultCode, resultMessage);
                throw new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
            }

            totalCount = serviceNode.path("list_total_count").asInt(0);
            JsonNode rows = serviceNode.path("row");

            if (!rows.isArray() || rows.isEmpty()) {
                log.info("[SEOUL ACOUSTIC SIGNAL COLLECT] empty rows. startIndex={}, endIndex={}", startIndex, endIndex);
                break;
            }

            int savedCount = 0;
            for (JsonNode row : rows) {
                if (upsertAcousticSignal(row)) {
                    savedCount++;
                }
            }

            log.info(
                    "[SEOUL ACOUSTIC SIGNAL COLLECT] fetched range={}~{}, saved={}, totalCount={}",
                    startIndex, endIndex, savedCount, totalCount
            );

            startIndex += PAGE_SIZE;
            endIndex = startIndex + PAGE_SIZE - 1;
        }
    }

    private JsonNode requestServiceNode(int startIndex, int endIndex) {
        String uri = String.format(
                "%s/%s/json/%s/%d/%d",
                seoulOpenDataBaseUrl,
                seoulOpenDataAppKey,
                SERVICE_NAME,
                startIndex,
                endIndex
        );

        JsonNode root = webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[SEOUL ACOUSTIC SIGNAL 4XX] uri={}, status={}, body={}",
                                            uri, response.statusCode(), body);
                                    return Mono.error(new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_4XX));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[SEOUL ACOUSTIC SIGNAL 5XX] uri={}, status={}, body={}",
                                            uri, response.statusCode(), body);
                                    return Mono.error(new BusinessException(CrosswalkErrorCode.CROSSWALK_API_HTTP_5XX));
                                })
                )
                .bodyToMono(JsonNode.class)
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(TimeoutException.class, e -> {
                    log.error("[SEOUL ACOUSTIC SIGNAL TIMEOUT] uri={}", uri, e);
                    return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_TIMEOUT);
                })
                .onErrorMap(WebClientRequestException.class, e -> {
                    log.error("[SEOUL ACOUSTIC SIGNAL CONNECTION ERROR] uri={}", uri, e);
                    return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_CONNECTION_ERROR);
                })
                .onErrorMap(
                        e -> !(e instanceof BusinessException),
                        e -> {
                            log.error("[SEOUL ACOUSTIC SIGNAL UNKNOWN ERROR] uri={}", uri, e);
                            return new BusinessException(CrosswalkErrorCode.CROSSWALK_API_ERROR);
                        }
                )
                .block();

        if (root == null || root.path(SERVICE_NAME).isMissingNode()) {
            log.error("[SEOUL ACOUSTIC SIGNAL INVALID RESPONSE] uri={}, root={}", uri, root);
            throw new BusinessException(CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE);
        }

        return root.path(SERVICE_NAME);
    }

    private boolean upsertAcousticSignal(JsonNode row) {
        String acousticSignalCode = firstText(
                row,
                "SUD_SGN_MNG_NO1",
                "음향신호관리번호",
                "MGRNU"
        );

        if (!hasText(acousticSignalCode)) {
            log.warn("[SEOUL ACOUSTIC SIGNAL SKIP] missing acoustic signal code. fields={}, row={}",
                    fieldNames(row), row);
            return false;
        }

        Double x = firstDouble(
                row,
                "XCRD",
                "X좌표",
                "XCE"
        );

        Double y = firstDouble(
                row,
                "YCRD",
                "Y좌표",
                "YCE"
        );

        if (x == null || y == null) {
            log.warn(
                    "[SEOUL ACOUSTIC SIGNAL SKIP] empty or unreadable coordinates. code={}, fields={}, XCRD={}, YCRD={}, X좌표={}, Y좌표={}, row={}",
                    acousticSignalCode,
                    fieldNames(row),
                    text(row, "XCRD"),
                    text(row, "YCRD"),
                    text(row, "X좌표"),
                    text(row, "Y좌표"),
                    row
            );
            return false;
        }

        Wgs84Point point = convertSeoulTmToWgs84(x, y);
        if (point == null) {
            log.warn("[SEOUL ACOUSTIC SIGNAL SKIP] coordinate transform failed. code={}, x={}, y={}",
                    acousticSignalCode, x, y);
            return false;
        }

        String direction = normalizeDirection(
                firstText(row, "DRCT", "방향 (공통)", "DRN_CDE")
        );

        String status = normalizeStatus(
                firstText(row, "STTS_CD", "상태 (공통)", "STAT_CDE")
        );

        String positionInfo = normalizePositionInfo(
                firstText(row, "PSTN_INFO", "위치정보", "POS")
        );

        LocalDate referenceDate = firstValidDateOrToday(
                firstText(row, "RPLC_YMD", "교체일", "CAE_YMD"),
                firstText(row, "INSTL_YMD", "설치일", "ESB_YMD")
        );

        LocalDateTime lastSyncedAt = LocalDateTime.now();

        Optional<AcousticSignal> optional =
                acousticSignalRepository.findByAcousticSignalCode(acousticSignalCode);

        if (optional.isPresent()) {
            optional.get().updateFrom(
                    point.latitude(),
                    point.longitude(),
                    direction,
                    status,
                    positionInfo,
                    DataSourceType.SEOUL_ACOUSTIC_SIGNAL,
                    referenceDate,
                    lastSyncedAt
            );
        } else {
            acousticSignalRepository.save(
                    AcousticSignal.builder()
                            .acousticSignalCode(acousticSignalCode)
                            .latitude(point.latitude())
                            .longitude(point.longitude())
                            .direction(direction)
                            .status(status)
                            .positionInfo(positionInfo)
                            .source(DataSourceType.SEOUL_ACOUSTIC_SIGNAL)
                            .referenceDate(referenceDate)
                            .lastSyncedAt(lastSyncedAt)
                            .build()
            );
        }

        return true;
    }

    private boolean isValidKoreaWgs84(double latitude, double longitude) {
        return latitude >= 33.0 && latitude <= 39.5
                && longitude >= 124.0 && longitude <= 132.0;
    }

    private Wgs84Point convertSeoulTmToWgs84(double x, double y) {
        try {
            CRSFactory crsFactory = new CRSFactory();

            CoordinateReferenceSystem sourceCrs = crsFactory.createFromParameters(
                    "EPSG:5186",
                    "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 "
                            + "+x_0=200000 +y_0=600000 "
                            + "+ellps=GRS80 +units=m +no_defs"
            );

            CoordinateReferenceSystem targetCrs = crsFactory.createFromParameters(
                    "EPSG:4326",
                    "+proj=longlat +ellps=WGS84 +datum=WGS84 +no_defs"
            );

            CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
            CoordinateTransform transform = ctFactory.createTransform(sourceCrs, targetCrs);

            ProjCoordinate source = new ProjCoordinate(x, y);
            ProjCoordinate target = new ProjCoordinate();

            transform.transform(source, target);

            double longitude = target.x;
            double latitude = target.y;

            if (!isValidKoreaWgs84(latitude, longitude)) {
                log.warn("[SEOUL ACOUSTIC SIGNAL SKIP] transformed coordinate out of range. x={}, y={}, lat={}, lon={}",
                        x, y, latitude, longitude);
                return null;
            }

            return new Wgs84Point(latitude, longitude);
        } catch (Exception e) {
            log.error("[SEOUL ACOUSTIC SIGNAL COORDINATE TRANSFORM ERROR] x={}, y={}", x, y, e);
            return null;
        }
    }

    private String text(JsonNode row, String fieldName) {
        JsonNode value = row.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return hasText(text) ? text.trim() : null;
    }

    private String normalizeDirection(String value) {
        if (!hasText(value)) {
            return null;
        }

        String normalized = stripDecimalZero(value).trim();

        return switch (normalized) {
            case "001" -> "0";
            case "002" -> "45";
            case "003" -> "90";
            case "004" -> "135";
            case "005", "006" -> "180";
            case "007" -> "225";
            case "008" -> "270";
            case "009" -> "315";
            default -> normalized;
        };
    }

    private String normalizeStatus(String value) {
        if (!hasText(value)) {
            return null;
        }

        String normalized = stripDecimalZero(value).trim();

        return switch (normalized) {
            case "001", "1" -> "양호";
            case "002", "2" -> "파손";
            case "003", "3" -> "도색";
            case "004", "4" -> "노후";
            default -> normalized;
        };
    }

    private String normalizePositionInfo(String value) {
        if (!hasText(value)) {
            return null;
        }

        return stripDecimalZero(value).trim();
    }

    private String stripDecimalZero(String value) {
        String trimmed = value.trim();

        if (trimmed.endsWith(".0")) {
            return trimmed.substring(0, trimmed.length() - 2);
        }

        return trimmed;
    }

    private String firstText(JsonNode row, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(row, fieldName);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Double firstDouble(JsonNode row, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = row.get(fieldName);

            if (valueNode == null || valueNode.isNull()) {
                continue;
            }

            if (valueNode.isNumber()) {
                return valueNode.asDouble();
            }

            String value = valueNode.asText();
            if (!hasText(value)) {
                continue;
            }

            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                log.warn("[SEOUL ACOUSTIC SIGNAL PARSE DOUBLE FAIL] field={}, value={}",
                        fieldName, value);
            }
        }

        return null;
    }

    private LocalDate firstValidDateOrToday(String... values) {
        for (String value : values) {
            LocalDate parsed = parseDate(value);
            if (parsed != null) {
                return parsed;
            }
        }

        return LocalDate.now();
    }

    private LocalDate parseDate(String value) {
        if (!hasText(value)) {
            return null;
        }

        String normalized = value.trim();

        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, DateTimeFormatter.BASIC_ISO_DATE);
            }

            if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(normalized);
            }

            log.warn("[SEOUL ACOUSTIC SIGNAL] unsupported date format. value={}", value);
            return null;
        } catch (DateTimeParseException e) {
            log.warn("[SEOUL ACOUSTIC SIGNAL] invalid date. value={}", value);
            return null;
        }
    }

    private List<String> fieldNames(JsonNode row) {
        List<String> names = new ArrayList<>();
        row.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record Wgs84Point(double latitude, double longitude) {
    }
}