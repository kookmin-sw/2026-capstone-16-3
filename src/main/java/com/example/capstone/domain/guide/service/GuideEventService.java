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

        GuideEventResponse message = new GuideEventResponse(
                request.message()
        );

        messagingTemplate.convertAndSendToUser(
                request.userId(),
                "/queue/guide",
                message
        );

        log.info("가이드 이벤트 전송 완료. userId={}", request.userId());
    }

    private void validateRequest(GuideEventRequest request) {
        if (request.userId() == null) {
            throw new GuideException(GuideErrorCode.USER_NOT_AUTHENTICATED);
        }
        if (!StringUtils.hasText(request.message())) {
            throw new GuideException(GuideErrorCode.INVALID_EVENT_MESSAGE);
        }
    }
}
