package com.example.capstone.global.exception;

import com.example.capstone.global.api.ErrorDetail;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final List<ErrorDetail> details;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST, null);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public BusinessException(String code, String message, HttpStatus status, List<ErrorDetail> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }
}