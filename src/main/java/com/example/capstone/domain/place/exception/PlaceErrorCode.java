package com.example.capstone.domain.place.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PlaceErrorCode {

    PLACE_BAD_REQUEST(HttpStatus.BAD_REQUEST,"잘못된 장소 요청입니다."),
    PLACE_INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "좌표 값이 올바르지 않습니다."),

    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),
    PLACE_CACHE_MISS(HttpStatus.NOT_FOUND, "캐시에 장소 정보가 없습니다."),

    PLACE_EXTERNAL_API_CONNECTION_ERROR(HttpStatus.BAD_GATEWAY, "외부 장소 API 연결에 실패했습니다."),
    PLACE_EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 장소 API 응답 시간이 초과되었습니다."),
    PLACE_EXTERNAL_API_HTTP_4XX(HttpStatus.BAD_GATEWAY, "외부 장소 API 요청이 잘못되었습니다."),
    PLACE_EXTERNAL_API_HTTP_5XX(HttpStatus.BAD_GATEWAY, "외부 장소 API 서버 오류가 발생했습니다."),
    PLACE_EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 장소 API 처리 중 오류가 발생했습니다.");

    private final String message;
    private final HttpStatus status;

    PlaceErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}