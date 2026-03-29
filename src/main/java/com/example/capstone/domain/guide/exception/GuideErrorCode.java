package com.example.capstone.domain.guide.exception;

import org.springframework.http.HttpStatus;

public enum GuideErrorCode {
    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "이미지 파일은 필수입니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지 크기는 10MB 이하여야 합니다."),

    FASTAPI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서버 요청에 실패했습니다."),
    FASTAPI_RESPONSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI 서버 응답 처리 중 오류가 발생했습니다."),

    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다."),

    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 없습니다."),

    INVALID_EVENT_MESSAGE(HttpStatus.BAD_REQUEST, "이벤트 메시지가 올바르지 않습니다."),
    INVALID_EVENT_STATUS(HttpStatus.NO_CONTENT, "이벤트 메시지가 없습니다."),

    INVALID_CAPTURED_AT(HttpStatus.NO_CONTENT, "captured_at이 없습니다.");

    private final HttpStatus status;
    private final String message;

    GuideErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
