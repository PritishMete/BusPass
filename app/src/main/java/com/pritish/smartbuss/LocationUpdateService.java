package com.pritish.smartbuss;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

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

public class LocationUpdateService extends Service {

    private static final String TAG = "LocationUpdateService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "location_updates";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private String routeNumber;
    private String busNumber;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configure location request
        locationRequest = LocationRequest.create()
                .setInterval(5000) // 5 seconds interval
                .setFastestInterval(2000) // Fastest update interval
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location Update: " + location.getLatitude() + ", " + location.getLongitude());
                    updateLocationInFirebase(location);
                }
            }
        };

        // Start location updates
        requestLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Retrieve route and bus values from the intent
        if (intent != null) {
            routeNumber = intent.getStringExtra("routeNumber");
            busNumber = intent.getStringExtra("busNumber");
            Log.d(TAG, "Route: " + routeNumber + ", Bus: " + busNumber);
        }
        return START_STICKY; // Ensures service restarts if killed
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateLocationInFirebase(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // Use the route and bus values passed from the fragment
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("bus location")
                .child(routeNumber) // Use the route value
                .child(busNumber); // Use the bus value

        HashMap<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", timestamp);

        databaseReference.setValue(locationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated in Firebase"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmartBus Location Tracking")
                .setContentText("Tracking bus location in the background...")
                .setSmallIcon(R.drawable.blueticket2) // Add a suitable icon in res/drawable
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Updates",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}