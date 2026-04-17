package com.example.capstone.domain.user.exception;

import com.example.capstone.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_SETTING_NOT_FOUND("USER_002", "설정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
