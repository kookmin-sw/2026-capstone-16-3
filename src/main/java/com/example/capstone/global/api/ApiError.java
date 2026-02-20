package com.example.capstone.global.api;

import java.util.List;

public record ApiError(
        String code,
        String message,
        List<ErrorDetail> details
) {
}
