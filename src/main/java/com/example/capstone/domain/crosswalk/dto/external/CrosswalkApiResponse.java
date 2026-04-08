package com.example.capstone.domain.crosswalk.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CrosswalkApiResponse {
    private Response response;

    @Getter
    @Setter
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    public static class Header {
        private String resultCode;
        private String resultMsg;
        private String type;
    }

    @Getter
    @Setter
    public static class Body {
        private List<CrosswalkApiItem> items;
        private String totalCount;
        private String numOfRows;
        private String pageNo;
    }
}