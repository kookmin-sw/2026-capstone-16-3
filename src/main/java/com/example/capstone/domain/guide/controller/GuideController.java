package com.example.capstone.domain.guide.controller;

import com.example.capstone.domain.guide.service.GuideService;
import com.example.capstone.global.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideService guideService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> forwardImage(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "capturedAt", required = false) String capturedAt,
            @RequestPart(value = "latitude", required = false) Double latitude,
            @RequestPart(value = "longitude", required = false) Double longitude
    ) {
        guideService.sendFrame(image, capturedAt, latitude, longitude);
        return ApiResponse.ok();
    }
}
