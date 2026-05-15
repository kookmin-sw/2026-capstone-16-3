package com.example.capstone.domain.guide.service;

import com.example.capstone.domain.guide.dto.request.GuideEventRequest;
import com.example.capstone.domain.guide.dto.response.GuideEventResponse;
import com.example.capstone.domain.guide.exception.GuideErrorCode;
import com.example.capstone.domain.guide.exception.GuideException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sentEventToUser(GuideEventRequest request) {
        validateRequest(request);

        GuideEventResponse response = new GuideEventResponse(
                request.status(),
                request.guideText(),
                request.primaryObjectClass(),
                request.clockDirection(),
                request.distance(),
                request.alertLevel(),
                request.primaryObjectId()
        );

        messagingTemplate.convertAndSendToUser(
                request.userId(),
                "/queue/guide",
                response
        );

        log.info("가이드 이벤트 전송 완료. userId={}", request.userId());
    }

    private void validateRequest(GuideEventRequest request) {
        if (!StringUtils.hasText(request.userId())) {
            throw new GuideException(GuideErrorCode.USER_NOT_AUTHENTICATED);
        }

        if (!StringUtils.hasText(request.status())) {
            throw new GuideException(GuideErrorCode.INVALID_EVENT_STATUS);
        }

        if (!StringUtils.hasText(request.guideText())) {
            throw new GuideException(GuideErrorCode.INVALID_EVENT_MESSAGE);
        }
    }
}
