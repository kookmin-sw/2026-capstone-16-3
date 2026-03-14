package com.example.capstone.domain.guide.controller;

import com.example.capstone.domain.guide.dto.request.GuideEventRequest;
import com.example.capstone.domain.guide.service.GuideEventService;
import com.example.capstone.domain.guide.service.GuideService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideService guideService;
    private final GuideEventService guideEventService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> forwardImage(
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal Long userId
    ) {
        guideService.sendFrame(image, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/event")
    public ApiResponse<Void> receiveGuideEvent(@RequestBody GuideEventRequest request) {
        guideEventService.sentEventToUser(request);
        return ApiResponse.ok();
    }
}
