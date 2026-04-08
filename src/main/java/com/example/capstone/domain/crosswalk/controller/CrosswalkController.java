package com.example.capstone.domain.crosswalk.controller;

import com.example.capstone.domain.crosswalk.dto.request.CrosswalkCandidateRequest;
import com.example.capstone.domain.crosswalk.dto.response.CrosswalkCandidateResponse;
import com.example.capstone.domain.crosswalk.service.CrosswalkCandidateService;
import com.example.capstone.domain.crosswalk.service.CrosswalkPublicApiService;
import com.example.capstone.global.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class CrosswalkController {

    private final CrosswalkCandidateService crosswalkCandidateService;
    private final CrosswalkPublicApiService crosswalkPublicApiService;

    @PostMapping("/api/crosswalks/candidates")
    public ApiResponse<CrosswalkCandidateResponse> findCandidates(
            @Valid @RequestBody CrosswalkCandidateRequest request
    ) {
        return ApiResponse.ok(crosswalkCandidateService.findCandidates(request));
    }

    @GetMapping("/api/crosswalks/raw")
    public ApiResponse<JsonNode> getRawResponse(
            @RequestParam(defaultValue = "서울특별시") String ctprvnNm,
            @RequestParam(defaultValue = "동작구") String signguNm
    ) {
        return ApiResponse.ok(crosswalkPublicApiService.fetchRawJson(ctprvnNm, signguNm));
    }
}