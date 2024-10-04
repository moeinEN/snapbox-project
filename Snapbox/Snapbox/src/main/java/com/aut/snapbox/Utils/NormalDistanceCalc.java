package com.aut.snapbox.Utils;

import static com.aut.snapbox.Model.Constants.EARTH_RADIUS;

public class NormalDistanceCalc {
    public static double[] calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = EARTH_RADIUS;
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        double deltaY = R * deltaLat;
        double deltaX = R * deltaLon * Math.cos(lat1Rad);
        double magnitude = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        return new double[]{deltaX, deltaY, magnitude};
    }
}
