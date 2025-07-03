package com.pritish.smartbuss;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color; // Added for gradient
import android.graphics.LinearGradient; // Added for gradient
import android.graphics.Rect;
import android.graphics.Shader; // Added for gradient
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint; // Added for gradient
import android.util.Log;
import android.view.View;
import android.widget.TextView; // Added for gradient
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class AuthActivity extends AppCompatActivity implements LoginFragment.OnLoginSuccessListener, SignupFragment.OnSignupSuccessListener {

    private static final String TAG = "AuthActivity";
    private MaterialButtonToggleGroup toggleButtonGroup;
    private SharedPreferences sharedPreferences;
    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 102; // From original login

    private TextView appNameHeadingTextView; // Variable for the "BussPass" TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        final View rootView = findViewById(R.id.auth_fragment_container);
        final View topHeader = findViewById(R.id.auth_top_header_old_style);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            // If keyboard is open (keypadHeight > 15% of screen height)
            if (keypadHeight > screenHeight * 0.15) {
                // Hide the top header
                topHeader.setVisibility(View.GONE);
            } else {
                // Show the top header
                topHeader.setVisibility(View.VISIBLE);
            }
        });
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            Log.d(TAG, "User is already logged in. Attempting redirect via SplashScreen...");
            startSplashScreen();
            return; // Important to return to prevent loading fragments
        }

        toggleButtonGroup = findViewById(R.id.auth_toggle_button_group);
        appNameHeadingTextView = findViewById(R.id.auth_app_name_heading); // Initialize the "BussPass" TextView

        // Apply gradient to the app name heading ("BussPass")
        if (appNameHeadingTextView != null) {
            TextPaint paint = appNameHeadingTextView.getPaint();
            float width = paint.measureText(appNameHeadingTextView.getText().toString());
            if (width > 0) { // Ensure width is positive to avoid issues with gradient
                Shader shader = new LinearGradient(
                        0, 0, width, appNameHeadingTextView.getTextSize(),
                        new int[]{Color.parseColor("#00AEEF"), Color.parseColor("#1570EF")},
                        null, Shader.TileMode.CLAMP);
                appNameHeadingTextView.getPaint().setShader(shader);
                appNameHeadingTextView.invalidate();
            } else {
                Log.w(TAG, "Width of appNameHeadingTextView is 0, cannot apply gradient. Text: '" + appNameHeadingTextView.getText().toString() + "'");
                // You might want to set a default text color here if gradient fails due to zero width
                // For example: appNameHeadingTextView.setTextColor(Color.parseColor("#00AEEF"));
            }
        } else {
            Log.e(TAG, "appNameHeadingTextView (auth_app_name_heading) not found!");
        }

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(new LoginFragment());
            if (toggleButtonGroup != null) {
                toggleButtonGroup.check(R.id.button_login_tab);
            } else {
                Log.e(TAG, "toggleButtonGroup is null before setting default fragment check!");
            }
        }

        if (toggleButtonGroup != null) {
            toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    if (checkedId == R.id.button_login_tab) {
                        replaceFragment(new LoginFragment());
                    } else if (checkedId == R.id.button_signup_tab) {
                        replaceFragment(new SignupFragment());
                    }
                }
            });
        } else {
            Log.e(TAG, "toggleButtonGroup is null, cannot set listener!");
        }

        requestNotificationPermissionIfNeeded();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.auth_fragment_container, fragment);
        // Consider adding: .setReorderingAllowed(true)
        // Consider adding: .addToBackStack(null) if you want tab changes to be back-navigable
        fragmentTransaction.commit();
    }

    @Override
    public void onLoginSuccess() {
        Log.d(TAG, "Login successful, starting SplashScreen from AuthActivity.");
        startSplashScreen();
    }

    @Override
    public void onSignupSuccess() {
        Log.d(TAG, "Signup successful, switching to Login tab.");
        Toast.makeText(this, "User registered successfully! Please log in.", Toast.LENGTH_LONG).show();
        replaceFragment(new LoginFragment());
        if (toggleButtonGroup != null) {
            toggleButtonGroup.check(R.id.button_login_tab);
        }
    }

    private void startSplashScreen() {
        Intent intent = new Intent(AuthActivity.this, SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- Notification Permission Methods (from original login.java) ---
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission not granted. Requesting...");
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
                Log.d(TAG, "Notification permission granted by user after request.");
            } else {
                Log.w(TAG, "Notification permission denied by user after request.");
                Toast.makeText(this, "Notification permission denied. Some features might be limited.", Toast.LENGTH_LONG).show();
            }
        }
    }
    // --- End of Permission Methods ---
}