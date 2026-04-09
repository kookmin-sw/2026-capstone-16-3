package com.example.capstone.domain.guide.service;

import com.example.capstone.domain.guide.exception.GuideErrorCode;
import com.example.capstone.domain.guide.exception.GuideException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
@Transactional(readOnly = true)
public class GuideService {

    private final WebClient fastApiWebClient;

    public GuideService(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    public void sendFrame(
            MultipartFile image,
            Long userId,
            String capturedAt
    ) {
        validateImage(image);
        validateUserId(userId);
        validateCapturedAt(capturedAt);

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();

            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return Objects.requireNonNullElse(image.getOriginalFilename(), "image.jpg");
                }
            };

            builder.part("image", imageResource)
                    .filename(Objects.requireNonNullElse(image.getOriginalFilename(), "image.jpg"))
                    .contentType(resolveMediaType(image.getContentType()));

            builder.part("user_id", userId.toString());
            builder.part("captured_at", capturedAt);

            fastApiWebClient.post()
                    .uri("/api/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException e) {
            log.error("FastAPI 응답 오류 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new GuideException(GuideErrorCode.FASTAPI_RESPONSE_ERROR);
        } catch (IOException e) {
            log.error("이미지 읽기 실패", e);
            throw new GuideException(GuideErrorCode.IMAGE_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("FastAPI 호출 실패", e);
            throw new GuideException(GuideErrorCode.FASTAPI_REQUEST_FAILED);
        }
    }

    private void validateImage(MultipartFile image) {

        if (image == null || image.isEmpty()) {
            throw new GuideException(GuideErrorCode.IMAGE_REQUIRED);
        }

        String contentType = image.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new GuideException(GuideErrorCode.INVALID_IMAGE_TYPE);
        }

        long maxSize = 10 * 1024 * 1024;

        if (image.getSize() > maxSize) {
            throw new GuideException(GuideErrorCode.IMAGE_SIZE_EXCEEDED);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return StringUtils.hasText(contentType)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.IMAGE_JPEG;
        } catch (Exception e) {
            return MediaType.IMAGE_JPEG;
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new GuideException(GuideErrorCode.USER_NOT_AUTHENTICATED);
        }
    }

    private void validateCapturedAt(String capturedAt) {
        if (!StringUtils.hasText(capturedAt)) {
            throw new GuideException(GuideErrorCode.INVALID_CAPTURED_AT);
        }
    }
}
