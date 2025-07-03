package com.pritish.smartbuss;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Set;


import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TicketBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "TicketBroadcastReceiver";
    private static final String CHANNEL_ID = "ticket_notifications";
    private static final String CHANNEL_NAME = "SmartBus Ticket Notifications";
    private static final String NOTIFICATION_PREFS = "NotificationPrefs";
    private static final String NOTIFICATION_LIST_KEY = "notification_list";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract the message from the intent
        String message = intent.getStringExtra("message");
        String selectedStart = intent.getStringExtra("selectedStart");
        String selectedStop = intent.getStringExtra("selectedStop");
        if (message == null || message.isEmpty()) {
            message = "Notification message missing!";
        }

        // Get current date and time
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Log.d(TAG, "Received notification message: " + message);

        // Save the notification locally with date and time
        try {
            saveNotification(context, message, currentDateTime);
        } catch (Exception e) {
            Log.e(TAG, "Error saving notification: " + e.getMessage(), e);
        }

        // Create a notification channel for Android 8.0+ (only once)
        createNotificationChannel(context);

        // Set a unique notification ID for each notification (timestamp-based)
        int notificationId = (int) System.currentTimeMillis();

        // Create an intent to open user_menu activity when the notification is clicked
        Intent notificationIntent = new Intent(context, user_menu.class);
        notificationIntent.putExtra("message", message);
        notificationIntent.putExtra("selectedStart", selectedStart);
        notificationIntent.putExtra("selectedStop", selectedStop);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_ticket) // Replace with your app's icon
                .setContentTitle("SmartBus Ticket")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // For long messages
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Set the click action
                .setAutoCancel(true); // Dismiss notification on click

        // Display the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Log.d(TAG, "Displaying notification with ID: " + notificationId);
            notificationManager.notify(notificationId, builder.build());
        } else {
            Log.e(TAG, "NotificationManager is null!");
        }
    }

    /**
     * Save the notification in SharedPreferences as a JSON Array.
     */
    private void saveNotification(Context context, String message, String dateTime) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Get existing notifications
        String existingNotifications = sharedPreferences.getString(NOTIFICATION_LIST_KEY, "[]");
        try {
            JSONArray notificationArray = new JSONArray(existingNotifications);
            JSONObject newNotification = new JSONObject();
            newNotification.put("message", message);
            newNotification.put("dateTime", dateTime);

            // Add the new notification
            notificationArray.put(newNotification);

            // Save back to SharedPreferences
            editor.putString(NOTIFICATION_LIST_KEY, notificationArray.toString());
            editor.apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving notification: " + e.getMessage(), e);
        }
    }


    /**
     * Creates the notification channel (only for Android 8.0+).
     */
    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for SmartBus ticket bookings.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_NAME);
            }
        }
    }
}
