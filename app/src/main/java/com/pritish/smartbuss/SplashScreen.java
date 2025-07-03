package com.pritish.smartbuss;

import android.animation.Animator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.airbnb.lottie.LottieAnimationView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    private static final String TAG = "SplashScreen";
    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";
    private static final String KEY_USERNAME = "userName";

    private boolean hasNavigated = false; // To prevent double navigation
    private static final int MAX_WAIT_TIME = 5000; // fallback in case animation stuck

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView2);
        lottieAnimationView.playAnimation();

        // --- Animation Listener ---
        lottieAnimationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!hasNavigated) {
                    hasNavigated = true;
                    goToNextActivity();
                }
            }

            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });

        // --- Fallback timeout in case animation doesn't finish ---
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!hasNavigated) {
                Log.w(TAG, "Fallback triggered after delay. Animation may have failed or is stuck.");
                hasNavigated = true;
                goToNextActivity();
            }
        }, MAX_WAIT_TIME);
    }

    private void goToNextActivity() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        String userType = sharedPreferences.getString(KEY_USER_TYPE, null);
        String mainuserPhone = sharedPreferences.getString(KEY_LOGGED_IN_PHONE, null);
        String userName = sharedPreferences.getString(KEY_USERNAME, null);

        Log.d(TAG, "goToNextActivity - Retrieved from SharedPreferences: userType=" + userType + ", mainuserPhone=" + mainuserPhone + ", userName=" + userName);

        Intent intent;
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn && mainuserPhone != null) {
            if ("Traveler".equals(userType)) {
                intent = new Intent(SplashScreen.this, user_menu.class);
                Log.d(TAG, "Redirecting to user_menu");
            } else if ("Conductor".equals(userType)) {
                intent = new Intent(SplashScreen.this, conductor_menu.class);
                Log.d(TAG, "Redirecting to conductor_menu");
            } else {
                intent = new Intent(SplashScreen.this, AuthActivity.class);
                Log.w(TAG, "Invalid userType, redirecting to login.");
            }
        } else {
            intent = new Intent(SplashScreen.this, AuthActivity.class);
            Log.d(TAG, "User not logged in, redirecting to login.");
        }

        if (mainuserPhone != null) {
            intent.putExtra("mainuserPhone", mainuserPhone);
        }
        if (userName != null) {
            intent.putExtra("userName", userName);
        }

        startActivity(intent);
        finish();
    }
}
