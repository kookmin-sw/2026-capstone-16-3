package com.example.capstone.domain.place.exception;

import lombok.Getter;

@Getter
public class PlaceException extends RuntimeException {

    private final PlaceErrorCode errorCode;

    public PlaceException(PlaceErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
