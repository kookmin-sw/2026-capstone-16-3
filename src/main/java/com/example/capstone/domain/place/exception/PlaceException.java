package com.example.capstone.domain.place.exception;

import com.example.capstone.global.exception.BusinessException;
import lombok.Getter;

@Getter
public class PlaceException extends BusinessException {

    public PlaceException(PlaceErrorCode errorCode) {
        super(errorCode);
    }
}
