package com.example.capstone.domain.auth.exception;

import com.example.capstone.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTH_INVALID_TOKEN("AUTH_001", "토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),

    AUTH_TOKEN_TYPE("AUTH_002", "Refresh 토큰이 필요합니다.", HttpStatus.BAD_REQUEST),
    AUTH_REFRESH_NOT_FOUND("AUTH_003", "Refresh 토큰이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
    AUTH_REFRESH_EXPIRED("AUTH_004", "만료된 Refresh 토큰입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
