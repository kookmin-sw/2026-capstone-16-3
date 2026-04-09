package com.example.capstone.global.exception;

import com.example.capstone.global.api.ErrorDetail;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class BusinessException extends RuntimeException{

    private final String code;
    private final List<ErrorDetail> details;
//    private final HttpStatus status;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, List<ErrorDetail> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
