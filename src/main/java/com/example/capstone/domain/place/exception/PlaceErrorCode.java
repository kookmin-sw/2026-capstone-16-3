package com.example.capstone.domain.place.exception;

import com.example.capstone.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

    PLACE_BAD_REQUEST("PLACE_001","잘못된 장소 요청입니다.", HttpStatus.BAD_REQUEST),
    PLACE_INVALID_COORDINATE("PLACE_002", "좌표 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    PLACE_NOT_FOUND("PLACE_003", "장소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PLACE_CACHE_MISS("PLACE_004", "캐시에 장소 정보가 없습니다.", HttpStatus.NOT_FOUND),

    PLACE_EXTERNAL_API_CONNECTION_ERROR("PLACE_005", "외부 장소 API 연결에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_TIMEOUT("PLACE_006", "외부 장소 API 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    PLACE_EXTERNAL_API_HTTP_4XX("PLACE_007", "외부 장소 API 요청이 잘못되었습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_HTTP_5XX("PLACE_008", "외부 장소 API 서버 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    PLACE_EXTERNAL_API_ERROR("PLACE_009", "외부 장소 API 처리 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),

    FAVORITE_ALREADY_EXISTS("PLACE_010", "이미 즐겨찾기에 등록된 장소입니다.", HttpStatus.ALREADY_REPORTED),
    FAVORITE_LIMIT_EXCEEDED("PLACE_011", "즐겨찾기는 최대 50개까지 등록할 수 있습니다.", HttpStatus.BANDWIDTH_LIMIT_EXCEEDED),
    FAVORITE_NOT_FOUND("PLACE_012", "해당 즐겨찾기를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}