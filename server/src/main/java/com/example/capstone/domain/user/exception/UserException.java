package com.example.capstone.domain.user.exception;

import com.example.capstone.global.exception.BusinessException;
import lombok.Getter;

@Getter
public class UserException extends BusinessException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
