package com.example.capstone.domain.place.exception;

import com.example.capstone.global.api.ErrorDetail;
import com.example.capstone.global.exception.BusinessException;

import java.util.List;

public class PlaceException extends BusinessException {

    public PlaceException(PlaceErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public PlaceException(PlaceErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
    }

    public PlaceException(PlaceErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(errorCode.getCode(), message, details);
    }
}