package com.example.capstone.domain.guide.exception;

import com.example.capstone.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GuideErrorCode implements ErrorCode {
    IMAGE_REQUIRED("GUIDE_001", "이미지 파일은 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_TYPE("GUIDE_002", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
    IMAGE_SIZE_EXCEEDED("GUIDE_003", "이미지 크기는 10MB 이하여야 합니다.", HttpStatus.BAD_REQUEST),

    FASTAPI_REQUEST_FAILED("GUIDE_004", "AI 서버 요청에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    FASTAPI_RESPONSE_ERROR("GUIDE_005", "AI 서버 응답 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    IMAGE_PROCESSING_FAILED("GUIDE_006", "이미지 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    USER_NOT_AUTHENTICATED("GUIDE_007", "인증된 사용자 정보가 없습니다.", HttpStatus.UNAUTHORIZED),

    INVALID_EVENT_MESSAGE("GUIDE_008", "이벤트 메시지가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_STATUS("GUIDE_009", "이벤트 메시지가 없습니다.", HttpStatus.NO_CONTENT),

    INVALID_CAPTURED_AT("GUIDE_010", "captured_at이 없습니다.", HttpStatus.NO_CONTENT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
