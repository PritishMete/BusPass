package com.pritish.smartbuss; // Your package name

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.widget.Toast; // For permission denial Toast

import androidx.annotation.NonNull; // For onRequestPermissionsResult
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

// Import permission classes if you have requestNotificationPermissionIfNeeded() here
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // For logging
    // If you have the notification permission request code here
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Log.d(TAG, "onCreate called. Intent: " + getIntent());
        if (getIntent() != null && getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Log.d(TAG, "onCreate Intent Extra: " + key + " = " + getIntent().getExtras().get(key));
            }
        }
        setContentView(R.layout.activity_main); // Ensure this has R.id.fragment_container

        // Call your permission request method if it's here
        // requestNotificationPermissionIfNeeded();

        if (savedInstanceState == null) {
            // Let handleIntent decide the initial fragment and backstack behavior
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called. Intent: " + intent);
        if (intent != null && intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                Log.d(TAG, "onNewIntent Intent Extra: " + key + " = " + intent.getExtras().get(key));
            }
        }
        setIntent(intent); // Update the activity's current intent
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent called.");
        if (intent != null && intent.getBooleanExtra("OPEN_NOTIFICATIONS_FRAGMENT", false)) {
            Log.d(TAG, "OPEN_NOTIFICATIONS_FRAGMENT extra is true.");

            // Check if user_home is already the base. If not, load it.
            // This logic ensures that if the app was already open on some other fragment,
            // we correctly build the stack: Home -> Notifications
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (!(currentFragment instanceof user_home)) {
                // Load home first without adding it to backstack (it's the base for this flow)
                // This will replace any existing fragment if it wasn't home.
                loadFragment(new user_home(), false, "user_home_tag_base");
            }

            // Then load user_notification and add this transaction to the back stack
            loadFragment(new user_notification(), true, "user_notification_tag");

            // Important: Remove the extra after processing to prevent this logic from
            // re-triggering on configuration changes if the Intent isn't new.
            intent.removeExtra("OPEN_NOTIFICATIONS_FRAGMENT");

        } else if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            // This case handles when the app starts normally (not from notification)
            // and the fragment container is empty.
            Log.d(TAG, "No specific extra found or no fragment loaded. Loading user_home fragment.");
            loadFragment(new user_home(), false, "user_home_tag_initial");
        } else {
            Log.d(TAG, "handleIntent: No action taken, fragment likely already exists or no relevant extra.");
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack, String tag) {
        Log.d(TAG, "loadFragment called for: " + tag + ", addToBackStack: " + addToBackStack);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag); // Use the tag for the back stack entry name
        }
        fragmentTransaction.commit();
    }

    // --- Include your requestNotificationPermissionIfNeeded() and onRequestPermissionsResult() if they are in this Activity ---
    // Example:
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d(TAG, "Notification permission already granted.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted by user.");
            } else {
                Log.w(TAG, "Notification permission denied by user.");
                Toast.makeText(this, "Notification permission denied. You won't receive notifications.", Toast.LENGTH_LONG).show();
            }
        }
    }
    // --- End of permission methods ---
}