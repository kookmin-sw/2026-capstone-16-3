package com.example.capstone.domain.crosswalk.service;

import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiItem;
import com.example.capstone.domain.crosswalk.dto.external.CrosswalkApiResponse;
import com.example.capstone.domain.crosswalk.entity.Crosswalk;
import com.example.capstone.domain.crosswalk.exception.CrosswalkErrorCode;
import com.example.capstone.domain.crosswalk.repository.CrosswalkRepository;
import com.example.capstone.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrosswalkSyncService {

    private final WebClient webClient;
    private final CrosswalkRepository crosswalkRepository;

    @Value("${public-data.crosswalk.base-url}")
    private String baseUrl;

    @Value("${public-data.crosswalk.service-key}")
    private String serviceKey;

    @Value("${public-data.crosswalk.default-page-size:1000}")
    private int pageSize;

    @Transactional
    public void syncByRegion(String ctprvnNm, String signguNm) {
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;

        while ((pageNo - 1) * pageSize < totalCount) {
            CrosswalkApiResponse response = fetchPage(ctprvnNm, signguNm, pageNo);

            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getBody() == null
                    || response.getResponse().getBody().getItems() == null
                    || response.getResponse().getBody().getItems().getItem() == null) {
                throw new BusinessException(
                        CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE.code(),
                        CrosswalkErrorCode.INVALID_CROSSWALK_API_RESPONSE.message()
                );
            }

            totalCount = response.getResponse().getBody().getTotalCount();
            List<CrosswalkApiItem> items = response.getResponse().getBody().getItems().getItem();

            for (CrosswalkApiItem item : items) {
                upsert(item);
            }

            pageNo++;
        }
    }

    private CrosswalkApiResponse fetchPage(String ctprvnNm, String signguNm, int pageNo) {
        String uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", pageSize)
                .queryParam("type", "json")
                .queryParam("ctprvnNm", ctprvnNm)
                .queryParam("signguNm", signguNm)
                .build(false)
                .toUriString();

        try {
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(CrosswalkApiResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.code(),
                    CrosswalkErrorCode.CROSSWALK_API_ERROR.message()
            );
        }
    }

    private void upsert(CrosswalkApiItem item) {
        if (item.getCrslkManageNo() == null || item.getCrslkManageNo().isBlank()) {
            return;
        }

        Crosswalk crosswalk = crosswalkRepository.findByCrslkManageNo(item.getCrslkManageNo())
                .orElseGet(() -> Crosswalk.create(item.getCrslkManageNo()));

        crosswalk.updateFromApi(
                item.getCtprvnNm(),
                item.getSignguNm(),
                item.getRoadNm(),
                item.getRdnmadr(),
                item.getLnmadr(),
                item.getCrslkKnd(),
                item.getBcyclCrslkCmbnatYn(),
                item.getHighlandYn(),
                parseDouble(item.getLatitude()),
                parseDouble(item.getLongitude()),
                parseInteger(item.getCartrkCo()),
                parseDouble(item.getBt()),
                parseDouble(item.getEt()),
                item.getTfclghtYn(),
                item.getFnctngSgngnrYn(),
                item.getSondSgngnrYn(),
                item.getGreenSgngnrTime(),
                item.getRedSgngnrTime(),
                item.getTfcilndYn(),
                item.getFtpthLowerYn(),
                item.getBrllBlckYn(),
                item.getCnctrLghtFcltyYn(),
                item.getInstitutionNm(),
                item.getPhoneNumber(),
                parseDate(item.getReferenceDate())
        );

        crosswalkRepository.save(crosswalk);
    }

    private Double parseDouble(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}