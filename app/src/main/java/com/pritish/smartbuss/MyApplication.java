package com.pritish.smartbuss; // Use your app's package name

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;

import java.util.List;

public class MyApplication extends Application {
    public static final String CHANNEL_ID = "SmartBussChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this); // Initialize Firebase if you haven't already

        BusRouteData.initializeDataFromFirebase(new BusRouteData.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                // Data is loaded, you can now safely access BusRouteData methods
                Log.d("App", "Bus data loaded successfully!");
                List<String> allRoutes = BusRouteData.getAllRoutes();
                // Update UI or perform actions that depend on loaded data
            }

            @Override
            public void onDataLoadFailed(String errorMessage) {
                // Handle the error, e.g., show a toast or a dialog
                Log.e("App", "Failed to load bus data: " + errorMessage);
            }
        });
        createNotificationChannel();
        try {
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
            // You can also enable disk persistence here if needed, before other DB calls
            // FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Log error or handle, though setLogLevel itself rarely throws if called early
            android.util.Log.e("MyApplication", "Error setting Firebase log level", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "SmartBuss Notifications"; // Channel name visible to user
            String description = "Channel for SmartBuss app notifications"; // Channel description
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}