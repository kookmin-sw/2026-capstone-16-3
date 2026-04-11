package com.example.capstone.domain.place.dto.response;

import java.util.List;

public record SliceResponse<T>(
        List<T> items,
        int page,
        int size,
        boolean hasNext,
        Integer nextPage
) {
    public static <T> SliceResponse<T> of(List<T> items, int page, int size, boolean hasNext) {
        return new SliceResponse<>(
                items,
                page,
                size,
                hasNext,
                hasNext ? page + 1 : null
        );
    }
}
