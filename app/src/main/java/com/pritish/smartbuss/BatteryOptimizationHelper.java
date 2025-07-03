package com.pritish.smartbuss;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

public class BatteryOptimizationHelper {
    private static final String TAG = "BatteryOptimizationHelper";

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // For older versions, assume no battery optimization
    }

    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                showBatteryOptimizationDialog(context);
            }
        }
    }

    @SuppressLint("BatteryLife")
    private static void showBatteryOptimizationDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Battery Optimization")
                .setMessage("To maintain a stable connection to your ESP32 RFID device, please disable battery optimization for this app. This ensures the background service continues running even when the phone is locked.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + context.getPackageName()));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open battery optimization settings", e);
                        // Fallback to general battery optimization settings
                        try {
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            context.startActivity(intent);
                        } catch (Exception e2) {
                            Log.e(TAG, "Failed to open general battery optimization settings", e2);
                        }
                    }
                })
                .setNegativeButton("Later", null)
                .show();
    }

    public static void showAutoStartDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Auto-start Permission")
                .setMessage("For the best experience, please enable auto-start for this app in your device settings. This ensures the Bluetooth service starts automatically when needed.\n\nThe exact steps vary by device manufacturer (Xiaomi, Huawei, OnePlus, etc.).")
                .setPositiveButton("OK", null)
                .show();
    }
}