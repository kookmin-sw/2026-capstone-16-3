package com.example.capstone.domain.crosswalk.exception;

public enum CrosswalkErrorCode {
    CROSSWALK_API_ERROR("CROSSWALK_API_ERROR", "횡단보도 공공데이터 조회에 실패했습니다."),
    INVALID_CROSSWALK_API_RESPONSE("INVALID_CROSSWALK_API_RESPONSE", "횡단보도 공공데이터 응답 형식이 올바르지 않습니다.");

    private final String code;
    private final String message;

    CrosswalkErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}