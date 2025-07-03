package com.pritish.smartbuss;

import android.location.Location;
import android.util.Log; // Import Log for debugging

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.*;
import java.util.stream.Collectors;

public class BusRouteData {

    private static final String TAG = "BusRouteData"; // Tag for logging
    private static final Map<String, List<BusStop>> busRoutes = new HashMap<>();
    private static final Map<String, List<String>> stopToRouteMap = new HashMap<>();
    private static final Map<String, LatLng> stopLatLngMap = new HashMap<>();

    // Interface for callback when data is loaded
    public interface OnDataLoadedListener {
        void onDataLoaded();
        void onDataLoadFailed(String errorMessage);
    }

    // Method to initialize data from Firebase
    public static void initializeDataFromFirebase(OnDataLoadedListener listener) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("all_routes");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                busRoutes.clear();
                stopToRouteMap.clear();
                stopLatLngMap.clear();

                // Check if any data exists at the "all_routes" node
                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "No data found at 'all_routes' node in Firebase.");
                    if (listener != null) {
                        listener.onDataLoadFailed("No bus route data found in Firebase.");
                    }
                    return;
                }

                for (DataSnapshot routeSnapshot : dataSnapshot.getChildren()) {
                    String routeNumber = routeSnapshot.getKey();
                    List<BusStop> stops = new ArrayList<>();

// Replace the existing parsing logic in BusRouteData.java
                    for (DataSnapshot stopSnapshot : routeSnapshot.getChildren()) {
                        try {
                            String name = stopSnapshot.child("name").getValue(String.class);

                            // Handle both String and Double types for lat/lng
                            double lat, lng;
                            Object latValue = stopSnapshot.child("lat").getValue();
                            Object lngValue = stopSnapshot.child("lng").getValue();

                            if (latValue instanceof String) {
                                lat = Double.parseDouble((String) latValue);
                            } else if (latValue instanceof Double) {
                                lat = (Double) latValue;
                            } else if (latValue instanceof Long) {
                                lat = ((Long) latValue).doubleValue();
                            } else {
                                Log.w(TAG, "Invalid lat type for stop in route " + routeNumber + ": " + latValue);
                                continue;
                            }

                            if (lngValue instanceof String) {
                                lng = Double.parseDouble((String) lngValue);
                            } else if (lngValue instanceof Double) {
                                lng = (Double) lngValue;
                            } else if (lngValue instanceof Long) {
                                lng = ((Long) lngValue).doubleValue();
                            } else {
                                Log.w(TAG, "Invalid lng type for stop in route " + routeNumber + ": " + lngValue);
                                continue;
                            }

                            // Check for nulls
                            if (name == null) {
                                Log.w(TAG, "Skipping stop with null name in route " + routeNumber);
                                continue;
                            }

                            // Handle ishelper field
                            boolean isHelper = false;
                            Object isHelperValue = stopSnapshot.child("ishelper").getValue();
                            if (isHelperValue instanceof String) {
                                isHelper = Boolean.parseBoolean((String) isHelperValue);
                            } else if (isHelperValue instanceof Boolean) {
                                isHelper = (Boolean) isHelperValue;
                            }

                            LatLng location = new LatLng(lat, lng);
                            BusStop busStop = new BusStop(name, location, isHelper);
                            stops.add(busStop);

                            Log.d(TAG, "Added stop: " + name + " at " + lat + ", " + lng + " (helper: " + isHelper + ")");

                        } catch (NumberFormatException e) {
                            Log.e(TAG, "NumberFormatException for route " + routeNumber + ": " + e.getMessage());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing stop data for route " + routeNumber + ": " + e.getMessage());
                        }
                    }                    if (!stops.isEmpty()) { // Only add route if it has valid stops
                        addRoute(routeNumber, stops);
                    } else {
                        Log.w(TAG, "Route " + routeNumber + " has no valid stops after parsing. Not added.");
                    }
                }
                if (!busRoutes.isEmpty()) {
                    Log.d(TAG, "Bus route data loaded successfully from Firebase. Total routes: " + busRoutes.size());
                    if (listener != null) {
                        listener.onDataLoaded();
                    }
                } else {
                    Log.w(TAG, "No bus routes were loaded successfully from Firebase. Check data structure.");
                    if (listener != null) {
                        listener.onDataLoadFailed("No valid bus routes found after parsing.");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load bus route data from Firebase: " + databaseError.getMessage());
                if (listener != null) {
                    listener.onDataLoadFailed(databaseError.getMessage());
                }
            }
        });
    }

    public static String getNearestStop(LatLng userLocation) {
        String nearestStop = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, LatLng> entry : stopLatLngMap.entrySet()) {
            double distance = calculateDistance(userLocation, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                nearestStop = entry.getKey();
            }
        }

        return nearestStop;
    }

    public static List<NearestStopModel> getNearestStopsDetailsWithinRadius(LatLng currentLocation, int radiusMeters) {
        List<NearestStopModel> nearestStops = new ArrayList<>();

        // Iterate through all stops
        for (String stop : getAllStops()) {
            LatLng stopLocation = getLatLngForStop(stop);

            if (stopLocation != null) {
                // Calculate distance between current location and stop
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.latitude, currentLocation.longitude,
                        stopLocation.latitude, stopLocation.longitude,
                        results
                );

                // If stop is within the specified radius, add to the list
                if (results[0] <= radiusMeters) {
                    nearestStops.add(new NearestStopModel(stop, results[0]));
                }
            }
        }

        // Sort stops by distance
        Collections.sort(nearestStops, (stop1, stop2) ->
                Float.compare(stop1.getDistance(), stop2.getDistance())
        );

        return nearestStops;
    }


    public static class BusStop {
        String name;
        LatLng location;
        boolean isHelper;

        BusStop(String name, LatLng location, boolean isHelper) {
            this.name = name;
            this.location = location;
            this.isHelper = isHelper;
        }
    }

    // Removed the static block as data will now be fetched dynamically

    private static void addRoute(String routeNumber, List<BusStop> stops) {
        busRoutes.put(routeNumber, stops);
        for (BusStop stop : stops) {
            if (!stop.isHelper) {
                stopLatLngMap.put(stop.name, stop.location);
                stopToRouteMap.computeIfAbsent(stop.name, k -> new ArrayList<>()).add(routeNumber);
            }
        }
    }

    public static List<String> getAllRoutes() {
        return new ArrayList<>(busRoutes.keySet());
    }

    public static List<String> getAllStops() {
        return new ArrayList<>(stopLatLngMap.keySet());
    }

    public static List<String> getRoutesBetweenStops(String start, String end) {
        List<String> startRoutes = stopToRouteMap.get(start);
        List<String> endRoutes = stopToRouteMap.get(end);

        if (startRoutes == null || endRoutes == null) {
            return new ArrayList<>();
        }

        List<String> commonRoutes = new ArrayList<>(startRoutes);
        commonRoutes.retainAll(endRoutes);
        return commonRoutes;
    }

    public static List<LatLng> getStopsForRoute(String routeNumber) {
        List<BusStop> stops = busRoutes.get(routeNumber);
        if (stops == null) return new ArrayList<>();
        return stops.stream().map(stop -> stop.location).collect(Collectors.toList());
    }

    public static List<String> getStopsForRouteNames(String routeNumber) {
        return busRoutes.getOrDefault(routeNumber, new ArrayList<>())
                .stream()
                .filter(stop -> !stop.isHelper)
                .map(stop -> stop.name)
                .collect(Collectors.toList());
    }

    public static String getStopNameByLatLng(LatLng latLng) {
        for (Map.Entry<String, LatLng> entry : stopLatLngMap.entrySet()) {
            if (entry.getValue().equals(latLng)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static LatLng getLatLngForStop(String stopName) {
        return stopLatLngMap.get(stopName);
    }
    // In BusRouteData.java
// In BusRouteData.java - Add this method if not already present
    public static List<BusStop> getBusStopsForRoute(String routeNumber) {
        return busRoutes.get(routeNumber);
    }
    public static boolean isForwardDirection(String routeNumber, String startStop, String endStop) {
        List<BusStop> stops = busRoutes.get(routeNumber);
        if (stops == null) return true; // default to forward if route not found

        int startIndex = -1;
        int endIndex = -1;

        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).name.equalsIgnoreCase(startStop)) {
                startIndex = i;
            }
            if (stops.get(i).name.equalsIgnoreCase(endStop)) {
                endIndex = i;
            }
        }

        // If we couldn't find either stop, default to forward
        if (startIndex == -1 || endIndex == -1) return true;

        // If start comes before end in the route, it's forward direction
        return startIndex < endIndex;
    }

    public static List<LatLng> getFilteredStopsForRoute(String routeNumber, String start, String end) {
        List<BusStop> stops = busRoutes.get(routeNumber);
        if (stops == null) return new ArrayList<>();

        int startIndex = -1, endIndex = -1;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).name.equals(start)) startIndex = i;
            if (stops.get(i).name.equals(end)) endIndex = i;
        }

        if (startIndex == -1 || endIndex == -1) return new ArrayList<>();

        // Handle reverse direction
        if (startIndex > endIndex) {
            List<BusStop> reversedStops = new ArrayList<>(stops.subList(endIndex, startIndex + 1));
            Collections.reverse(reversedStops);
            return reversedStops.stream().map(stop -> stop.location).collect(Collectors.toList());
        } else {
            return stops.subList(startIndex, endIndex + 1).stream()
                    .map(stop -> stop.location)
                    .collect(Collectors.toList());
        }
    }

    public static boolean isNearAnyStop(LatLng location, double radiusInMeters) {
        for (LatLng stopLocation : stopLatLngMap.values()) {
            if (calculateDistance(location, stopLocation) <= radiusInMeters) {
                return true;
            }
        }
        return false;
    }

    private static double calculateDistance(LatLng start, LatLng end) {
        Location startLocation = new Location("start");
        startLocation.setLatitude(start.latitude);
        startLocation.setLongitude(start.longitude);

        Location endLocation = new Location("end");
        endLocation.setLatitude(end.latitude);
        endLocation.setLongitude(end.longitude);

        return startLocation.distanceTo(endLocation);
    }
}