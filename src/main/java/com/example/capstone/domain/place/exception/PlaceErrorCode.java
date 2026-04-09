package com.example.capstone.domain.place.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PlaceErrorCode {

    PLACE_BAD_REQUEST("PLACE_BAD_REQUEST", "잘못된 장소 요청입니다.", HttpStatus.BAD_REQUEST),
    PLACE_INVALID_COORDINATE("PLACE_INVALID_COORDINATE", "좌표 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    PLACE_NOT_FOUND("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PLACE_CACHE_MISS("PLACE_CACHE_MISS", "캐시에 장소 정보가 없습니다.", HttpStatus.NOT_FOUND),

    PLACE_EXTERNAL_API_CONNECTION_ERROR("PLACE_EXTERNAL_API_CONNECTION_ERROR", "외부 장소 API 연결에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_TIMEOUT("PLACE_EXTERNAL_API_TIMEOUT", "외부 장소 API 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    PLACE_EXTERNAL_API_HTTP_4XX("PLACE_EXTERNAL_API_HTTP_4XX", "외부 장소 API 요청이 잘못되었습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_HTTP_5XX("PLACE_EXTERNAL_API_HTTP_5XX", "외부 장소 API 서버 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_ERROR("PLACE_EXTERNAL_API_ERROR", "외부 장소 API 처리 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;

    PlaceErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}