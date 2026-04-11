package com.example.capstone.domain.auth.exception;

import com.example.capstone.global.exception.BusinessException;

public class AuthException extends BusinessException {

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode);
    }
}
