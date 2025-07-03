package com.pritish.smartbuss;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap; // Added
import android.graphics.BitmapFactory; // Still used as a fallback or for other images
import android.graphics.Canvas; // Added
import android.graphics.drawable.BitmapDrawable; // Added
import android.graphics.drawable.Drawable; // Added
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat; // Added

// If you are using androidx.vectordrawable:vectordrawable for older APIs,
// you might need VectorDrawableCompat, but for most modern cases, framework's VectorDrawable is fine.
// import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class NotificationHelper {
    private static final String NOTIFICATION_PREFS = "NotificationPrefs";
    private static final String NOTIFICATION_LIST_KEY = "notification_list";
    private static final String TAG = "NotificationHelper";

    // --- Method to get Bitmap from Vector Drawable ---
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable == null) {
            Log.e(TAG, "Drawable not found for ID: " + drawableId + ". Ensure the drawable resource exists.");
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            Log.d(TAG, "Drawable is already a BitmapDrawable.");
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof android.graphics.drawable.VectorDrawable) {
            // Using android.graphics.drawable.VectorDrawable for API 21+
            Log.d(TAG, "Converting framework VectorDrawable to Bitmap. Intrinsic W: " + drawable.getIntrinsicWidth() + " H: " + drawable.getIntrinsicHeight());
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                Log.e(TAG, "VectorDrawable has intrinsic width/height <= 0. Cannot create Bitmap.");
                // You might want to provide a default size if intrinsics are problematic
                // For example: Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                // But it's better if the vector has proper dimensions.
                return null;
            }
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
        // Uncomment and use VectorDrawableCompat if you are specifically using it for backward compatibility
        // else if (drawable instanceof VectorDrawableCompat) {
        //     Log.d(TAG, "Converting VectorDrawableCompat to Bitmap.");
        //     Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
        //             drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        //     Canvas canvas = new Canvas(bitmap);
        //     drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        //     drawable.draw(canvas);
        //     return bitmap;
        // }
        else {
            Log.e(TAG, "Drawable is not a recognized type for direct Bitmap conversion (BitmapDrawable, VectorDrawable). Type: " + drawable.getClass().getSimpleName());
            // Fallback for other drawable types or if above fails.
            // This might not work well for all vectors if ContextCompat.getDrawable didn't already give a usable one.
            try {
                Log.d(TAG, "Attempting fallback Bitmap conversion for drawable type: " + drawable.getClass().getSimpleName());
                if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                    Log.e(TAG, "Drawable (fallback) has intrinsic width/height <= 0. Cannot create Bitmap.");
                    return null;
                }
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            } catch (Exception e) {
                Log.e(TAG, "Generic fallback to convert drawable to Bitmap failed.", e);
                return null;
            }
        }
    }


    public static List<user_notification.Notification> getNotifications(Context context) {
        List<user_notification.Notification> notifications = new ArrayList<>();
        notifications.addAll(getSavedNotifications(context));
        return notifications;
    }

    public static List<user_notification.Notification> getSavedNotifications(Context context) {
        List<user_notification.Notification> notifications = new ArrayList<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        String existingNotifications = sharedPreferences.getString(NOTIFICATION_LIST_KEY, "[]");
        try {
            JSONArray notificationArray = new JSONArray(existingNotifications);
            for (int i = 0; i < notificationArray.length(); i++) {
                JSONObject notification = notificationArray.getJSONObject(i);
                String message = notification.getString("message");
                String dateTime = notification.getString("dateTime");
                notifications.add(new user_notification.Notification(message, dateTime));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error getting saved notifications", e);
        }
        return notifications;
    }

    public static void saveNotification(Context context, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateTime = sdf.format(new Date());
        saveNotification(context, message, dateTime);
    }

    public static void saveNotification(Context context, String message, String dateTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        try {
            String existingNotifications = sharedPreferences.getString(NOTIFICATION_LIST_KEY, "[]");
            JSONArray notificationArray = new JSONArray(existingNotifications);

            JSONObject newNotification = new JSONObject();
            newNotification.put("message", message);
            newNotification.put("dateTime", dateTime);

            JSONArray newArray = new JSONArray();
            newArray.put(newNotification);
            for (int i = 0; i < notificationArray.length(); i++) {
                newArray.put(notificationArray.getJSONObject(i));
            }

            sharedPreferences.edit()
                    .putString(NOTIFICATION_LIST_KEY, newArray.toString())
                    .apply();

            showSystemNotification(context, message, dateTime);
        } catch (JSONException e) {
            Log.e(TAG, "Error saving notification", e);
        }
    }

    private static void showSystemNotification(Context context, String message, String dateTime) {
        Intent intent = new Intent(context, user_menu.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("OPEN_NOTIFICATIONS_FRAGMENT", true);

        PendingIntent pendingIntent;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        pendingIntent = PendingIntent.getActivity(context, new Random().nextInt() /* Unique Request code for each PendingIntent content */, intent, flags);


        // --- IMPORTANT: Replace with your actual drawable resource names ---
        // For the small icon in the status bar (must be monochrome silhouette)
        int smallIconResId = R.drawable.ic_simple_status_bar_bell; // EXAMPLE: Use a simple bell icon you created for this

        // For the large colorful icon (your bus pass vector)
        // This is the complex, colorful vector you provided earlier, e.g., the one named ic_notification_icon.xml
        // Ensure you use its CORRECT FILENAME here. For example, if its name is 'ic_bus_pass_logo.xml'
        // then use R.drawable.ic_bus_pass_logo
        int largeIconResId = R.drawable.ic_your_colorful_bus_pass_icon; // <<<<< REPLACE THIS WITH YOUR ACTUAL COLORFUL ICON'S FILENAME (e.g., R.drawable.your_bus_pass_vector_name)

        Bitmap largeIconBitmap = getBitmapFromVectorDrawable(context, largeIconResId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, MyApplication.CHANNEL_ID)
                .setSmallIcon(smallIconResId) // Use your SIMPLE MONOCHROME icon here
                .setContentTitle("SmartBuss Notification")
                .setContentText(message)
                .setSubText(dateTime)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap);
            Log.d(TAG, "Large icon has been set.");
        } else {
            Log.e(TAG, "Large icon bitmap is NULL. Large icon will NOT be shown. Check drawable name/file: " + context.getResources().getResourceEntryName(largeIconResId));
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Using a unique ID for each notification so they don't overwrite each other
            notificationManager.notify(new Random().nextInt(), builder.build());
        } else {
            Log.e(TAG, "NotificationManager is null.");
        }
    }

    public static void deleteSpecificNotification(Context context, String message, String dateTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        String existingNotificationsJson = sharedPreferences.getString(NOTIFICATION_LIST_KEY, "[]");
        try {
            JSONArray notificationArray = new JSONArray(existingNotificationsJson);
            JSONArray updatedArray = new JSONArray();
            boolean found = false;
            for (int i = 0; i < notificationArray.length(); i++) {
                JSONObject notificationObj = notificationArray.getJSONObject(i);
                if (!found && notificationObj.getString("message").equals(message) &&
                        notificationObj.getString("dateTime").equals(dateTime)) {
                    found = true;
                    Log.d(TAG, "Deleting notification via Helper: Msg='" + message + "', DateTime='" + dateTime + "'");
                } else {
                    updatedArray.put(notificationObj);
                }
            }
            sharedPreferences.edit().putString(NOTIFICATION_LIST_KEY, updatedArray.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error deleting specific notification from SharedPreferences", e);
        }
    }
}