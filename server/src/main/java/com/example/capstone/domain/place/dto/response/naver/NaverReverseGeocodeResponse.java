package com.example.capstone.domain.place.dto.response.naver;

import java.util.List;

public record NaverReverseGeocodeResponse(
        Status status,
        List<Result> results
) {
    public record Status(
            int code,
            String name,
            String message
    ) {}

    public record Result(
            String name,
            Code code,
            Region region,
            Land land
    ) {}

    public record Code(
            String id,
            String type,
            String mappingId
    ) {}

    public record Region(
            Area area0,
            Area area1,
            Area area2,
            Area area3,
            Area area4
    ) {}

    public record Area(
            String name,
            Coords coords,
            String alias
    ) {}

    public record Coords(
            Center center
    ) {}

    public record Center(
            String crs,
            double x,
            double y
    ) {}

    public record Land(
            String type,
            String number1,
            String number2,
            Addition addition0,
            Addition addition1,
            Addition addition2,
            Addition addition3,
            Addition addition4,
            String name,
            Coords coords
    ) {}

    public record Addition(
            String type,
            String value
    ) {}
}