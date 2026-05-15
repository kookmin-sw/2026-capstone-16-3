package com.example.capstone.global.api;

public record ErrorDetail(
        String field,
        String reason
) {
    public static ErrorDetail of(String field, Object reason) {
        return new ErrorDetail(field, reason == null ? null : String.valueOf(reason));
    }
}