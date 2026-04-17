package com.example.capstone.domain.crosswalk.exception;

import com.example.capstone.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CrosswalkErrorCode implements ErrorCode {

    CROSSWALK_API_ERROR("CROSSWALK_API_ERROR", "횡단보도 공공데이터 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_API_TIMEOUT("CROSSWALK_API_TIMEOUT", "횡단보도 공공데이터 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    CROSSWALK_API_CONNECTION_ERROR("CROSSWALK_API_CONNECTION_ERROR", "횡단보도 공공데이터 연결에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_API_HTTP_4XX("CROSSWALK_API_HTTP_4XX", "횡단보도 공공데이터 요청이 잘못되었습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_API_HTTP_5XX("CROSSWALK_API_HTTP_5XX", "횡단보도 공공데이터 서버 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_API_EMPTY_BODY("CROSSWALK_API_EMPTY_BODY", "횡단보도 공공데이터 응답 본문이 비어 있습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_API_DECODING_ERROR("CROSSWALK_API_DECODING_ERROR", "횡단보도 공공데이터 응답 해석에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    INVALID_CROSSWALK_API_RESPONSE("INVALID_CROSSWALK_API_RESPONSE", "횡단보도 공공데이터 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),

    REGION_RESOLVE_FAILED("REGION_RESOLVE_FAILED", "현재 위치를 행정구역으로 변환하지 못했습니다.", HttpStatus.BAD_GATEWAY),
    REGION_RESOLVE_TIMEOUT("REGION_RESOLVE_TIMEOUT", "행정구역 변환 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    REGION_RESOLVE_HTTP_4XX("REGION_RESOLVE_HTTP_4XX", "행정구역 변환 요청이 잘못되었습니다.", HttpStatus.BAD_GATEWAY),
    REGION_RESOLVE_HTTP_5XX("REGION_RESOLVE_HTTP_5XX", "행정구역 변환 서버 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    INVALID_REGION_RESPONSE("INVALID_REGION_RESPONSE", "행정구역 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    REGION_NOT_FOUND("REGION_NOT_FOUND", "현재 위치에 대한 행정구역 정보를 찾지 못했습니다.", HttpStatus.NOT_FOUND),

    CROSSWALK_NO_DATA("CROSSWALK_NO_DATA", "조회된 횡단보도 데이터가 없습니다.", HttpStatus.NOT_FOUND),

    CROSSWALK_INVALID_PARAMETER("CROSSWALK_INVALID_PARAMETER", "횡단보도 공공데이터 요청 파라미터가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    CROSSWALK_MISSING_PARAMETER("CROSSWALK_MISSING_PARAMETER", "횡단보도 공공데이터 필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),

    CROSSWALK_SERVICE_ACCESS_DENIED("CROSSWALK_SERVICE_ACCESS_DENIED", "횡단보도 공공데이터 서비스 접근이 거부되었습니다.", HttpStatus.FORBIDDEN),
    CROSSWALK_SERVICE_KEY_TEMPORARILY_DISABLED("CROSSWALK_SERVICE_KEY_TEMPORARILY_DISABLED", "서비스 키를 일시적으로 사용할 수 없습니다.", HttpStatus.FORBIDDEN),
    CROSSWALK_RATE_LIMIT_EXCEEDED("CROSSWALK_RATE_LIMIT_EXCEEDED", "공공데이터 요청 가능 횟수를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    CROSSWALK_SERVICE_KEY_NOT_REGISTERED("CROSSWALK_SERVICE_KEY_NOT_REGISTERED", "등록되지 않은 서비스 키입니다.", HttpStatus.UNAUTHORIZED),
    CROSSWALK_SERVICE_KEY_EXPIRED("CROSSWALK_SERVICE_KEY_EXPIRED", "기한이 만료된 서비스 키입니다.", HttpStatus.UNAUTHORIZED),
    CROSSWALK_UNREGISTERED_IP("CROSSWALK_UNREGISTERED_IP", "등록되지 않은 IP에서 호출했습니다.", HttpStatus.FORBIDDEN),
    CROSSWALK_UNSIGNED_CALL("CROSSWALK_UNSIGNED_CALL", "서명되지 않은 호출입니다.", HttpStatus.FORBIDDEN),

    CROSSWALK_APPLICATION_ERROR("CROSSWALK_APPLICATION_ERROR", "공공데이터 서비스 내부 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_DB_ERROR("CROSSWALK_DB_ERROR", "공공데이터 서비스 데이터베이스 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_HTTP_ERROR("CROSSWALK_HTTP_ERROR", "공공데이터 HTTP 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    CROSSWALK_TIMEOUT_ERROR("CROSSWALK_TIMEOUT_ERROR", "공공데이터 서비스 연결에 실패했습니다.", HttpStatus.GATEWAY_TIMEOUT),
    CROSSWALK_UNKNOWN_ERROR("CROSSWALK_UNKNOWN_ERROR", "알 수 없는 공공데이터 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;

    CrosswalkErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    public static CrosswalkErrorCode fromOpenApiCode(String resultCode) {
        return switch (resultCode) {
            case "00" -> null;
            case "01" -> CROSSWALK_APPLICATION_ERROR;
            case "02" -> CROSSWALK_DB_ERROR;
            case "03" -> CROSSWALK_NO_DATA;
            case "04" -> CROSSWALK_HTTP_ERROR;
            case "05" -> CROSSWALK_TIMEOUT_ERROR;
            case "10" -> CROSSWALK_INVALID_PARAMETER;
            case "11" -> CROSSWALK_MISSING_PARAMETER;
            case "20" -> CROSSWALK_SERVICE_ACCESS_DENIED;
            case "21" -> CROSSWALK_SERVICE_KEY_TEMPORARILY_DISABLED;
            case "22" -> CROSSWALK_RATE_LIMIT_EXCEEDED;
            case "30" -> CROSSWALK_SERVICE_KEY_NOT_REGISTERED;
            case "31" -> CROSSWALK_SERVICE_KEY_EXPIRED;
            case "32" -> CROSSWALK_UNREGISTERED_IP;
            case "33" -> CROSSWALK_UNSIGNED_CALL;
            case "99" -> CROSSWALK_UNKNOWN_ERROR;
            default -> CROSSWALK_API_ERROR;
        };
    }
}