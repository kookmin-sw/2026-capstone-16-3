package com.example.capstone.domain.guide.exception;

import lombok.Getter;

@Getter
public class GuideException extends RuntimeException {

    private final GuideErrorCode errorCode;

    public GuideException(GuideErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
