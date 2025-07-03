package com.pritish.smartbuss;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationService";
    private static final int NOTIFICATION_ID = 1002;
    private static final String CHANNEL_ID = "LocationServiceChannel";

    // SharedPreferences keys
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_BUS_DIRECTION = "bus_direction";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;
    private SharedPreferences sharedPreferences;

    // Conductor details
    private String conductorPhone;
    private String busNumber;
    private String conductorRouteNumber;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationTrackingService created");

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        createNotificationChannel();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Log.d(TAG, "New location: " + location.getLatitude() + ", " + location.getLongitude());
                        sendLocationToFirebase(location);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationTrackingService started");

        if (intent != null) {
            conductorPhone = intent.getStringExtra("CONDUCTOR_PHONE");
            busNumber = intent.getStringExtra("BUS_NUMBER");
            conductorRouteNumber = intent.getStringExtra("ROUTE_NUMBER");

            Log.d(TAG, "Service started with - Bus: " + busNumber +
                    ", Route: " + conductorRouteNumber +
                    ", Phone: " + conductorPhone);
        }

        startForeground(NOTIFICATION_ID, createNotification("Tracking bus location..."));
        startLocationUpdates();
        return START_STICKY;
    }

    private void sendLocationToFirebase(Location location) {
        if (busNumber == null || conductorRouteNumber == null || conductorPhone == null) {
            Log.w(TAG, "Missing required data - bus: " + busNumber +
                    ", route: " + conductorRouteNumber +
                    ", phone: " + conductorPhone);
            return;
        }

        // Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String currentTime = timeFormat.format(new Date());

        // Create Firebase reference
        DatabaseReference busLocationRef = databaseReference
                .child("BusLiveLocation")
                .child(busNumber.replace(" ", "_")) // Sanitize bus number
                .child(conductorRouteNumber); // Route as child node

        // Prepare location data
        HashMap<String, Object> locationData = new HashMap<>();
        locationData.put("conductor_name", sharedPreferences.getString(KEY_USER_NAME, ""));
        locationData.put("conductor_phone", conductorPhone);
        locationData.put("date", currentDate);
        locationData.put("direction", sharedPreferences.getString(KEY_BUS_DIRECTION, "forward"));
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("speed_kmph", location.getSpeed() * 3.6f); // Convert m/s to km/h
        locationData.put("timestamp", currentTime);

        // Update Firebase
        busLocationRef.setValue(locationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String contentText) {
        Intent notificationIntent = new Intent(this, QRScannerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Bus Location")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.buslocationpicbg)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LocationTrackingService destroyed");
        stopLocationUpdates();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}