package com.example.capstone.domain.guide.service;

import com.example.capstone.domain.guide.exception.GuideErrorCode;
import com.example.capstone.domain.guide.exception.GuideException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
public class GuideService {

    private final WebClient fastApiWebClient;

    public GuideService(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    public void sendFrame(
            MultipartFile image,
            String capturedAt,
            Double latitude,
            Double longitude
    ) {
        validateImage(image);

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

            if (StringUtils.hasText(capturedAt)) {
                builder.part("captured_at", capturedAt);
            }
            if (latitude != null) {
                builder.part("latitude", latitude.toString());
            }
            if (longitude != null) {
                builder.part("longitude", longitude.toString());
            }

            fastApiWebClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException e) {
            log.error("FastAPI 응답 오류 status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 응답 처리 중 오류가 발생했습니다.");
        } catch (IOException e) {
            log.error("이미지 읽기 실패", e);
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("FastAPI 호출 실패", e);
            throw new RuntimeException("AI 서버 호출 중 오류가 발생했습니다.");
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
}
