package com.pritish.smartbuss;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class bus_location extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private DatabaseReference busLocationsRef;
    private DatabaseReference rootBookingsRef;
    private static final String TAG = "BusLocationActivity";

    // Location and route data
    private LatLng currentLocation;
    private Map<String, Marker> busMarkers = new HashMap<>();
    private Map<String, Polyline> busPolylines = new HashMap<>();
    private String currentRouteNumber;
    private String startLocationName;
    private String endLocationName;
    private String userPhone;

    // Route management
    private Map<String, RouteInfo> availableRoutes = new HashMap<>();
    private String selectedRouteId = null;
    private List<Polyline> routePolylines = new ArrayList<>();
    private List<Marker> routeStopMarkers = new ArrayList<>();

    // Booking details
    private TextView timeTextView, priceTextView;
    private int bookingPersonCount;
    private float bookingPrice;
    private String bookingDate, bookingTime;
    private boolean isScanned;
    private static final int WALKING_ROUTE_COLOR = Color.argb(200, 76, 175, 80); // Green color for walking route
    private static final int WALKING_ROUTE_WIDTH_DP = 6;
    private Polyline walkingPolyline;

    // Speed calculation constant (30 km/h)
    private static final double SPEED_KMH = 30.0;

    // Google Directions API
    private static final String[] TRAVEL_MODES = {"transit"};
    private static final String API_KEY = "you_api";

    // Store all bus locations at startup
    private Map<String, Map<String, BusLocationData>> allBusLocations = new HashMap<>();

    private static class RouteInfo {
        String routeNumber;
        List<LatLng> routePoints;
        double totalDistance;
        double travelTime;

        RouteInfo(String routeNumber, List<LatLng> routePoints, double totalDistance, double travelTime) {
            this.routeNumber = routeNumber;
            this.routePoints = routePoints;
            this.totalDistance = totalDistance;
            this.travelTime = travelTime;
        }
    }

    private static class BusLocationData {
        double latitude;
        double longitude;
        String direction;
        String routeNumber;

        BusLocationData(double latitude, double longitude, String direction, String routeNumber) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.direction = direction;
            this.routeNumber = routeNumber;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bus_location);

        busLocationsRef = FirebaseDatabase.getInstance().getReference("BusLiveLocation");
        rootBookingsRef = FirebaseDatabase.getInstance().getReference();

        Intent intent = getIntent();
        userPhone = intent.getStringExtra("userPhone");

        timeTextView = findViewById(R.id.tv_time);
        priceTextView = findViewById(R.id.tv_price);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load all bus locations at startup
        loadAllBusLocations();
        fetchBookingDetails();
    }

    private void loadAllBusLocations() {
        busLocationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allBusLocations.clear();

                for (DataSnapshot busSnapshot : dataSnapshot.getChildren()) {
                    String busNumber = busSnapshot.getKey();
                    Map<String, BusLocationData> busRoutes = new HashMap<>();

                    for (DataSnapshot routeSnapshot : busSnapshot.getChildren()) {
                        String routeNumber = routeSnapshot.getKey();
                        Double latitude = routeSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = routeSnapshot.child("longitude").getValue(Double.class);
                        String direction = routeSnapshot.child("direction").getValue(String.class);

                        if (latitude != null && longitude != null) {
                            busRoutes.put(routeNumber, new BusLocationData(
                                    latitude, longitude,
                                    direction != null ? direction : "unknown",
                                    routeNumber));
                        }
                    }

                    if (!busRoutes.isEmpty()) {
                        allBusLocations.put(busNumber, busRoutes);
                    }
                }

                Log.d(TAG, "Loaded " + allBusLocations.size() + " buses with locations");
                // Now we can filter and display buses when route data is available
                if (!availableRoutes.isEmpty()) {
                    displayFilteredBuses();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load bus locations: " + databaseError.getMessage());
            }
        });
    }

    private void displayFilteredBuses() {
        if (mMap == null || startLocationName == null || endLocationName == null || allBusLocations.isEmpty()) {
            return;
        }

        // Clear existing markers and polylines
        List<Marker> markersToRemove = new ArrayList<>(busMarkers.values());
        for (Marker marker : markersToRemove) {
            marker.remove();
        }
        busMarkers.clear();
        clearBusPolylines();

        // Get all available route numbers from our route data
        Set<String> availableRouteNumbers = availableRoutes.keySet();
        LatLng startLocation = BusRouteData.getLatLngForStop(startLocationName);
        Marker nearestBusMarker = null;
        double minDistance = Double.MAX_VALUE;

        // Iterate through all pre-loaded bus locations
        for (Map.Entry<String, Map<String, BusLocationData>> busEntry : allBusLocations.entrySet()) {
            String busNumber = busEntry.getKey();
            Map<String, BusLocationData> busRoutes = busEntry.getValue();

            for (BusLocationData busData : busRoutes.values()) {
                String routeNumber = busData.routeNumber;

                // Only process if this route is one of our available routes AND matches the selected route
                if (availableRouteNumbers.contains(routeNumber) &&
                        (selectedRouteId == null || routeNumber.equals(selectedRouteId))) {

                    // Check if bus direction matches the route direction
                    boolean isForwardDirection = BusRouteData.isForwardDirection(
                            routeNumber, startLocationName, endLocationName);
                    boolean isBusForward = "forward".equalsIgnoreCase(busData.direction);

                    // Only process bus if it's moving in the correct direction
                    if (isBusForward == isForwardDirection) {
                        LatLng busPosition = new LatLng(busData.latitude, busData.longitude);

                        // Get the route stops for this route
                        List<BusRouteData.BusStop> routeStops = BusRouteData.getBusStopsForRoute(routeNumber);
                        if (routeStops == null) continue;

                        // Find indices of start location and bus position in the route
                        int startIndex = -1;
                        int busIndex = -1;

                        for (int i = 0; i < routeStops.size(); i++) {
                            if (routeStops.get(i).name.equalsIgnoreCase(startLocationName)) {
                                startIndex = i;
                            }
                            // Find the nearest stop to the bus position
                            if (calculateDistance(busPosition, routeStops.get(i).location) < 100) { // 100m threshold
                                busIndex = i;
                            }
                        }

                        // Only show the bus if:
                        // 1. For forward direction: bus is before start location (busIndex < startIndex)
                        // 2. For backward direction: bus is after start location (busIndex > startIndex)
                        boolean shouldShowBus = false;
                        if (isForwardDirection) {
                            shouldShowBus = (busIndex != -1 && busIndex < startIndex);
                        } else {
                            shouldShowBus = (busIndex != -1 && busIndex > startIndex);
                        }

                        if (shouldShowBus) {
                            // Create and add bus marker
                            Bitmap busIcon = createBusIcon();
                            String markerTitle = "Bus " + busNumber + " (" + routeNumber + ") - " + busData.direction;

                            Marker busMarker = mMap.addMarker(new MarkerOptions()
                                    .position(busPosition)
                                    .title(markerTitle)
                                    .icon(busIcon != null ? BitmapDescriptorFactory.fromBitmap(busIcon) :
                                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                            if (busMarker != null) {
                                busMarkers.put(busNumber, busMarker);

                                // Find nearest bus to start location
                                if (startLocation != null) {
                                    double distance = calculateDistance(startLocation, busPosition);
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                        nearestBusMarker = busMarker;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Draw polylines to all buses
        drawPolylinesToBuses();

        // Draw special black polyline to nearest bus
        if (nearestBusMarker != null && startLocation != null) {
            drawNearestBusPolyline(startLocation, nearestBusMarker.getPosition());
        }
    }


    private void fetchBookingDetails() {
        if (userPhone == null || userPhone.isEmpty()) {
            Log.w(TAG, "User phone not available, cannot fetch booking details");
            runOnUiThread(() -> {
                Toast.makeText(bus_location.this, "User phone not available.", Toast.LENGTH_SHORT).show();
                displayJourneyInfo();
                loadRoutesFromBusRouteData();
            });
            return;
        }



        Query userBookingsQuery = rootBookingsRef.child("Bookings").child(userPhone).orderByChild("date").limitToLast(1);

        userBookingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Booking found in 'Bookings' for user: " + userPhone);
                    processBookingSnapshot(dataSnapshot.getChildren().iterator().next());
                } else {
                    Log.d(TAG, "No booking found in 'Bookings' for user: " + userPhone + ", checking 'Booking_for_others'");
                    Query otherBookingsQuery = rootBookingsRef.child("Booking_for_others").child(userPhone).orderByChild("date").limitToLast(1);
                    otherBookingsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot otherDataSnapshot) {
                            if (otherDataSnapshot.exists()) {
                                Log.d(TAG, "Booking found in 'Booking_for_others' for user: " + userPhone);
                                processBookingSnapshot(otherDataSnapshot.getChildren().iterator().next());
                            } else {
                                Log.d(TAG, "No active ticket found for user: " + userPhone + " in either node.");
                                runOnUiThread(() -> {
                                    Toast.makeText(bus_location.this, "No active ticket found", Toast.LENGTH_SHORT).show();
                                    displayJourneyInfo();
                                    loadRoutesFromBusRouteData();
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Failed to load booking details from 'Booking_for_others': " + databaseError.getMessage());
                            runOnUiThread(() -> {
                                displayJourneyInfo();
                                loadRoutesFromBusRouteData();
                            });
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load booking details from 'Bookings': " + databaseError.getMessage());
                runOnUiThread(() -> {
                    displayJourneyInfo();
                    loadRoutesFromBusRouteData();
                });
            }
        });
    }

    private void processBookingSnapshot(DataSnapshot bookingSnapshot) {
        startLocationName = bookingSnapshot.child("startStop").getValue(String.class);
        endLocationName = bookingSnapshot.child("destinationStop").getValue(String.class);
        bookingPersonCount = bookingSnapshot.child("personCount").getValue(Integer.class) != null ?
                bookingSnapshot.child("personCount").getValue(Integer.class) : 0;

        // Simplified price handling
        try {
            Object priceObj = bookingSnapshot.child("price").getValue();
            if (priceObj != null) {
                if (priceObj instanceof Double) {
                    bookingPrice = ((Double) priceObj).floatValue();
                } else if (priceObj instanceof Long) {
                    bookingPrice = ((Long) priceObj).floatValue();
                } else if (priceObj instanceof Float) {
                    bookingPrice = (Float) priceObj;
                } else if (priceObj instanceof String) {
                    bookingPrice = Float.parseFloat((String) priceObj);
                } else {
                    bookingPrice = 0.0f;
                }
            } else {
                bookingPrice = 0.0f;
            }
        } catch (Exception e) {
            bookingPrice = 0.0f;
            Log.e(TAG, "Error parsing ticket price: " + e.getMessage());
        }

        bookingDate = bookingSnapshot.child("date").getValue(String.class);
        bookingTime = bookingSnapshot.child("time").getValue(String.class);
        isScanned = bookingSnapshot.child("isScanned").getValue(Boolean.class) != null ?
                bookingSnapshot.child("isScanned").getValue(Boolean.class) : false;

        runOnUiThread(() -> {
            TextView startText = findViewById(R.id.tv_start_location);
            TextView endText = findViewById(R.id.tv_end_location);
            TextView routeText = findViewById(R.id.tv_route);
            TextView statusText = findViewById(R.id.tv_status);

            if (startText != null && startLocationName != null) {
                startText.setText(startLocationName);
            }
            if (endText != null && endLocationName != null) {
                endText.setText(endLocationName);
            }
            if (timeTextView != null && bookingTime != null) {
                timeTextView.setText("Time: " + bookingTime);
            }
            if (priceTextView != null) {
                priceTextView.setText(String.format(Locale.getDefault(), "₹%.2f", bookingPrice));
            }
            if (routeText != null) {
                routeText.setText("Persons: " + bookingPersonCount);
            }
            if (statusText != null) {
                statusText.setText(isScanned ? "USED" : "VALID");
                statusText.setBackgroundResource(isScanned ?
                        R.drawable.status_background_used : R.drawable.status_background);
            }
        });

        Log.d(TAG, String.format(Locale.getDefault(),
                "Booking details loaded: %s to %s, %d persons, ₹%.2f, %s %s, Scanned: %b",
                startLocationName, endLocationName, bookingPersonCount, bookingPrice,
                bookingDate, bookingTime, isScanned));

        loadRoutesFromBusRouteData();
    }
    private void loadRoutesFromBusRouteData() {
        if (startLocationName == null || endLocationName == null) {
            Log.w(TAG, "Start or end location not available, cannot load routes");
            return;
        }

        availableRoutes.clear();
        List<String> routeNumbers = BusRouteData.getRoutesBetweenStops(startLocationName, endLocationName);

        if (routeNumbers.isEmpty()) {
            Toast.makeText(this, "No routes available between the selected stops.", Toast.LENGTH_SHORT).show();
            addFallbackRouteMarkers();
            return;
        }

        for (String routeNumber : routeNumbers) {
            List<LatLng> filteredPoints = BusRouteData.getFilteredStopsForRoute(routeNumber, startLocationName, endLocationName);

            if (!filteredPoints.isEmpty()) {
                double totalDistance = calculateRouteDistance(filteredPoints);
                double travelTime = (totalDistance / 1000.0) / SPEED_KMH * 60;

                RouteInfo routeInfo = new RouteInfo(routeNumber, filteredPoints, totalDistance, travelTime);
                availableRoutes.put(routeNumber, routeInfo);
            }
        }

        if (!availableRoutes.isEmpty()) {
            String shortestRouteId = findShortestRoute();
            displayAllRoutes(shortestRouteId);
            drawRouteMarkers(shortestRouteId);
            adjustCameraToShowRoutes();
            showRouteInformation();
            displayFilteredBuses();
        } else {
            addFallbackRouteMarkers();
        }
    }

    private void displayJourneyInfo() {
        TextView routeText = findViewById(R.id.tv_route);
        TextView statusText = findViewById(R.id.tv_status);
        TextView startText = findViewById(R.id.tv_start_location);
        TextView endText = findViewById(R.id.tv_end_location);
        TextView timeText = findViewById(R.id.tv_time);
        TextView priceText = findViewById(R.id.tv_price);

        if (startLocationName == null || endLocationName == null) {
            Intent intent = getIntent();
            startLocationName = intent.getStringExtra("startLocation");
            endLocationName = intent.getStringExtra("endLocation");
            currentRouteNumber = intent.getStringExtra("routeNumber");
        }

        if (routeText != null) {
            routeText.setText("Persons: " + bookingPersonCount);
        }
        if (statusText != null) {
            statusText.setText(isScanned ? "USED" : "VALID");
            statusText.setBackgroundResource(isScanned ?
                    R.drawable.status_background_used : R.drawable.status_background);
        }
        if (startText != null && startLocationName != null) {
            startText.setText(startLocationName);
        }
        if (endText != null && endLocationName != null) {
            endText.setText(endLocationName);
        }
        if (timeText != null && bookingTime != null) {
            timeText.setText("Time: " + bookingTime);
        }
        if (priceText != null) {
            priceText.setText(String.format("₹%.2f", bookingPrice));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        mMap.setOnPolylineClickListener(polyline -> {
            String routeId = (String) polyline.getTag();
            if (routeId != null) {
                selectRoute(routeId);
            }
        });

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mMap.setOnCameraIdleListener(() -> {
            float zoom = mMap.getCameraPosition().zoom;
            adjustTextMarkersVisibility(zoom);
        });
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = new LatLng(
                                    location.getLatitude(),
                                    location.getLongitude());

                            // Add user location marker
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Your Location")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                            // Draw walking route to nearest bus stop
                            LatLng startLocation = BusRouteData.getLatLngForStop(startLocationName);
                            if (startLocation != null) {
                                drawWalkingRoute(currentLocation, startLocation);
                            }

                            drawPolylinesToBuses();
                        } else {
                            // Handle default location case
                            currentLocation = new LatLng(22.557569214996526, 88.39953748823015);
                            mMap.addMarker(new MarkerOptions()
                                    .position(currentLocation)
                                    .title("Your Location (Default)")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                            drawPolylinesToBuses();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get last known location: " + e.getMessage());
                        currentLocation = new LatLng(22.557569214996526, 88.39953748823015);
                        mMap.addMarker(new MarkerOptions()
                                .position(currentLocation)
                                .title("Your Location (Default)")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                        drawPolylinesToBuses();
                    });
        }
    }


    private double calculateRouteDistance(List<LatLng> routePoints) {
        double totalDistance = 0.0;
        for (int i = 0; i < routePoints.size() - 1; i++) {
            Location startLoc = new Location("start");
            startLoc.setLatitude(routePoints.get(i).latitude);
            startLoc.setLongitude(routePoints.get(i).longitude);
            Location endLoc = new Location("end");
            endLoc.setLatitude(routePoints.get(i + 1).latitude);
            endLoc.setLongitude(routePoints.get(i + 1).longitude);
            totalDistance += startLoc.distanceTo(endLoc);
        }
        return totalDistance;
    }

    private String findShortestRoute() {
        String shortestRouteId = null;
        double shortestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, RouteInfo> entry : availableRoutes.entrySet()) {
            if (entry.getValue().totalDistance < shortestDistance) {
                shortestDistance = entry.getValue().totalDistance;
                shortestRouteId = entry.getKey();
            }
        }
        return shortestRouteId;
    }

    private void displayAllRoutes(String shortestRouteId) {
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();
        for (Map.Entry<String, RouteInfo> entry : availableRoutes.entrySet()) {
            String routeId = entry.getKey();
            RouteInfo routeInfo = entry.getValue();
            boolean isSelectedRoute = routeId.equals(selectedRouteId);
            float alpha = isSelectedRoute ? 1.0f : 0.4f;
            float width = isSelectedRoute ? 10f : 6f;
            int color = Color.BLUE;
            int colorWithAlpha = Color.argb((int)(alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(routeInfo.routePoints)
                    .width(width)
                    .color(colorWithAlpha)
                    .clickable(true)
                    .geodesic(true);
            Polyline polyline = mMap.addPolyline(polylineOptions);
            polyline.setTag(routeId);
            routePolylines.add(polyline);
        }
        if (selectedRouteId == null && shortestRouteId != null) {
            selectedRouteId = shortestRouteId;
        }
    }

    private void selectRoute(String routeId) {
        if (routeId.equals(selectedRouteId)) {
            return;
        }
        selectedRouteId = routeId;
        String shortestRouteId = findShortestRoute();
        displayAllRoutes(shortestRouteId);
        drawRouteMarkers(selectedRouteId);
        RouteInfo selectedRoute = availableRoutes.get(routeId);
        if (selectedRoute != null) {
            String message = String.format("Selected Route: %s\nDistance: %.2f km\nEstimated Time: %.1f minutes",
                    selectedRoute.routeNumber,
                    selectedRoute.totalDistance / 1000.0,
                    selectedRoute.travelTime);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            currentRouteNumber = selectedRoute.routeNumber;

            // Refresh the bus display to show only buses for the selected route
            displayFilteredBuses();
        }
    }
    private void drawRouteMarkers(String routeId) {
        for (Marker marker : routeStopMarkers) {
            marker.remove();
        }
        routeStopMarkers.clear();

        RouteInfo routeInfo = availableRoutes.get(routeId);
        if (routeInfo == null || routeInfo.routePoints.isEmpty()) {
            return;
        }

        List<LatLng> filteredRoutePoints = routeInfo.routePoints;
        for (int i = 0; i < filteredRoutePoints.size(); i++) {
            LatLng stopLatLng = filteredRoutePoints.get(i);
            String stopName = BusRouteData.getStopNameByLatLng(stopLatLng);

            if (stopName != null && !stopName.equals("helper")) {
                MarkerOptions markerOptions = new MarkerOptions().position(stopLatLng);

                if (i == 0 && stopName.equalsIgnoreCase(startLocationName)) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            .title("Board at: " + stopName);
                } else if (i == filteredRoutePoints.size() - 1 && stopName.equalsIgnoreCase(endLocationName)) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .title("Alight at: " + stopName);
                } else {
                    // White circle for regular stops
                    markerOptions.icon(createCircleMarker(Color.WHITE)).title(stopName);
                }

                Marker stopMarker = mMap.addMarker(markerOptions);
                routeStopMarkers.add(stopMarker);

                LatLng labelPosition = new LatLng(stopLatLng.latitude + 0.00009, stopLatLng.longitude);
                MarkerOptions textMarkerOptions = new MarkerOptions()
                        .position(labelPosition)
                        .icon(createTextMarker(stopName))
                        .anchor(0.5f, 1f)
                        .title("TEXT_" + stopName);
                Marker textMarker = mMap.addMarker(textMarkerOptions);
                routeStopMarkers.add(textMarker);
            }
        }
        adjustTextMarkersVisibility(mMap.getCameraPosition().zoom);
    }
    private BitmapDescriptor createCircleMarker(int color) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (12 * density); // 12dp size for both types
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // White fill for stop names
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Gray border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1 * density);
        borderPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (borderPaint.getStrokeWidth() / 2), borderPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    private BitmapDescriptor createTextMarker(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(40);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);
        float textWidth = paint.measureText(text);
        int padding = 20;
        int width = (int) textWidth + padding;
        int height = 80;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 15, 15, backgroundPaint);
        canvas.drawText(text, padding / 2, height / 2 + 10, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void addIntermediateCirclesAlongRoute(LatLng start, LatLng end) {
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        float distance = results[0];
        int circleSpacing = 150;
        int numCircles = (int) (distance / circleSpacing);

        if (numCircles <= 0) return;

        double latStep = (end.latitude - start.latitude) / (numCircles + 1);
        double lngStep = (end.longitude - start.longitude) / (numCircles + 1);

        for (int i = 1; i <= numCircles; i++) {
            double newLat = start.latitude + (latStep * i);
            double newLng = start.longitude + (lngStep * i);
            LatLng circlePosition = new LatLng(newLat, newLng);

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(circlePosition)
                    .icon(createBlackCircleMarker()) // Now creates yellow circles
                    .anchor(0.5f, 0.5f)
                    .zIndex(1);
            Marker circleMarker = mMap.addMarker(markerOptions);
            routeStopMarkers.add(circleMarker);
        }
    }

    private BitmapDescriptor createBlackCircleMarker() {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (12 * density); // Same 12dp size as white circles
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Yellow fill for intermediate points
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Blue border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLUE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1 * density);
        borderPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (borderPaint.getStrokeWidth() / 2), borderPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void adjustTextMarkersVisibility(float zoomLevel) {
        boolean showText = zoomLevel > 15.8;
        for (Marker marker : routeStopMarkers) {
            if (marker.getTitle() != null && marker.getTitle().startsWith("TEXT_")) {
                marker.setVisible(showText);
            }
        }
    }

    private void adjustCameraToShowRoutes() {
        if (availableRoutes.isEmpty()) {
            return;
        }
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (RouteInfo routeInfo : availableRoutes.values()) {
            for (LatLng point : routeInfo.routePoints) {
                boundsBuilder.include(point);
            }
        }
        if (currentLocation != null) {
            boundsBuilder.include(currentLocation);
        }
        try {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error moving camera: " + e.getMessage());
            RouteInfo firstRoute = availableRoutes.values().iterator().next();
            if (!firstRoute.routePoints.isEmpty()) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstRoute.routePoints.get(0), 12));
            }
        }
    }

    private void showRouteInformation() {
        if (availableRoutes.isEmpty()) {
            return;
        }
        StringBuilder routeInfo = new StringBuilder();
        routeInfo.append("Available Routes:\n");
        String shortestRouteId = findShortestRoute();
        for (Map.Entry<String, RouteInfo> entry : availableRoutes.entrySet()) {
            RouteInfo route = entry.getValue();
            boolean isShortestRoute = entry.getKey().equals(shortestRouteId);
            routeInfo.append(String.format("%s Route %s: %.2f km, %.1f min\n",
                    isShortestRoute ? "⭐" : "•",
                    route.routeNumber,
                    route.totalDistance / 1000.0,
                    route.travelTime));
        }
        routeInfo.append("\n⭐ = Shortest route (full opacity)");
        routeInfo.append("\nTap on any route to select it");
        Toast.makeText(this, routeInfo.toString(), Toast.LENGTH_LONG).show();
    }

    private void addFallbackRouteMarkers() {
        LatLng startStop = new LatLng(22.5726, 88.3639);
        LatLng endStop = new LatLng(22.5359, 88.3467);
        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();
        for (Marker marker : routeStopMarkers) {
            marker.remove();
        }
        routeStopMarkers.clear();
        PolylineOptions routePolylineOptions = new PolylineOptions()
                .add(startStop)
                .add(endStop)
                .width(8f)
                .color(Color.BLUE)
                .clickable(false)
                .geodesic(true);
        Polyline polyline = mMap.addPolyline(routePolylineOptions);
        routePolylines.add(polyline);
        mMap.addMarker(new MarkerOptions()
                .position(startStop)
                .title("Start: " + (startLocationName != null ? startLocationName : "Unknown"))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        mMap.addMarker(new MarkerOptions()
                .position(endStop)
                .title("End: " + (endLocationName != null ? endLocationName : "Unknown"))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        addIntermediateCirclesAlongRoute(startStop, endStop);

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(startStop)
                .include(endStop)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void drawPolylinesToBuses() {
        LatLng startLocationForPolyline = BusRouteData.getLatLngForStop(startLocationName);

        if (startLocationForPolyline == null) {
            Log.d(TAG, "Start location not available for polyline drawing");
            return;
        }

        if (busMarkers.isEmpty()) {
            Log.d(TAG, "Bus locations not available for polyline drawing");
            return;
        }

        clearBusPolylines();

        // Create a copy of the busMarkers entries to avoid ConcurrentModificationException
        List<Map.Entry<String, Marker>> busEntries = new ArrayList<>(busMarkers.entrySet());

        Marker nearestBusMarker = null;
        double minDistance = Double.MAX_VALUE;

        // Find nearest bus first using the copied list
        for (Map.Entry<String, Marker> entry : busEntries) {
            double distance = calculateDistance(startLocationForPolyline, entry.getValue().getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearestBusMarker = entry.getValue();
            }
        }

        // Draw polylines for all buses using the copied list
        for (Map.Entry<String, Marker> entry : busEntries) {
            String busNumber = entry.getKey();
            LatLng busPosition = entry.getValue().getPosition();

            if (entry.getValue() != nearestBusMarker) {
                // For selected route, draw through intermediate stops
                if (selectedRouteId != null) {
                    drawRoutePolylineForBus(startLocationForPolyline, busPosition, busNumber);
                } else {
                    // For non-selected or when no route is selected, draw direct line
                    getRoute(startLocationForPolyline, busPosition, busNumber, Color.GRAY, 0);
                }
            }
        }

        if (nearestBusMarker != null) {
            drawNearestBusPolyline(startLocationForPolyline, nearestBusMarker.getPosition());
        }
    }

    private void drawRoutePolylineForBus(LatLng start, LatLng busLocation, String busId) {
        if (selectedRouteId == null) {
            Log.d(TAG, "No route selected, skipping route polyline drawing");
            return;
        }

        // Verify this bus is actually on the selected route
        if (!allBusLocations.containsKey(busId)) {
            Log.d(TAG, "Bus " + busId + " not found in allBusLocations");
            return;
        }

        Map<String, BusLocationData> busRoutes = allBusLocations.get(busId);
        if (!busRoutes.containsKey(selectedRouteId)) {
            Log.d(TAG, "Bus " + busId + " doesn't operate on route " + selectedRouteId);
            return;
        }

        RouteInfo selectedRoute = availableRoutes.get(selectedRouteId);
        if (selectedRoute == null || selectedRoute.routePoints.isEmpty()) {
            Log.d(TAG, "Selected route " + selectedRouteId + " has no points or doesn't exist");
            return;
        }

        // Find the closest point in the route to the bus location
        int closestPointIndex = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < selectedRoute.routePoints.size(); i++) {
            double distance = calculateDistance(busLocation, selectedRoute.routePoints.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestPointIndex = i;
            }
        }

        // Create a new list of points from start to bus location via route points
        List<LatLng> points = new ArrayList<>();
        points.add(start);

        // Add all route points up to the closest point
        for (int i = 0; i <= closestPointIndex; i++) {
            points.add(selectedRoute.routePoints.get(i));
        }

        // Finally add the bus location
        points.add(busLocation);

        // Create the polyline with appropriate styling
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(points)
                .width(4f)
                .color(Color.BLACK)
                .zIndex(5) // Below the nearest bus polyline but above others
                .geodesic(true);

        // Remove existing polyline if it exists
        if (busPolylines.containsKey(busId + "_route")) {
            busPolylines.get(busId + "_route").remove();
        }

        // Add the new polyline to the map and store reference
        Polyline polyline = mMap.addPolyline(polylineOptions);
        busPolylines.put(busId + "_route", polyline);

        // Add a small marker at the connection point between route and bus
        LatLng connectionPoint = selectedRoute.routePoints.get(closestPointIndex);
        MarkerOptions connectionMarkerOptions = new MarkerOptions()
                .position(connectionPoint)
                .icon(createConnectionPointMarker())
                .anchor(0.5f, 0.5f)
                .zIndex(6)
                .title("Connection point for bus " + busId);

        Marker connectionMarker = mMap.addMarker(connectionMarkerOptions);
        busMarkers.put(busId + "_connection", connectionMarker);
    }

    private BitmapDescriptor createConnectionPointMarker() {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (10 * density); // 10dp size
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);


        // Red fill for connection points
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // White border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1 * density);
        borderPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (borderPaint.getStrokeWidth() / 2), borderPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    private void drawWalkingRoute(LatLng origin, LatLng destination) {
        if (origin == null || destination == null) {
            return;
        }

        // Remove existing walking polyline if any
        if (walkingPolyline != null) {
            walkingPolyline.remove();
        }

        // Create dotted polyline options
        PolylineOptions walkingRouteOptions = new PolylineOptions()
                .add(origin, destination)
                .width(dpToPx(WALKING_ROUTE_WIDTH_DP))
                .color(WALKING_ROUTE_COLOR)
                .pattern(Arrays.asList(new Dot(), new Gap(dpToPx(7))));

        // Add to map
        walkingPolyline = mMap.addPolyline(walkingRouteOptions);

        // Add walking icon at midpoint
        LatLng midPoint = new LatLng(
                (origin.latitude + destination.latitude) / 2,
                (origin.longitude + destination.longitude) / 2
        );

        BitmapDescriptor walkingIcon = getResizedBitmapDescriptor(
                this, R.drawable.user_icon, 20, 20);
        if (walkingIcon != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(midPoint)
                    .icon(walkingIcon)
                    .anchor(0.5f, 0.5f));
        }
    }

    private BitmapDescriptor getResizedBitmapDescriptor(Context context, int resourceId, int widthDp, int heightDp) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resourceId, options);

            options.inSampleSize = calculateInSampleSize(options, dpToPx(widthDp), dpToPx(heightDp));
            options.inJustDecodeBounds = false;

            Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            if (originalBitmap != null) {
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                        originalBitmap,
                        dpToPx(widthDp),
                        dpToPx(heightDp),
                        true);
                return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resizing bitmap: " + e.getMessage());
        }
        return null;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    private void drawNearestBusPolyline(LatLng start, LatLng end) {
        if (busPolylines.containsKey("nearest")) {
            busPolylines.get("nearest").remove();
            busPolylines.remove("nearest");
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(start)
                .add(end)
                .width(4f)
                .color(Color.BLACK)
                .zIndex(10);

        Polyline polyline = mMap.addPolyline(polylineOptions);
        busPolylines.put("nearest", polyline);
    }

    private void clearBusPolylines() {
        // Create a copy of the keys to avoid ConcurrentModificationException
        List<String> polylineKeys = new ArrayList<>(busPolylines.keySet());

        for (String key : polylineKeys) {
            Polyline polyline = busPolylines.get(key);
            if (polyline != null) {
                polyline.remove();
            }
            busPolylines.remove(key);
        }

        // Also clear any connection markers
        List<String> markerKeys = new ArrayList<>(busMarkers.keySet());
        for (String key : markerKeys) {
            if (key.endsWith("_connection")) {
                Marker marker = busMarkers.get(key);
                if (marker != null) {
                    marker.remove();
                }
                busMarkers.remove(key);
            }
        }
    }

    private double calculateDistance(LatLng start, LatLng end) {
        Location startLoc = new Location("start");
        startLoc.setLatitude(start.latitude);
        startLoc.setLongitude(start.longitude);

        Location endLoc = new Location("end");
        endLoc.setLatitude(end.latitude);
        endLoc.setLongitude(end.longitude);

        return startLoc.distanceTo(endLoc);
    }

    private void getRoute(LatLng origin, LatLng destination, String busId, int color, int modeIndex) {
        if (origin == null || destination == null) {
            Log.d(TAG, "Skipping route calculation - origin or destination is null");
            return;
        }

        if (modeIndex >= TRAVEL_MODES.length) {
            Log.e(TAG, "Failed to find route for bus " + busId + " after trying all travel modes. Drawing direct line.");
            drawDirectLine(origin, destination, busId, color);
            return;
        }

        String travelMode = TRAVEL_MODES[modeIndex];
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=" + travelMode +
                "&key=" + API_KEY;

        Log.d(TAG, "Requesting route for bus " + busId + " with mode: " + travelMode);
        new FetchRouteTask(busId, color, modeIndex, origin, destination).execute(url);
    }

    private void drawDirectLine(LatLng origin, LatLng destination, String busId, int color) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(origin)
                .add(destination)
                .width(4f)
                .color(color)
                .clickable(false);
        Polyline polyline = mMap.addPolyline(polylineOptions);
        busPolylines.put(busId, polyline);
    }

    private class FetchRouteTask extends AsyncTask<String, Void, String> {
        private String busId;
        private int color;
        private int modeIndex;
        private LatLng origin;
        private LatLng destination;

        FetchRouteTask(String busId, int color, int modeIndex, LatLng origin, LatLng destination) {
            this.busId = busId;
            this.color = color;
            this.modeIndex = modeIndex;
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        protected String doInBackground(String... urls) {
            String data = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();

                InputStream iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                br.close();
                iStream.close();
                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Exception while fetching route: " + e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && !result.isEmpty()) {
                new ParserTask(busId, color, modeIndex, origin, destination).execute(result);
            } else {
                getRoute(origin, destination, busId, color, modeIndex + 1);
            }
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        private String busId;
        private int color;
        private int modeIndex;
        private LatLng origin;
        private LatLng destination;

        ParserTask(String busId, int color, int modeIndex, LatLng origin, LatLng destination) {
            this.busId = busId;
            this.color = color;
            this.modeIndex = modeIndex;
            this.origin = origin;
            this.destination = destination;
        }

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            try {
                jObject = new JSONObject(jsonData[0]);
                JSONArray jRoutes = jObject.getJSONArray("routes");

                for (int i = 0; i < jRoutes.length(); i++) {
                    JSONArray jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<>();

                    for (int j = 0; j < jLegs.length(); j++) {
                        JSONArray jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString(list.get(l).latitude));
                                hm.put("lng", Double.toString(list.get(l).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            if (result.size() < 1) {
                Log.d(TAG, "No route found for bus " + busId + " with mode: " + TRAVEL_MODES[modeIndex] + ". Retrying...");
                getRoute(origin, destination, busId, color, modeIndex + 1);
                return;
            }

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(6f);
                lineOptions.color(color);
            }

            if (lineOptions != null) {
                Polyline polyline = mMap.addPolyline(lineOptions);
                busPolylines.put(busId, polyline);
            } else {
                Log.d(TAG, "LineOptions are null for bus " + busId + ". Retrying...");
                getRoute(origin, destination, busId, color, modeIndex + 1);
            }
        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;
                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }
            return poly;
        }
    }
    private Map<String, Marker> filterBusesByRoute(String routeNumber) {
        Map<String, Marker> filteredBuses = new HashMap<>();

        for (Map.Entry<String, Marker> entry : busMarkers.entrySet()) {
            String busNumber = entry.getKey();
            if (allBusLocations.containsKey(busNumber)) {
                Map<String, BusLocationData> busRoutes = allBusLocations.get(busNumber);
                if (busRoutes.containsKey(routeNumber)) {
                    filteredBuses.put(busNumber, entry.getValue());
                }
            }
        }

        return filteredBuses;
    }
    private Bitmap createBusIcon() {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.buslocationpicbg);
            if (originalBitmap != null) {
                int width = 80;
                int height = 80;
                return Bitmap.createScaledBitmap(originalBitmap, width, height, false);
            } else {
                Log.w(TAG, "busicon drawable not found, using default marker");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating bus icon: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                currentLocation = new LatLng(22.557569214996526, 88.39953748823015);
                mMap.addMarker(new MarkerOptions()
                        .position(currentLocation)
                        .title("Your Location (Default)")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
                drawPolylinesToBuses();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearBusPolylines();

        if (walkingPolyline != null) {
            walkingPolyline.remove();
        }

        for (Polyline polyline : routePolylines) {
            polyline.remove();
        }
        routePolylines.clear();

        for (Marker marker : routeStopMarkers) {
            marker.remove();
        }
        routeStopMarkers.clear();
    }
}