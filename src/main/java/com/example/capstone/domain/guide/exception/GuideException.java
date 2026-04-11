package com.example.capstone.domain.guide.exception;

import com.example.capstone.global.exception.BusinessException;
import lombok.Getter;

@Getter
public class GuideException extends BusinessException {

    public GuideException(GuideErrorCode errorCode) {
        super(errorCode);
    }
}
