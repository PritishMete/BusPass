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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteInfoModel;
import com.codebyashish.googledirectionapi.RouteListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class user_map_others extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng userLocation, destinationLocation;
    private Marker destinationMarker;
    private Polyline routePolyline;
    private List<Marker> stopMarkers = new ArrayList<>();

    private AutoCompleteTextView autoCompleteTextView;
    private AutoCompleteTextView autoCompleteTextView2;
    private Button searchButton;
    private Button bookTicketButton;
    private TextView phoneNumberTextView;

    private String routeName;
    private String enteredNumber;
    private String mainuserPhone;
    private Map<String, Marker> busMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.user_map_others);

        initializeViews();

        Intent intent = getIntent();
        enteredNumber = intent.getStringExtra("enteredNumber");
        mainuserPhone = intent.getStringExtra("mainuserPhone");
        phoneNumberTextView.setText("Phone: " + (enteredNumber != null ? enteredNumber : "Not available"));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_GOOGLE_API_KEY");
        }

        setupAutocomplete();
        setupButtonListeners();
    }

    private void initializeViews() {
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteTextView2 = findViewById(R.id.autoCompleteTextView2);
        searchButton = findViewById(R.id.searchButton);
        bookTicketButton = findViewById(R.id.bookTicketButton);
        phoneNumberTextView = findViewById(R.id.phone_number_text_view);
    }

    private void setupAutocomplete() {
        List<String> allStops = BusRouteData.getAllStops();
        ArrayAdapter<String> stopsAdapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,  // Your custom layout
                allStops
        );
        autoCompleteTextView.setAdapter(stopsAdapter);
        autoCompleteTextView2.setAdapter(stopsAdapter);
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView2.setThreshold(1);
    }

    private void setupButtonListeners() {
        searchButton.setOnClickListener(v -> handleSearch());
        bookTicketButton.setOnClickListener(v -> handleBookTicket());
    }

    private void handleSearch() {
        String startLocationName = autoCompleteTextView.getText().toString().trim();
        String endLocationName = autoCompleteTextView2.getText().toString().trim();

        if (startLocationName.isEmpty() || endLocationName.isEmpty()) {
            Toast.makeText(this, "Enter both locations to search", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> filteredRoutes = BusRouteData.getRoutesBetweenStops(startLocationName, endLocationName);

        if (filteredRoutes.isEmpty()) {
            Toast.makeText(this, "No routes available between the selected stops.", Toast.LENGTH_SHORT).show();
            return;
        }

        routeName = filteredRoutes.get(0);
        Toast.makeText(this, "Selected Route: " + routeName, Toast.LENGTH_SHORT).show();

        searchLocation(startLocationName, true);
        searchLocation(endLocationName, false);

        drawRouteForSelectedRoute(routeName, startLocationName, endLocationName);
    }

    private void drawRouteForSelectedRoute(String routeName, String startLocationName, String endLocationName) {
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }

        for (Marker marker : stopMarkers) {
            marker.remove();
        }
        stopMarkers.clear();

        clearNonBusMarkers();

        List<LatLng> filteredRoutePoints = BusRouteData.getFilteredStopsForRoute(routeName, startLocationName, endLocationName);

        if (filteredRoutePoints.isEmpty()) {
            Toast.makeText(this, "No valid stops available for this route.", Toast.LENGTH_SHORT).show();
            return;
        }

        float totalDistanceMeters = 0;
        LatLng previousLatLng = null;

        PolylineOptions busRoutePolylineOptions = new PolylineOptions()
                .width(10)
                .color(Color.BLUE)
                .startCap(new RoundCap())
                .endCap(new RoundCap());

        for (int i = 0; i < filteredRoutePoints.size(); i++) {
            LatLng stopLatLng = filteredRoutePoints.get(i);
            busRoutePolylineOptions.add(stopLatLng);

            if (previousLatLng != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        previousLatLng.latitude, previousLatLng.longitude,
                        stopLatLng.latitude, stopLatLng.longitude,
                        results
                );
                totalDistanceMeters += results[0];
            }

            String stopName = BusRouteData.getStopNameByLatLng(stopLatLng);

            if (stopName != null && !stopName.equals("helper")) {
                MarkerOptions markerOptions = new MarkerOptions().position(stopLatLng);

                if (i == 0 && stopName.equals(startLocationName)) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)).title("Board at: " + stopName);
                } else if (i == filteredRoutePoints.size() - 1 && stopName.equals(endLocationName)) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("Alight at: " + stopName);
                } else {
                    markerOptions.icon(createCircleMarker(Color.WHITE)).title(stopName);
                }

                Marker stopMarker = map.addMarker(markerOptions);
                stopMarkers.add(stopMarker);

                LatLng labelPosition = new LatLng(stopLatLng.latitude + 0.00009, stopLatLng.longitude);

                MarkerOptions textMarkerOptions = new MarkerOptions()
                        .position(labelPosition)
                        .icon(createTextMarker(stopName))
                        .anchor(0.5f, 1f)
                        .title("TEXT_" + stopName);

                Marker textMarker = map.addMarker(textMarkerOptions);
                stopMarkers.add(textMarker);
            }

            if (previousLatLng != null) {
                addCirclesAlongRoute(previousLatLng, stopLatLng);
            }
            previousLatLng = stopLatLng;
        }

        if (!filteredRoutePoints.isEmpty()) {
            routePolyline = map.addPolyline(busRoutePolylineOptions);
        }

        float distanceInKm = totalDistanceMeters / 1000;
        Toast.makeText(this, "Distance: " + String.format("%.2f", distanceInKm) + " km", Toast.LENGTH_SHORT).show();
    }

    private void clearNonBusMarkers() {
        fetchAndDisplayBuses();
    }

    private void addCirclesAlongRoute(LatLng start, LatLng end) {
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
                    .icon(createRouteCircleMarker())
                    .anchor(0.5f, 0.5f)
                    .zIndex(1);

            Marker circleMarker = map.addMarker(markerOptions);
            stopMarkers.add(circleMarker);
        }
    }

    private BitmapDescriptor createRouteCircleMarker() {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (12 * density);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.BLUE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2 * density);
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

    private LatLng searchLocation(String locationName, boolean isStartLocation) {
        LatLng locationLatLng = BusRouteData.getLatLngForStop(locationName);

        if (locationLatLng == null) {
            Toast.makeText(this, "Stop not found: " + locationName, Toast.LENGTH_SHORT).show();
            return null;
        }

        if (isStartLocation) {
            userLocation = locationLatLng;
            map.addMarker(new MarkerOptions().position(userLocation)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        } else {
            destinationLocation = locationLatLng;
            if (destinationMarker != null) destinationMarker.remove();
            destinationMarker = map.addMarker(new MarkerOptions().position(destinationLocation)
                    .title(locationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(locationLatLng, 15));
        return locationLatLng;
    }

    private void handleBookTicket() {
        String start = autoCompleteTextView.getText().toString().trim();
        String end = autoCompleteTextView2.getText().toString().trim();

        if (routeName == null || routeName.isEmpty()) {
            Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (start.isEmpty() || end.isEmpty()) {
            Toast.makeText(this, "Please select both start and end stops", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = calculateTicketPrice(start, end);
        showBookingDialog(start, end, price);
    }

    // ##### LOGIC CHANGE IS HERE #####
    private double calculateTicketPrice(String start, String end) {
        LatLng startLatLng = BusRouteData.getLatLngForStop(start);
        LatLng endLatLng = BusRouteData.getLatLngForStop(end);

        if (startLatLng == null || endLatLng == null) {
            // Default to the minimum possible fare if stops are invalid
            return FareCalculator.calculateFare(0);
        }

        // Calculate the straight-line distance in meters
        float[] results = new float[1];
        Location.distanceBetween(
                startLatLng.latitude, startLatLng.longitude,
                endLatLng.latitude, endLatLng.longitude,
                results
        );

        double distanceInKm = results[0] / 1000.0;
        // Use the slab-based FareCalculator to get the final price
        return FareCalculator.calculateFare(distanceInKm);
    }

    private void showBookingDialog(String start, String end, double price) {
        BookingActivity_others dialog = BookingActivity_others.newInstance(
                start,
                end,
                enteredNumber,  // passengerPhone
                mainuserPhone,   // booker's phone
                price
        );
        dialog.show(getSupportFragmentManager(), "booking_dialog");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        enableLocationAccess();
        getCurrentLocation();
        fetchAndDisplayBuses();

        map.setOnCameraIdleListener(() -> {
            float zoom = map.getCameraPosition().zoom;
            adjustTextMarkersVisibility(zoom);
        });

        map.setOnMarkerClickListener(marker -> {
            if (marker.getTitle() != null && !marker.getTitle().startsWith("TEXT_")) {
                Toast.makeText(this, "Stop: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
            }
            return false;
        });
    }

    private void adjustTextMarkersVisibility(float zoomLevel) {
        boolean showText = zoomLevel > 15.8;

        for (Marker marker : stopMarkers) {
            if (marker.getTitle() != null && marker.getTitle().startsWith("TEXT_")) {
                marker.setVisible(showText);
            }
        }
    }

    private void enableLocationAccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        map.setMyLocationEnabled(true);
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                        addRedMarker(userLocation, "You are here");
                    } else {
                        Toast.makeText(this, "Unable to fetch location. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addRedMarker(LatLng location, String title) {
        if (map != null) {
            map.addMarker(new MarkerOptions()
                    .position(location)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void fetchAndDisplayBuses() {
        DatabaseReference busRef = FirebaseDatabase.getInstance().getReference("bus location");
        busRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Marker marker : busMarkers.values()) {
                    marker.remove();
                }
                busMarkers.clear();

                for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                    String routeName = routeSnapshot.getKey();

                    for (DataSnapshot busSnapshot : routeSnapshot.getChildren()) {
                        String busNumber = busSnapshot.getKey();
                        Double latitude = busSnapshot.child("latitude").getValue(Double.class);
                        Double longitude = busSnapshot.child("longitude").getValue(Double.class);

                        if (latitude != null && longitude != null) {
                            LatLng busLocation = new LatLng(latitude, longitude);

                            BitmapDescriptor busIcon = getResizedBitmapDescriptor(
                                    user_map_others.this,
                                    R.drawable.buslocationpicbg,
                                    45,
                                    40
                            );

                            Marker busMarker = map.addMarker(new MarkerOptions()
                                    .position(busLocation)
                                    .title(routeName)
                                    .icon(busIcon));

                            if (busMarker != null) {
                                busMarkers.put(busNumber, busMarker);
                            }
                        } else {
                            Log.e("BusLocation", "Invalid latitude/longitude for bus: " + busNumber + " under route: " + routeName);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BusLocation", "Firebase error: " + error.getMessage());
                Toast.makeText(user_map_others.this, "Error fetching bus data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BitmapDescriptor createCircleMarker(int color) {
        int size = dpToPx(20);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size/2f, size/2f, size/2f, paint);

        Paint border = new Paint();
        border.setColor(Color.GRAY);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dpToPx(2));
        border.setAntiAlias(true);
        canvas.drawCircle(size/2f, size/2f, size/2f - border.getStrokeWidth()/2, border);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private BitmapDescriptor getResizedBitmapDescriptor(Context context, int resId, int width, int height) {
        Bitmap image = BitmapFactory.decodeResource(context.getResources(), resId);
        Bitmap resized = Bitmap.createScaledBitmap(image, dpToPx(width), dpToPx(height), false);
        return BitmapDescriptorFactory.fromBitmap(resized);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // RouteListener methods
    @Override public void onRouteFailure(ErrorHandling error) {
        Toast.makeText(this, "Route calculation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
    }
    @Override public void onRouteStart() {
        Toast.makeText(this, "Calculating route...", Toast.LENGTH_SHORT).show();
    }
    @Override public void onRouteSuccess(ArrayList<RouteInfoModel> routes, int index) {}
    @Override public void onRouteCancelled() {
        Toast.makeText(this, "Route calculation cancelled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationAccess();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}