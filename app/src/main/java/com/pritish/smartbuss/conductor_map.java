package com.pritish.smartbuss;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.pritish.smartbuss.BusRouteData.getAllStops;

import android.Manifest;
import android.app.Dialog;
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
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codebyashish.googledirectionapi.AbstractRouting;
import com.codebyashish.googledirectionapi.ErrorHandling;
import com.codebyashish.googledirectionapi.RouteDrawing;
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
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class conductor_map extends AppCompatActivity implements OnMapReadyCallback, RouteListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private LatLng destinationLocation, userLocation;

    private Marker destinationMarker;
    private Polyline routePolyline;

    private AutoCompleteTextView autoCompleteTextView;
    private Button searchButton;
    private Button bookTicketButton;
    private Map<String, Marker> busMarkers = new HashMap<>();
    private List<Marker> stopMarkers = new ArrayList<>();

    private String routeName;
    private String selectedStartStopName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);

        BusRouteData.initializeDataFromFirebase(new BusRouteData.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                Log.d("user map", "Bus data loaded successfully!");
                List<String> allRoutes = BusRouteData.getAllRoutes();
            }

            @Override
            public void onDataLoadFailed(String errorMessage) {
                Log.e("user map", "Failed to load bus data: " + errorMessage);
            }
        });

        EdgeToEdge.enable(this);
        setContentView(R.layout.user_map);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.WHITE);
        }

        SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (fragment != null) {
            fragment.getMapAsync(this);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);

        List<String> allStops = getAllStops();
        ArrayAdapter<String> stopsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, allStops);
        autoCompleteTextView.setAdapter(stopsAdapter);
        autoCompleteTextView.setThreshold(1);

        searchButton = findViewById(R.id.searchButton);
        bookTicketButton = findViewById(R.id.bookTicketButton);
        final TextView phoneNumberTextView = findViewById(R.id.phone_number_text_view);
        Intent intent = getIntent();
        String mainuserPhone = intent.getStringExtra("mainuserPhone");
        if (mainuserPhone != null) {
            phoneNumberTextView.setText("Phone Number: " + mainuserPhone);
        } else {
            phoneNumberTextView.setText("Phone Number: Not available");
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "YOUR_GOOGLE_API_KEY");
        }

        searchButton.setOnClickListener(v -> {
            String destinationLocationName = autoCompleteTextView.getText().toString().trim();
            if (!destinationLocationName.isEmpty()) {
                if (userLocation == null) {
                    Toast.makeText(this, "Unable to fetch your current location. Please try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (BusRouteData.getAllStops().isEmpty()) {
                    Toast.makeText(this, "Bus data is still loading. Please wait a moment and try again.", Toast.LENGTH_SHORT).show();
                    return;
                }


                List<NearestStopModel> nearestStops = BusRouteData.getNearestStopsDetailsWithinRadius(userLocation, 1000);
                if (nearestStops.isEmpty()) {
                    Log.d("DEBUG", "Current location: " + userLocation.latitude + ", " + userLocation.longitude);
                    Log.d("DEBUG", "Total stops available: " + BusRouteData.getAllStops().size());
                    Log.d("DEBUG", "First few stops: " + BusRouteData.getAllStops().subList(0, Math.min(5, BusRouteData.getAllStops().size())));

                    Toast.makeText(this, "No nearby stops found within 1km. Try increasing search radius.", Toast.LENGTH_SHORT).show();
                    return;
                }
                showNearestStopsForDestinationDialog(nearestStops, destinationLocationName);
            } else {
                Toast.makeText(this, "Enter a destination to search", Toast.LENGTH_SHORT).show();
            }
        });


        bookTicketButton.setOnClickListener(v -> {
            if (destinationLocation != null && routeName != null && !routeName.isEmpty() && selectedStartStopName != null) {
                String selectedStop = autoCompleteTextView.getText().toString();
                // Get start stop LatLng for accurate distance calculation
                LatLng startLocation = BusRouteData.getLatLngForStop(selectedStartStopName);
                if (startLocation == null) {
                    Toast.makeText(this, "Could not find start stop location.", Toast.LENGTH_SHORT).show();
                    return;
                }
                float ticketPrice = (float) calculateTicketPrice(startLocation, destinationLocation);
                BookingActivity_conductor bookingDialog = new BookingActivity_conductor(selectedStartStopName, selectedStop,  ticketPrice, mainuserPhone);
                bookingDialog.show(getSupportFragmentManager(), "BookingDialog");
            } else {
                Toast.makeText(this, "Please select a destination and route first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ##### LOGIC CHANGE IS HERE #####
    private double calculateTicketPrice(LatLng start, LatLng end) {
        if (start == null || end == null) return 0.0;
        // Calculate the straight-line distance in meters
        float distanceInMeters = calculateDistance(start, end);
        double distanceInKm = distanceInMeters / 1000.0;
        // Use the slab-based FareCalculator to get the final price
        return FareCalculator.calculateFare(distanceInKm);
    }

    private void hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController == null) {
            return;
        }

        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void hideKeyboard() {
        if (autoCompleteTextView != null) {
            autoCompleteTextView.clearFocus();
        }
        View view = this.getCurrentFocus();
        if (view == null && autoCompleteTextView != null) {
            view = autoCompleteTextView;
        }
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showNearestStopsForDestinationDialog(List<NearestStopModel> nearestStops, final String destinationLocationName) {
        final Dialog dialog = new Dialog(this, R.style.AppTheme); // Use your app's theme

        dialog.setContentView(R.layout.dialog_nearest_stops);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText searchEditText = dialog.findViewById(R.id.search_edit_text);
        RecyclerView recyclerView = dialog.findViewById(R.id.nearestStopsRecyclerView);
        Button closeButton = dialog.findViewById(R.id.closeButton);
        TextView titleTextView = dialog.findViewById(R.id.nearest_stops_title);

        if (titleTextView != null) {
            titleTextView.setVisibility(View.VISIBLE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final List<NearestStopModel> initialStopsList = new ArrayList<>(nearestStops);

        NearestStopsAdapter.OnStopSelectedListener stopClickListener = selectedStartStopName -> {
            conductor_map.this.selectedStartStopName = selectedStartStopName;

            LatLng selectedStartStopLocation = BusRouteData.getLatLngForStop(selectedStartStopName);
            if (selectedStartStopLocation == null) {
                Toast.makeText(conductor_map.this, "Selected start stop location data not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            LatLng destLatLng = BusRouteData.getLatLngForStop(destinationLocationName);
            if (destLatLng == null) {
                Toast.makeText(conductor_map.this, "Destination '" + destinationLocationName + "' not found as a known stop.", Toast.LENGTH_LONG).show();
                return;
            }

            List<String> filteredRoutes = BusRouteData.getRoutesBetweenStops(selectedStartStopName, destinationLocationName);
            if (filteredRoutes.isEmpty()) {
                Toast.makeText(conductor_map.this, "No direct bus routes found from " + selectedStartStopName + " to " + destinationLocationName, Toast.LENGTH_LONG).show();
                return;
            }

            this.destinationLocation = destLatLng;
            this.routeName = filteredRoutes.get(0); // Select the first route by default

            if (map != null) map.clear();
            fetchAndDisplayBuses();

            if (userLocation != null) {
                drawWalkingRoute(userLocation, selectedStartStopLocation);
            }
            drawRouteForSelectedRoute(this.routeName, selectedStartStopName, destinationLocationName);

            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            if (userLocation != null) boundsBuilder.include(userLocation);
            boundsBuilder.include(selectedStartStopLocation);
            if (this.destinationLocation != null) boundsBuilder.include(this.destinationLocation);

            try {
                if (map != null) {
                    LatLngBounds bounds = boundsBuilder.build();
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPx(70)));
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Cannot zoom to journey, not enough points or map not ready: " + e.getMessage());
                if (map != null && this.destinationLocation != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(this.destinationLocation, 14));
                else if (map != null && userLocation != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
            }

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            hideKeyboard();
        };

        final NearestStopsAdapter adapter = new NearestStopsAdapter(initialStopsList, stopClickListener);
        recyclerView.setAdapter(adapter);

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            Log.e(TAG, "Search EditText (search_edit_text) not found in dialog_nearest_stops.xml. Search will not work.");
        }

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                hideKeyboard();
            });
        } else {
            Log.e(TAG, "Close button not found in dialog layout.");
        }

        dialog.show();
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
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title("Board at: " + stopName);
                } else if (i == filteredRoutePoints.size() - 1 && stopName.equals(endLocationName)) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("Alight at: " + stopName);
                } else {
                    markerOptions.icon(createCircleMarker(Color.WHITE)).title(stopName);
                }
                Marker stopM = map.addMarker(markerOptions);
                if (stopM != null) stopMarkers.add(stopM);

                LatLng labelPosition = new LatLng(stopLatLng.latitude + 0.00009, stopLatLng.longitude);
                MarkerOptions textMarkerOptions = new MarkerOptions()
                        .position(labelPosition)
                        .icon(createTextMarker(stopName))
                        .anchor(0.5f, 1f);
                Marker textM = map.addMarker(textMarkerOptions);
                if (textM != null) stopMarkers.add(textM);
            }

            if (previousLatLng != null) {
                addCirclesAlongRoute(previousLatLng, stopLatLng);
            }
            previousLatLng = stopLatLng;
        }

        if (!filteredRoutePoints.isEmpty()) {
            routePolyline = map.addPolyline(busRoutePolylineOptions);
        }

        float distanceInKm = totalDistanceMeters / 1000f;
        Toast.makeText(this, "Route Distance: " + String.format("%.2f", distanceInKm) + " km", Toast.LENGTH_SHORT).show();
        System.out.println("Bus route polyline drawn. Total distance: " + String.format("%.2f", distanceInKm) + " km");
    }

    private void drawWalkingRoute(LatLng origin, LatLng destination) {
        RouteDrawing walkingRouteDrawing = new RouteDrawing.Builder()
                .context(conductor_map.this)
                .travelMode(AbstractRouting.TravelMode.WALKING)
                .withListener(new RouteListener() {
                    Polyline walkingPolyline;

                    @Override
                    public void onRouteFailure(ErrorHandling e) {
                        drawSimpleDottedLine(origin, destination);
                        String walkErrorMessage = "Walking route calculation failed";
                        if (e != null) {
                            String detail = e.getMessage();
                            if (detail != null && !detail.isEmpty()) {
                                walkErrorMessage += ": " + detail;
                            } else {
                                walkErrorMessage += ": " + e.toString();
                            }
                            Log.e(TAG, "Walking route failure: " + walkErrorMessage);
                        } else {
                            Log.e(TAG, "Walking route failure: ErrorHandling object was null");
                        }
                    }

                    @Override
                    public void onRouteStart() {
                        Toast.makeText(conductor_map.this, "Calculating walking route...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRouteSuccess(ArrayList<RouteInfoModel> routeInfoList, int indexing) {
                        if (routeInfoList != null && !routeInfoList.isEmpty() &&
                                routeInfoList.get(indexing) != null &&
                                routeInfoList.get(indexing).getPoints() != null &&
                                !routeInfoList.get(indexing).getPoints().isEmpty()) {

                            List<LatLng> walkingPoints = routeInfoList.get(indexing).getPoints();
                            PolylineOptions walkingRouteOptions = new PolylineOptions()
                                    .addAll(walkingPoints)
                                    .width(dpToPx(6))
                                    .color(Color.argb(200, 76, 175, 80))
                                    .pattern(Arrays.asList(new Dot(), new Gap(dpToPx(7))));

                            if (walkingPolyline != null) walkingPolyline.remove();
                            walkingPolyline = map.addPolyline(walkingRouteOptions);

                            if (!walkingPoints.isEmpty()) {
                                int midIndex = walkingPoints.size() / 2;
                                LatLng midPoint = walkingPoints.get(midIndex);
                                BitmapDescriptor walkingIcon = getResizedBitmapDescriptorInPixels(
                                        conductor_map.this, R.drawable.user_icon, dpToPx(20), dpToPx(20));
                                if (walkingIcon != null) {
                                    map.addMarker(new MarkerOptions().position(midPoint).icon(walkingIcon).anchor(0.5f, 0.5f));
                                }
                            }
                            float totalDistance = calculateTotalRouteDistance(walkingPoints);
                            Toast.makeText(conductor_map.this,
                                    String.format("Walking distance: %.2f km", totalDistance / 1000f),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            drawSimpleDottedLine(origin, destination);
                        }
                    }

                    @Override
                    public void onRouteCancelled() {
                        drawSimpleDottedLine(origin, destination);
                    }
                })
                .waypoints(origin, destination)
                .build();
        walkingRouteDrawing.execute();
    }

    private float calculateTotalRouteDistance(List<LatLng> routePoints) {
        float totalDistance = 0;
        for (int i = 1; i < routePoints.size(); i++) {
            float[] results = new float[1];
            Location.distanceBetween(
                    routePoints.get(i-1).latitude, routePoints.get(i-1).longitude,
                    routePoints.get(i).latitude, routePoints.get(i).longitude,
                    results
            );
            totalDistance += results[0];
        }
        return totalDistance;
    }

    private BitmapDescriptor getResizedBitmapDescriptorInPixels(Context context, int resourceId, int widthPx, int heightPx) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int inSampleSize = 1;
            if (originalHeight > heightPx || originalWidth > widthPx) {
                final int halfHeight = originalHeight / 2;
                final int halfWidth = originalWidth / 2;
                while ((halfHeight / inSampleSize) >= heightPx && (halfWidth / inSampleSize) >= widthPx) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            Bitmap originalBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            if (originalBitmap == null) {
                Log.e(TAG, "Decoded bitmap is null. ResId: " + resourceId + ", SampleSize: " + inSampleSize);
                return null;
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, widthPx, heightPx, true);
            return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError resizing bitmap: " + resourceId, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error resizing bitmap: " + resourceId, e);
            return null;
        }
    }

    private void drawSimpleDottedLine(LatLng origin, LatLng destination) {
        if (map != null && origin != null && destination != null) {
            map.addPolyline(new PolylineOptions()
                    .add(origin, destination)
                    .width(dpToPx(3))
                    .color(Color.GRAY)
                    .pattern(Arrays.asList(new Dot(), new Gap(dpToPx(7)))));
        }
    }

    private void addCirclesAlongRoute(LatLng start, LatLng end) {
        if (map == null || start == null || end == null) return;
        float[] results = new float[1];
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        float distance = results[0];
        int circleSpacing = 150;
        int numCircles = (int) (distance / circleSpacing);
        if (numCircles <= 0) return;
        double latStep = (end.latitude - start.latitude) / (numCircles + 1);
        double lngStep = (end.longitude - start.longitude) / (numCircles + 1);
        BitmapDescriptor circleIcon = createRouteCircleMarker();
        if (circleIcon == null) return;
        for (int i = 1; i <= numCircles; i++) {
            LatLng circlePosition = new LatLng(start.latitude + (latStep * i), start.longitude + (lngStep * i));
            Marker m = map.addMarker(new MarkerOptions()
                    .position(circlePosition)
                    .icon(circleIcon)
                    .anchor(0.5f, 0.5f)
                    .zIndex(0));
            if (m != null) stopMarkers.add(m);
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
        canvas.drawText(text, padding / 2f, height / 2f + 10, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private BitmapDescriptor createCircleMarker(int color) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (20 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2 * density);
        borderPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (borderPaint.getStrokeWidth() / 2), borderPaint);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        map.setOnMapClickListener(latLng -> {
            if (destinationMarker != null) destinationMarker.remove();
            destinationLocation = latLng;
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .title("Selected Destination");
            destinationMarker = map.addMarker(markerOptions);

            if (userLocation != null) {
                getRoute(userLocation, destinationLocation);
            } else {
                Toast.makeText(this, "User location not available yet.", Toast.LENGTH_SHORT).show();
            }
        });

        fetchAndDisplayBuses();
        enableLocationAccess();
    }

    private void fetchAndDisplayBuses() {
        DatabaseReference busLocationReference = FirebaseDatabase.getInstance().getReference("bus location");
        busLocationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (map == null) return;
                for (Marker busMarkerInstance : busMarkers.values()) {
                    busMarkerInstance.remove();
                }
                busMarkers.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot routeSnapshot : snapshot.getChildren()) {
                        String routeNameKey = routeSnapshot.getKey();
                        for (DataSnapshot busSnapshot : routeSnapshot.getChildren()) {
                            String busNumber = busSnapshot.getKey();
                            Double latitude = busSnapshot.child("latitude").getValue(Double.class);
                            Double longitude = busSnapshot.child("longitude").getValue(Double.class);
                            if (latitude != null && longitude != null) {
                                LatLng busLocation = new LatLng(latitude, longitude);
                                BitmapDescriptor busIcon = getResizedBitmapDescriptor(
                                        conductor_map.this, R.drawable.buslocationpicbg, 36, 30);
                                if (busIcon != null) {
                                    Marker busM = map.addMarker(new MarkerOptions()
                                            .position(busLocation)
                                            .title("Bus (" + busNumber + ") on route: " + routeNameKey)
                                            .icon(busIcon)
                                            .anchor(0.5f,0.5f)
                                            .zIndex(3f));
                                    if (busM != null) busMarkers.put(busNumber, busM);
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "No bus locations found in Firebase.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase bus location error: " + error.getMessage());
            }
        });
    }

    private void enableLocationAccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        try {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in enableLocationAccess: " + e.getMessage());
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (!BusRouteData.isNearAnyStop(currentLatLng, 1000)) { // Check if user's actual location is near a stop within 1000m
                    currentLatLng = new LatLng(22.557569214996526, 88.39953748823015); // Hardcoded default
                    Toast.makeText(this, "Default location set as you're not near a known stop.", Toast.LENGTH_LONG).show();
                }
                userLocation = currentLatLng;
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            } else {
                Toast.makeText(this, "Could not get current location. Enable GPS or wait.", Toast.LENGTH_LONG).show();
                userLocation = new LatLng(22.5726, 88.3639);
                if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location: " + e.getMessage());
            Toast.makeText(this, "Failed to get current location.", Toast.LENGTH_SHORT).show();
            userLocation = new LatLng(22.5726, 88.3639);
            if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));
        });
    }

    private float calculateDistance(LatLng start, LatLng end) {
        if (start == null || end == null) return 0f;
        Location startLocation = new Location("start");
        startLocation.setLatitude(start.latitude);
        startLocation.setLongitude(start.longitude);
        Location endLocation = new Location("end");
        endLocation.setLatitude(end.latitude);
        endLocation.setLongitude(end.longitude);
        return startLocation.distanceTo(endLocation);
    }

    @Override
    public void onRouteFailure(ErrorHandling errorHandling) {
        String errorMessage = "Route calculation failed";
        String logMessage = "RouteListener (getRoute) - onRouteFailure: ";
        if (errorHandling != null) {
            String detailMessage = errorHandling.getMessage();
            if (detailMessage != null && !detailMessage.isEmpty()) {
                errorMessage += ": " + detailMessage;
                logMessage += errorHandling.toString() + " (" + detailMessage + ")";
            } else {
                errorMessage += ": " + errorHandling.toString();
                logMessage += errorHandling.toString();
            }
        } else {
            errorMessage += ": Unknown error (error object was null)";
            logMessage += "errorHandling object was null";
        }
        Log.e(TAG, logMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRouteStart() {
    }

    @Override
    public void onRouteSuccess(ArrayList<RouteInfoModel> arrayList, int indexing) {
        clearMap();

        if (arrayList != null && !arrayList.isEmpty() &&
                arrayList.get(indexing) != null &&
                arrayList.get(indexing).getPoints() != null &&
                !arrayList.get(indexing).getPoints().isEmpty()) {

            PolylineOptions polylineOptions = new PolylineOptions()
                    .color(Color.parseColor("#FF6F00"))
                    .width(dpToPx(5))
                    .startCap(new RoundCap())
                    .endCap(new RoundCap())
                    .addAll(arrayList.get(indexing).getPoints())
                    .zIndex(0.5f);
            this.routePolyline = map.addPolyline(polylineOptions);

            if (userLocation != null && this.destinationLocation != null) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(userLocation);
                builder.include(this.destinationLocation);
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), dpToPx(80)));
                } catch (IllegalStateException e) {
                    if (this.destinationLocation != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(this.destinationLocation, 14));
                }
            }
        } else {
            Toast.makeText(this, "General route calculation returned no points.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearMap() {
        if (this.routePolyline != null) {
            this.routePolyline.remove();
            this.routePolyline = null;
        }
        if (this.destinationMarker != null) {
            this.destinationMarker.remove();
            this.destinationMarker = null;
        }
    }

    private void getRoute(LatLng origin, LatLng destination) {
        if (map == null || origin == null || destination == null) {
            Toast.makeText(this, "Map or location not ready for general routing.", Toast.LENGTH_SHORT).show();
            return;
        }
        RouteDrawing routeDrawing = new RouteDrawing.Builder()
                .context(this)
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(origin, destination)
                .build();
        routeDrawing.execute();
    }

    @Override
    public void onRouteCancelled() {
        Toast.makeText(this, "General route calculation cancelled.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationAccess();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BitmapDescriptor getResizedBitmapDescriptor(Context context, int resourceId, int widthDp, int heightDp) {
        return getResizedBitmapDescriptorInPixels(context, resourceId, dpToPx(widthDp), dpToPx(heightDp));
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    private int dpToPx(float dpValue) {
        return Math.round(dpValue * getResources().getDisplayMetrics().density);
    }
}