package com.pritish.smartbuss;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;

/**
 * Utility class for managing emergency notification features
 */
public class EmergencyNotificationUtils {

    private static final String PREFS_NAME = "emergency_prefs";
    private static final String KEY_LAST_MESSAGE_COUNT = "last_message_count";
    private static final String KEY_LAST_CHECK_TIME = "last_check_time";

    /**
     * Show notification dot with animation
     */
    public static void showNotificationDot(@NonNull View dotView, @NonNull View countView, int count) {
        if (dotView.getVisibility() != View.VISIBLE) {
            dotView.setVisibility(View.VISIBLE);

            // Scale animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(dotView, "scaleX", 0f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(dotView, "scaleY", 0f, 1.2f, 1f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(400);
            animatorSet.start();
        }

        // Update count if needed
        if (count > 0 && countView != null) {
            updateNotificationCount(countView, count);
        }
    }

    /**
     * Hide notification dot with animation
     */
    public static void hideNotificationDot(@NonNull View dotView, View countView) {
        if (dotView.getVisibility() == View.VISIBLE) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(dotView, "scaleX", 1f, 0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(dotView, "scaleY", 1f, 0f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(scaleX, scaleY);
            animatorSet.setDuration(200);
            animatorSet.start();

            // Hide after animation
            dotView.postDelayed(() -> dotView.setVisibility(View.GONE), 200);
        }

        if (countView != null) {
            countView.setVisibility(View.GONE);
        }
    }

    /**
     * Update notification count badge
     */
    public static void updateNotificationCount(@NonNull View countView, int count) {
        if (countView instanceof android.widget.TextView) {
            android.widget.TextView textView = (android.widget.TextView) countView;

            if (count > 0) {
                String displayCount = count > 99 ? "99+" : String.valueOf(count);
                textView.setText(displayCount);
                textView.setVisibility(View.VISIBLE);

                // Pulse animation for count update
                ObjectAnimator pulse = ObjectAnimator.ofFloat(textView, "scaleX", 1.0f, 1.3f, 1.0f);
                ObjectAnimator pulseY = ObjectAnimator.ofFloat(textView, "scaleY", 1.0f, 1.3f, 1.0f);

                AnimatorSet pulseSet = new AnimatorSet();
                pulseSet.playTogether(pulse, pulseY);
                pulseSet.setDuration(300);
                pulseSet.start();
            } else {
                textView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Start pulse animation for notification dot
     */
    public static void startPulseAnimation(@NonNull Context context, @NonNull View pulseView) {
        Animation pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse_animation);
        pulseView.startAnimation(pulseAnimation);
        pulseView.setVisibility(View.VISIBLE);
    }

    /**
     * Stop pulse animation
     */
    public static void stopPulseAnimation(@NonNull View pulseView) {
        pulseView.clearAnimation();
        pulseView.setVisibility(View.GONE);
    }

    /**
     * Check if there are new messages since last check
     */
    public static boolean hasNewMessages(@NonNull Context context, int currentCount) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastCount = prefs.getInt(KEY_LAST_MESSAGE_COUNT, 0);
        return currentCount > lastCount;
    }

    /**
     * Save current message count
     */
    public static void saveMessageCount(@NonNull Context context, int count) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_LAST_MESSAGE_COUNT, count)
                .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
                .apply();
    }

    /**
     * Get severity color based on message severity
     */
    public static int getSeverityColor(@NonNull String severity) {
        switch (severity.toLowerCase()) {
            case "high":
            case "critical":
                return 0xFFFF4444; // Red
            case "medium":
            case "warning":
                return 0xFFFF9800; // Orange
            case "low":
            case "info":
                return 0xFFFFEB3B; // Yellow
            default:
                return 0xFF9E9E9E; // Gray
        }
    }

    /**
     * Get severity text based on message severity
     */
    public static String getSeverityText(@NonNull String severity) {
        switch (severity.toLowerCase()) {
            case "high":
            case "critical":
                return "🔴 CRITICAL";
            case "medium":
            case "warning":
                return "🟠 WARNING";
            case "low":
            case "info":
                return "🟡 INFO";
            default:
                return "⚪ NOTICE";
        }
    }

    /**
     * Format emergency message for notification
     */
    public static String formatNotificationText(@NonNull String title, @NonNull String content) {
        String formattedContent = content.length() > 100 ?
                content.substring(0, 97) + "..." : content;
        return title + ": " + formattedContent;
    }

    /**
     * Check if emergency notification should be shown based on time
     */
    public static boolean shouldShowNotification(@NonNull Context context, long messageTimestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0);

        // Show notification if message is newer than last check time
        return messageTimestamp > lastCheckTime;
    }

    /**
     * Create notification dot programmatically if not in layout
     */
    public static View createNotificationDot(@NonNull Context context) {
        View dot = new View(context);
        dot.setBackgroundResource(R.drawable.notification_dot_background);

        // Set layout params
        android.widget.RelativeLayout.LayoutParams params =
                new android.widget.RelativeLayout.LayoutParams(
                        dpToPx(context, 12),
                        dpToPx(context, 12)
                );
        params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        params.setMargins(0, dpToPx(context, 8), dpToPx(context, 8), 0);

        dot.setLayoutParams(params);
        dot.setVisibility(View.GONE);

        return dot;
    }

    /**
     * Convert dp to pixels
     */
    private static int dpToPx(@NonNull Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}