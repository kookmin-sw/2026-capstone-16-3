package com.example.capstone.domain.place.service;

import org.springframework.stereotype.Component;

@Component
public class PlaceGeoCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public long haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_METERS * c);
    }

    public Integer toDirectionClock(
            double originLat,
            double originLng,
            double targetLat,
            double targetLng
    ) {
        double bearing = bearingDegrees(originLat, originLng, targetLat, targetLng);
        int clock = (int) Math.floor((bearing + 15.0) / 30.0) % 12;
        return clock == 0 ? 12 : clock;
    }

    public Long parseDistance(String distance) {
        if (!hasText(distance)) {
            return null;
        }

        try {
            return Long.parseLong(distance);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double parseDoubleOrNull(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);

        double y = Math.sin(dLon) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(dLon);

        double theta = Math.atan2(y, x);
        double deg = Math.toDegrees(theta);

        return (deg + 360.0) % 360.0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}