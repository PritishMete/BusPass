package com.pritish.smartbuss;

public class FareCalculator {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Calculates the distance between two points on Earth using the Haversine formula.
     *
     * @param lat1 Latitude of the first point.
     * @param lon1 Longitude of the first point.
     * @param lat2 Latitude of the second point.
     * @param lon2 Longitude of the second point.
     * @return The distance in kilometers.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates the ticket fare based on the distance in kilometers according to the specified fare slabs.
     *
     * @param distanceInKm The travel distance.
     * @return The calculated fare as a float.
     */
    public static float calculateFare(double distanceInKm) {
        if (distanceInKm <= 4) {
            return 7.0f;
        } else if (distanceInKm <= 12) {
            return 9.0f;
        } else if (distanceInKm <= 16) {
            return 10.0f;
        } else if (distanceInKm <= 20) {
            return 11.0f;
        } else if (distanceInKm <= 24) {
            return 12.0f;
        } else {
            // After 24 km, add ₹1 for every 4 km block
            double distanceBeyond24Km = distanceInKm - 24;
            // Calculate how many 4km blocks are there, using Math.ceil to round up.
            int additionalBlocks = (int) Math.ceil(distanceBeyond24Km / 4.0);
            return 12.0f + additionalBlocks;
        }
    }
}