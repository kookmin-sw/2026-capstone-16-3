package com.example.capstone.global.exception;

import com.example.capstone.global.api.ApiError;
import com.example.capstone.global.api.ApiResponse;
import com.example.capstone.global.api.ErrorDetail;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();

        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<ErrorDetail> details = e.getConstraintViolations().stream()
                .map(v -> ErrorDetail.of(v.getPropertyPath().toString(), v.getMessage()))
                .toList();

        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ApiError error = new ApiError(
                "TYPE_MISMATCH",
                "요청 파라미터 타입이 올바르지 않습니다.",
                List.of(
                        ErrorDetail.of("parameter", e.getName()),
                        ErrorDetail.of("rejectedValue", e.getValue()),
                        ErrorDetail.of("requiredType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : null)
                )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        ApiError error = new ApiError(
                "MISSING_PARAMETER",
                "필수 요청 파라미터가 누락되었습니다.",
                List.of(
                        ErrorDetail.of("parameter", e.getParameterName()),
                        ErrorDetail.of("expectedType", e.getParameterType())
                )
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        ApiError error = new ApiError(
                "INVALID_REQUEST_BODY",
                "요청 본문 형식이 올바르지 않습니다.",
                List.of(ErrorDetail.of("cause", e.getMostSpecificCause().getMessage()))
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ApiError error = new ApiError(
                e.getCode(),
                e.getMessage(),
                e.getDetails()
        );

        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientRequest(WebClientRequestException e) {
        ApiError error = new ApiError(
                "EXTERNAL_API_REQUEST_ERROR",
                "외부 API 요청 중 연결 오류가 발생했습니다.",
                List.of(
                        ErrorDetail.of("exception", e.getClass().getSimpleName()),
                        ErrorDetail.of("message", e.getMessage()),
                        ErrorDetail.of("cause", e.getCause() != null ? e.getCause().getClass().getSimpleName() : null),
                        ErrorDetail.of("causeMessage", e.getCause() != null ? e.getCause().getMessage() : null)
                )
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebClientResponse(WebClientResponseException e) {
        ApiError error = new ApiError(
                "EXTERNAL_API_RESPONSE_ERROR",
                "외부 API 응답 오류가 발생했습니다.",
                List.of(
                        ErrorDetail.of("exception", e.getClass().getSimpleName()),
                        ErrorDetail.of("httpStatus", e.getRawStatusCode()),
                        ErrorDetail.of("message", e.getMessage()),
                        ErrorDetail.of("responseBody", e.getResponseBodyAsString())
                )
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ApiError error = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "서버 오류가 발생했습니다.",
                List.of(ErrorDetail.of("cause", e.getClass().getSimpleName()))
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(error));
    }

    private ErrorDetail toErrorDetail(FieldError fe) {
        return new ErrorDetail(
                fe.getField(),
                fe.getDefaultMessage() + (fe.getRejectedValue() != null
                        ? " (입력값: " + fe.getRejectedValue() + ")"
                        : "")
        );
    }
}