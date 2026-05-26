package com.example.capstone.domain.crosswalk.service;

public final class AdministrativeRegionParser {

    private AdministrativeRegionParser() {
    }

    public static AdministrativeRegion parse(String address) {
        if (!hasText(address)) {
            return AdministrativeRegion.empty();
        }

        String normalized = address
                .replaceAll("\\s+", " ")
                .trim();

        String[] tokens = normalized.split(" ");

        String sido = null;
        String sigungu = null;
        String emd = null;

        for (String token : tokens) {
            String cleaned = token.trim();

            if (sido == null && isSido(cleaned)) {
                sido = normalizeSido(cleaned);
                continue;
            }

            if (sigungu == null && isSigungu(cleaned)) {
                sigungu = normalizeSigungu(cleaned);
                continue;
            }

            if (emd == null && isEmd(cleaned)) {
                emd = cleaned;
            }
        }

        return new AdministrativeRegion(sido, sigungu, emd);
    }

    public static String normalizeSido(String sido) {
        if (!hasText(sido)) {
            return null;
        }

        String normalized = sido.replace(" ", "").trim();

        return switch (normalized) {
            case "서울", "서울시", "서울특별시" -> "서울특별시";
            default -> normalized;
        };
    }

    public static String normalizeSigungu(String sigungu) {
        if (!hasText(sigungu)) {
            return null;
        }

        return sigungu.replace(" ", "").trim();
    }

    private static boolean isSido(String token) {
        return token.endsWith("특별시")
                || token.endsWith("광역시")
                || token.endsWith("특별자치시")
                || token.endsWith("도")
                || token.equals("서울")
                || token.equals("서울시");
    }

    private static boolean isSigungu(String token) {
        return token.endsWith("시")
                || token.endsWith("군")
                || token.endsWith("구");
    }

    private static boolean isEmd(String token) {
        return token.endsWith("읍")
                || token.endsWith("면")
                || token.endsWith("동")
                || token.endsWith("가");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AdministrativeRegion(
            String sido,
            String sigungu,
            String emd
    ) {
        public static AdministrativeRegion empty() {
            return new AdministrativeRegion(null, null, null);
        }
    }
}