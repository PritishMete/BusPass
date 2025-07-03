package com.pritish.smartbuss;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "BluetoothServiceChannel";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ESP32_DEVICE_NAME = "ESP32_GPS_RFID";

    // --- Broadcast Actions for UI updates ---
    public static final String ACTION_BLUETOOTH_CONNECTED = "com.pritish.smartbuss.BLUETOOTH_CONNECTED";
    public static final String ACTION_BLUETOOTH_DISCONNECTED = "com.pritish.smartbuss.BLUETOOTH_DISCONNECTED";
    public static final String ACTION_BLUETOOTH_CONNECTION_FAILED = "com.pritish.smartbuss.BLUETOOTH_CONNECTION_FAILED";
    public static final String ACTION_VALIDATION_SUCCESS = "com.pritish.smartbuss.VALIDATION_SUCCESS";
    public static final String ACTION_VALIDATION_FAILURE = "com.pritish.smartbuss.VALIDATION_FAILURE";
    public static final String ACTION_GPS_DATA_RECEIVED = "com.pritish.smartbuss.GPS_DATA_RECEIVED";
    public static final String ACTION_SERVICE_STATUS_UPDATE = "com.pritish.smartbuss.SERVICE_STATUS_UPDATE";

    // --- Broadcast Extras ---
    public static final String EXTRA_DATA = "extra_data"; // General message/status
    public static final String EXTRA_MESSAGE = "extra_message"; // Validation specific message
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_SPEED = "extra_speed";
    public static final String EXTRA_STATUS = "extra_status"; // Service running status

    // --- Direction constants ---
    private static final String DIRECTION_FORWARD = "forward";
    private static final String DIRECTION_RETURN = "return";

    // --- Service State & Bluetooth ---
    private final IBinder binder = new BluetoothServiceBinder();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice esp32Device;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream; // <--- ADDED THIS DECLARATION
    private ConnectedThread connectedThread; // Handles both RFID and GPS data
    private Handler reconnectHandler;
    private boolean isServiceRunning = false;
    private boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY = 10000; // 10 seconds

    // --- Conductor & Scan Data (for QR/RFID) & GPS Data (for location updates) ---
    private DatabaseReference databaseReference;
    private String conductorName;
    private String busNumber;
    private String conductorPhone;
    private String conductorRouteNumber;
    private String conductorDirection; // New: for GPS updates
    private String currentDate;
    private int scansToday = 0;

    // Inner class for GPS data parsing
    private static class GpsData {
        double latitude;
        double longitude;
        float speedKmph;
        String date; //YYYY-MM-DD
        String timestamp; // HH:MM:SS (IST)
        int satellites;
        float altitude;

        @Override
        public String toString() {
            return "GpsData{" +
                    "latitude=" + latitude +
                    "longitude=" + longitude +
                    ", speedKmph=" + speedKmph +
                    ", date='" + date + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    ", satellites=" + satellites +
                    ", altitude=" + altitude +
                    '}';
        }
    }


    public class BluetoothServiceBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BluetoothService created");
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        reconnectHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BluetoothService started");

        if (intent != null) {
            String phoneFromIntent = intent.getStringExtra("CONDUCTOR_PHONE");
            // If we get a new phone number or it's the first time, load the scan count
            if (phoneFromIntent != null && !phoneFromIntent.equals(this.conductorPhone)) {
                this.conductorPhone = phoneFromIntent;
                loadTodayScanCount(); // Load scan count when conductor info is available
            }
            conductorName = intent.getStringExtra("CONDUCTOR_NAME");
            busNumber = intent.getStringExtra("BUS_NUMBER");
            conductorRouteNumber = intent.getStringExtra("ROUTE_NUMBER");
            conductorDirection = intent.getStringExtra("DIRECTION"); // New: Get direction
            Log.d(TAG, "Service updated with Conductor: " + conductorName + " on Bus: " + busNumber + " Direction: " + conductorDirection);
        }

        if (!isServiceRunning) {
            isServiceRunning = true;
            // Initial notification, will be updated to reflect GPS status
            startForeground(NOTIFICATION_ID, createNotification("Initializing Bluetooth & GPS..."));
            connectToESP32();
        }
        sendServiceStatusUpdateBroadcast(); // Broadcast service running status
        return START_STICKY; // Service will be restarted if killed by Android
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BluetoothService destroyed");
        stopGpsReceiver(); // Ensure all Bluetooth resources are closed
        isServiceRunning = false;
        shouldReconnect = false; // Prevent further reconnect attempts

        if (reconnectHandler != null) reconnectHandler.removeCallbacksAndMessages(null);
        sendServiceStatusUpdateBroadcast(); // Broadcast service stopped status
    }

    // --- Public Methods for Activity Interaction ---
    public boolean isConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }

    // This method is used by QRScannerActivity to validate QR codes
    public void validateQRContent(String qrContent) {
        try {
            Log.d(TAG, "SERVICE: Validating QR Content: " + qrContent);
            String[] parts = qrContent.split("\\|");
            if (parts.length != 6) {
                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Invalid QR Code Format");
                // Pass null for fare and personCount as they are not extracted here
                recordScanDetails(qrContent, "QR_INVALID_FORMAT", null, null, null, null);
                return;
            }

            String transactionID = parts[0].trim();
            String fromLocation = parts[3].trim();
            String scannedKey = parts[4].trim();
            String passengerPhone = parts[5].trim();
            String toLocation = "Unknown"; // QR codes might not explicitly contain toLocation, handle as needed
            String fare = "0.00"; // QR codes might not explicitly contain fare, handle as needed
            String personCount = "1"; // QR codes might not explicitly contain personCount, handle as needed

            checkIfAlreadyScanned(transactionID, scannedKey, fromLocation, toLocation, personCount, fare, passengerPhone);

        } catch (Exception e) {
            Log.e(TAG, "Error validating QR ticket", e);
            sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Error processing QR data");
            // Pass null for fare and personCount as they are not extracted here
            recordScanDetails(qrContent, "QR_PROCESSING_ERROR", null, null, null, null);
        }
    }

    // --- Bluetooth Connection & GPS Data Handling Logic ---
    public void connectToESP32() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, "Bluetooth is not enabled");
            updateNotification("Bluetooth disabled. Enable to connect.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, "Bluetooth connect permission required.");
                updateNotification("Permission error. Check permissions.");
                return;
            }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        esp32Device = null;
        for (BluetoothDevice device : pairedDevices) {
            if (ESP32_DEVICE_NAME.equals(device.getName())) {
                esp32Device = device;
                break;
            }
        }

        if (esp32Device == null) {
            sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, "ESP32 device '" + ESP32_DEVICE_NAME + "' not paired. Please pair it.");
            updateNotification("ESP32 not found. Pair device.");
            scheduleReconnect();
            return;
        }

        // Only proceed if not already connected
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            Log.d(TAG, "Already connected to ESP32");
            updateNotification("Connected to ESP32. GPS & RFID Ready.");
            return;
        }

        new Thread(() -> {
            try {
                updateNotification("Connecting to ESP32...");
                // Close any existing socket first
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing previous socket", e);
                    }
                }

                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(MY_UUID);

                // Cancel discovery because it will slow down the connection
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }

                bluetoothSocket.connect(); // This is a blocking call

                if (bluetoothSocket.isConnected()) {
                    Log.d(TAG, "Successfully connected to ESP32");
                    reconnectAttempts = 0;
                    inputStream = bluetoothSocket.getInputStream(); // Get input stream for GPS
                    updateNotification("Connected to ESP32 - GPS & RFID Ready");
                    connectedThread = new ConnectedThread(bluetoothSocket, inputStream); // Pass inputStream
                    connectedThread.start();
                    sendBroadcast(ACTION_BLUETOOTH_CONNECTED, ESP32_DEVICE_NAME);
                } else {
                    throw new IOException("Socket not connected after connection attempt.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Connection failed: " + e.getMessage(), e);
                reconnectAttempts++;
                String failMessage = "Connection failed (" + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")";
                updateNotification(failMessage);
                sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, failMessage);
                try {
                    if (bluetoothSocket != null) bluetoothSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close socket after failure", closeException);
                }
                bluetoothSocket = null; // Clear socket reference
                inputStream = null; // Clear input stream
                scheduleReconnect();
            } catch (SecurityException se) {
                Log.e(TAG, "Bluetooth permission missing during connection: " + se.getMessage(), se);
                updateNotification("Permission Error: " + se.getMessage());
                sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, "Permission Error");
            }
        }).start();
    }

    private void scheduleReconnect() {
        if (!shouldReconnect || !isServiceRunning) {
            Log.d(TAG, "Stopping reconnect attempts. shouldReconnect=" + shouldReconnect + ", isServiceRunning=" + isServiceRunning);
            return;
        }
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            updateNotification("Connection failed - Max attempts reached.");
            sendBroadcast(ACTION_BLUETOOTH_CONNECTION_FAILED, "Max reconnect attempts reached.");
            Log.d(TAG, "Max reconnect attempts reached.");
            // Optionally, stop the service here if max attempts are reached and it cannot connect.
            // Or allow it to retry if user explicitly restarts service.
            stopSelf(); // Stop the service if it can't connect after many attempts
            return;
        }
        Log.d(TAG, "Scheduling reconnect attempt " + (reconnectAttempts + 1) + " in " + (RECONNECT_DELAY / 1000) + " seconds.");
        reconnectHandler.postDelayed(this::connectToESP32, RECONNECT_DELAY);
    }

    private void stopGpsReceiver() {
        Log.d(TAG, "stopGpsReceiver called.");
        shouldReconnect = false; // Prevent further reconnects
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing Bluetooth resources: " + e.getMessage());
        } finally {
            inputStream = null;
            bluetoothSocket = null;
            updateNotification("Service stopped.");
            Log.d(TAG, "Bluetooth service resources closed.");
        }
    }


    // --- Combined Data Handling Thread (for both RFID and GPS) ---
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final StringBuilder dataBuffer = new StringBuilder();

        public ConnectedThread(BluetoothSocket socket, InputStream inputStream) {
            mmSocket = socket;
            mmInStream = inputStream;
            Log.d(TAG, "ConnectedThread created with input stream.");
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            Log.d(TAG, "ConnectedThread started. Socket connected: " + mmSocket.isConnected());
            while (mmSocket.isConnected() && !Thread.currentThread().isInterrupted() && isServiceRunning) {
                try {
                    // Check if input stream is ready to read
                    if (mmInStream.available() > 0) {
                        bytes = mmInStream.read(buffer);
                        String receivedData = new String(buffer, 0, bytes);
                        processReceivedData(receivedData);
                    } else {
                        // Sleep briefly to avoid busy-waiting if no data is available
                        Thread.sleep(100);
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream disconnected or read error: " + e.getMessage(), e);
                    if (isServiceRunning) { // Only attempt reconnect if service is still intended to run
                        updateNotification("ESP32 disconnected - Reconnecting...");
                        sendBroadcast(ACTION_BLUETOOTH_DISCONNECTED, "Connection lost");
                        stopGpsReceiver(); // Clean up current resources
                        scheduleReconnect(); // Schedule a reconnect attempt
                    }
                    break; // Exit the thread loop
                } catch (InterruptedException e) {
                    Log.d(TAG, "ConnectedThread interrupted.", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit the thread loop
                }
            }
            Log.d(TAG, "ConnectedThread finishing.");
        }

        private void processReceivedData(String receivedChunk) {
            dataBuffer.append(receivedChunk);
            String bufferContent = dataBuffer.toString();
            int newLineIndex = bufferContent.indexOf('\n');

            while (newLineIndex != -1) {
                String completeMessage = bufferContent.substring(0, newLineIndex).trim();
                dataBuffer.delete(0, newLineIndex + 1);

                Log.d(TAG, "SERVICE: Raw data line: " + completeMessage);

                // Enhanced RFID detection
                if (completeMessage.startsWith("NFC UID:") || completeMessage.contains("RFID") || completeMessage.contains("UID:")) {
                    String rfidUid = completeMessage.replace("NFC UID:", "")
                            .replace("RFID", "")
                            .replace("UID:", "")
                            .trim();
                    if (!rfidUid.isEmpty()) {
                        Log.d(TAG, "RFID UID detected: " + rfidUid);
                        BluetoothService.this.validateRFIDTag(rfidUid);
                    }
                }

                else if (completeMessage.startsWith("------ GPS DATA")) {
                    // Look for the end delimiter for a complete GPS data block
                    int gpsEndIndex = bufferContent.indexOf("---------------------------------");
                    if (gpsEndIndex != -1) {
                        String gpsBlock = bufferContent.substring(0, gpsEndIndex + "---------------------------------".length());
                        Log.d(TAG, "Processing GPS block:\n" + gpsBlock);
                        GpsData parsedData = parseGpsData(gpsBlock);
                        if (parsedData != null) {
                            updateLocationInFirebase(conductorName, conductorPhone, parsedData.latitude, parsedData.longitude,
                                    parsedData.timestamp, parsedData.date, conductorRouteNumber, busNumber, conductorDirection, parsedData.speedKmph);
                            sendGpsDataBroadcast(parsedData); // Send to UI
                            updateNotification("GPS: " + String.format(Locale.US, "%.4f", parsedData.latitude) + ", " + String.format(Locale.US, "%.4f", parsedData.longitude));
                        } else {
                            Log.w(TAG, "Failed to parse GPS data block.");
                        }
                        dataBuffer.delete(0, gpsEndIndex + "---------------------------------".length()); // Remove processed block
                        newLineIndex = dataBuffer.toString().indexOf('\n'); // Re-evaluate for next line/block
                        continue; // Continue to next iteration, as a large block might have been processed
                    }
                }
                newLineIndex = dataBuffer.toString().indexOf('\n');
            }
        }

        public void cancel() {
            try {
                if (mmInStream != null) mmInStream.close();
                if (mmSocket != null) mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket or input stream", e);
            } finally {
                // Ensure the thread itself is stopped
                Thread.currentThread().interrupt();
            }
        }
    }

    private GpsData parseGpsData(String rawData) {
        if (rawData == null || rawData.trim().isEmpty()) {
            Log.w(TAG, "Raw GPS data is null or empty, cannot parse.");
            return null;
        }
        Log.d(TAG, "Attempting to parse data: \n" + rawData);
        GpsData data = new GpsData();
        // More robust regex, allows for variable whitespace around colon and value
        Pattern latPattern = Pattern.compile("Latitude:\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Pattern lonPattern = Pattern.compile("Longitude:\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Pattern satPattern = Pattern.compile("Satellites:\\s*(\\d+)");
        Pattern altPattern = Pattern.compile("Altitude \\(m\\):\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Pattern speedPattern = Pattern.compile("Speed \\(avg, km/h\\):\\s*([-+]?[0-9]*\\.?[0-9]+)");
        Pattern datePattern = Pattern.compile("Date:\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})"); // DD/MM/YYYY
        Pattern timePattern = Pattern.compile("Time \\(IST\\):\\s*(\\d{2}):(\\d{2}):(\\d{2})"); // HH:MM:SS

        Matcher matcher;
        boolean essentialDataFound = false;

        matcher = latPattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null) {
            data.latitude = Double.parseDouble(matcher.group(1));
            essentialDataFound = true;
        } else Log.w(TAG, "Latitude not found in data.");


        matcher = lonPattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null) {
            data.longitude = Double.parseDouble(matcher.group(1));
            essentialDataFound = essentialDataFound && true;
        } else Log.w(TAG, "Longitude not found in data.");

        matcher = datePattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null && matcher.group(2) != null && matcher.group(3) != null) {
            String day = String.format(Locale.US,"%02d", Integer.parseInt(matcher.group(1)));
            String month = String.format(Locale.US,"%02d", Integer.parseInt(matcher.group(2)));
            String year = matcher.group(3);
            data.date = year + "-" + month + "-" + day; //YYYY-MM-DD
            essentialDataFound = essentialDataFound && true;
        } else Log.w(TAG, "Date not found in data.");

        matcher = timePattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null && matcher.group(2) != null && matcher.group(3) != null) {
            data.timestamp = matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3);
            essentialDataFound = essentialDataFound && true;
        } else Log.w(TAG, "Time not found in data.");

        if (!essentialDataFound) {
            Log.e(TAG, "Essential GPS data (Lat, Lon, Date, Time) missing. Parse failed.");
            return null;
        }

        // Optional data
        matcher = speedPattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null) data.speedKmph = Float.parseFloat(matcher.group(1));
        else Log.w(TAG, "Speed not found, defaulting to 0 or previous.");


        matcher = satPattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null) data.satellites = Integer.parseInt(matcher.group(1));
        else Log.w(TAG, "Satellites not found, defaulting to 0 or previous.");


        matcher = altPattern.matcher(rawData);
        if (matcher.find() && matcher.group(1) != null) data.altitude = Float.parseFloat(matcher.group(1));
        else Log.w(TAG, "Altitude not found, defaulting to 0 or previous.");


        Log.d(TAG, "Parsed GPS Data: " + data.toString());
        return data;
    }

    private void updateLocationInFirebase(String userName, String mainuserPhone,
                                          double latitude, double longitude, String timeStr, String dateStr,
                                          String routeNumber, String busNumber, String direction, float speed) {
        if (routeNumber == null || routeNumber.isEmpty() || routeNumber.equals("N/A") ||
                busNumber == null || busNumber.isEmpty() || busNumber.equals("N/A")) {
            Log.w(TAG, "Route or Bus number is invalid for Firebase update.");
            return;
        }
        if (userName == null || userName.isEmpty() || mainuserPhone == null || mainuserPhone.isEmpty()) {
            Log.w(TAG, "User name or phone is invalid for Firebase update.");
            return;
        }


        DatabaseReference locationRef = FirebaseDatabase.getInstance()
                .getReference("Bus locations")
                .child(routeNumber)
                .child(busNumber);

        HashMap<String, Object> locationData = new HashMap<>();
        locationData.put("conductor name", userName);
        locationData.put("conductor phone", mainuserPhone);
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("direction", direction);
        locationData.put("timestamp", timeStr);
        locationData.put("date", dateStr);
        locationData.put("speed_kmph", speed);

        locationRef.setValue(locationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "ESP32 Location updated: " + routeNumber + "/" + busNumber))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase ESP32 location update failed for " + routeNumber + "/" + busNumber, e));
    }


    // --- TICKET VALIDATION LOGIC (UNCHANGED) ---
    private void validateRFIDTag(String rfidUid) {
        Log.d(TAG, "SERVICE: Attempting to validate RFID UID: " + rfidUid);
        showLoadingInActivity("Validating RFID tag...");

        String formattedUid = rfidUid.replace(" ", "");
        DatabaseReference nfcTagRef = databaseReference.child("NFCtags").child(formattedUid);

        nfcTagRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String phoneNumber = snapshot.child("phone").getValue(String.class);
                    final String transactionId = snapshot.child("transactionId").getValue(String.class);

                    if (phoneNumber != null && !phoneNumber.isEmpty() && transactionId != null && !transactionId.isEmpty()) {
                        DatabaseReference ticketKeyRef = databaseReference.child("SmartBus/TicketKeys").child(transactionId);
                        ticketKeyRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot keySnapshot) {
                                if (keySnapshot.exists()) {
                                    String savedKeyFromDb = keySnapshot.getValue(String.class);
                                    if (savedKeyFromDb != null && !savedKeyFromDb.isEmpty()) {
                                        // Get booking details with improved fare extraction
                                        databaseReference.child("Bookings").child(phoneNumber).child(transactionId)
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
                                                        if (bookingSnapshot.exists()) {
                                                            // Use the new fare extraction method
                                                            String fare = extractFareFromSnapshot(bookingSnapshot);

                                                            Integer personCountInt = bookingSnapshot.child("personCount").getValue(Integer.class);
                                                            String personCount = (personCountInt != null) ? String.valueOf(personCountInt) : "1";

                                                            String fromLocation = bookingSnapshot.child("startStop").getValue(String.class);
                                                            String toLocation = bookingSnapshot.child("destinationStop").getValue(String.class);

                                                            // Debug logging
                                                            Log.d(TAG, "RFID Validation - Transaction: " + transactionId +
                                                                    ", Fare: " + fare + ", PersonCount: " + personCount);

                                                            checkIfAlreadyScanned(transactionId, savedKeyFromDb,
                                                                    fromLocation != null ? fromLocation : "Unknown",
                                                                    toLocation != null ? toLocation : "Unknown",
                                                                    personCount, fare, phoneNumber);
                                                        } else {
                                                            hideLoadingInActivity();
                                                            sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Booking not found for this tag");
                                                            recordScanDetails(rfidUid, "RFID_BOOKING_NOT_FOUND", null, null, null, null);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                        hideLoadingInActivity();
                                                        sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Error fetching booking details");
                                                        recordScanDetails(rfidUid, "RFID_BOOKING_FETCH_ERROR", null, null, null, null);
                                                    }
                                                });
                                    } else {
                                        hideLoadingInActivity();
                                        sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Invalid Tag: No valid ticket key value.");
                                        recordScanDetails(rfidUid, "RFID_NO_TICKET_KEY_VALUE", null, null, null, null);
                                    }
                                } else {
                                    hideLoadingInActivity();
                                    sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Invalid Tag: Ticket key not found.");
                                    recordScanDetails(rfidUid, "RFID_TICKET_KEY_MISSING", null, null, null, null);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                hideLoadingInActivity();
                                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Firebase error fetching ticket key.");
                                recordScanDetails(rfidUid, "RFID_TICKET_KEY_FETCH_ERROR", null, null, null, null);
                            }
                        });
                    } else {
                        hideLoadingInActivity();
                        sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Tag has no active booking associated.");
                        recordScanDetails(rfidUid, "RFID_NO_TXN_OR_PHONE", null, null, null, null);
                    }
                } else {
                    hideLoadingInActivity();
                    sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Invalid NFC Tag: UID not registered.");
                    recordScanDetails(rfidUid, "RFID_INVALID", null, null, null, null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingInActivity();
                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Firebase error checking NFC tag.");
                recordScanDetails(rfidUid, "RFID_NFC_TAG_FETCH_ERROR", null, null, null, null);
            }
        });
    }
    public void testRfidCommunication() {
        if (isConnected() && connectedThread != null) {
            try {


                // Send a test command to the ESP32
                String testCommand = "TEST_RFID\n";
                bluetoothSocket.getOutputStream().write(testCommand.getBytes());
                Log.d(TAG, "Sent RFID test command");
            } catch (IOException e) {
                Log.e(TAG, "Error sending test command", e);
            }
        } else {
            Log.d(TAG, "Cannot send test command - not connected");
        }
    }
    private void showLoadingInActivity(String message) {
        Intent intent = new Intent(ACTION_SERVICE_STATUS_UPDATE);
        intent.putExtra(EXTRA_STATUS, true);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void hideLoadingInActivity() {
        Intent intent = new Intent(ACTION_SERVICE_STATUS_UPDATE);
        intent.putExtra(EXTRA_STATUS, false);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void checkIfAlreadyScanned(String transactionID, String scannedKey,
                                       String fromLocation, String toLocation,
                                       String personCount, String fare, // Keep fare and personCount for potential use
                                       String passengerPhone) {
        DatabaseReference bookingRef = databaseReference.child("Bookings")
                .child(passengerPhone)
                .child(transactionID);

        bookingRef.child("scannedAt").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String scanTime = snapshot.getValue(String.class);
                    String failMessage = "Ticket already scanned at " + scanTime;
                    hideLoadingInActivity();
                    sendValidationBroadcast(ACTION_VALIDATION_FAILURE, failMessage);
                    // Pass null for fare and personCount as they are not validated here
                    recordScanDetails(transactionID, "ALREADY_SCANNED", null, null, null, null);
                } else {
                    // For RFID, we don't need to validate the key since we got it from the database
                    if (fromLocation.equals("RFID")) {
                        markTicketAsScanned(transactionID, passengerPhone); // Will fetch details from booking
                    } else {
                        // For QR, we need to validate the key found in QR content
                        validateTicketKey(transactionID, scannedKey, passengerPhone);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingInActivity();
                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Error checking ticket status");
                // Pass null for fare and personCount as they are not validated here
                recordScanDetails(transactionID, "CHECK_SCAN_STATUS_ERROR", null, null, null, null);
            }
        });
    }

    private void validateTicketKey(String transactionID, String scannedKey, String passengerPhone) {
        databaseReference.child("SmartBus/TicketKeys").child(transactionID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String savedKey = task.getResult().getValue(String.class);
                if (scannedKey.equals(savedKey != null ? savedKey.trim() : "")) {
                    markTicketAsScanned(transactionID, passengerPhone); // Will fetch details from booking
                } else {
                    sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Invalid Ticket (Key Mismatch)");
                    // Pass null for fare and personCount as they are not validated here
                    recordScanDetails(transactionID, "KEY_MISMATCH", null, null, null, null);
                }
            } else {
                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Ticket Key Not Found in Database");
                // Pass null for fare and personCount as they are not validated here
                recordScanDetails(transactionID, "KEY_NOT_FOUND", null, null, null, null);
            }
        });
    }

    private void markTicketAsScanned(String transactionID, String passengerPhone) {
        DatabaseReference bookingRef = databaseReference.child("Bookings")
                .child(passengerPhone)
                .child(transactionID);

        bookingRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful() && task.getResult().exists()){
                DataSnapshot bookingSnapshot = task.getResult();

                String fromLocation = bookingSnapshot.child("startStop").getValue(String.class);
                String toLocation = bookingSnapshot.child("destinationStop").getValue(String.class);

                // Fix: Check multiple possible fare field names and handle different data types
                String fare = extractFareFromSnapshot(bookingSnapshot);

                Integer personCountInt = bookingSnapshot.child("personCount").getValue(Integer.class);
                String personCount = (personCountInt != null) ? String.valueOf(personCountInt) : "1";

                // Debug logging
                Log.d(TAG, "Extracted fare for transaction " + transactionID + ": " + fare);

                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                Map<String, Object> updates = new HashMap<>();
                updates.put("scannedAt", currentTime);
                updates.put("isScanned", true);
                updates.put("scannedBy", conductorName);
                updates.put("scannedByPhone", conductorPhone);
                updates.put("busNumber", busNumber);
                updates.put("routeNumber", conductorRouteNumber);

                bookingRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        String determinedScanType;
                        if ("RFID".equals(fromLocation)) {
                            determinedScanType = "RFID_VERIFIED";
                        } else {
                            determinedScanType = "QR_VERIFIED";
                        }

                        storeScannedTicketData(transactionID,
                                fromLocation != null ? fromLocation : "Unknown",
                                toLocation != null ? toLocation : "Unknown",
                                fare, // Pass the extracted fare directly
                                personCount,
                                currentTime,
                                passengerPhone);

                        recordScanDetails(transactionID, determinedScanType,
                                fromLocation, toLocation, fare, personCount);

                        hideLoadingInActivity();
                        sendValidationBroadcast(ACTION_VALIDATION_SUCCESS, "Ticket Validated!");
                    } else {
                        hideLoadingInActivity();
                        sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Failed to mark ticket as scanned.");
                        recordScanDetails(transactionID, "MARK_SCAN_FAILED", null, null, null, null);
                    }
                });
            } else {
                hideLoadingInActivity();
                sendValidationBroadcast(ACTION_VALIDATION_FAILURE, "Booking not found for transaction.");
                recordScanDetails(transactionID, "BOOKING_NOT_FOUND", null, null, null, null);
            }
        });
    }

    private String extractFareFromSnapshot(DataSnapshot bookingSnapshot) {
        // List of possible field names for fare/price
        String[] fareFields = {"price", "totalPrice", "fare", "amount"};

        for (String fieldName : fareFields) {
            if (bookingSnapshot.hasChild(fieldName)) {
                Object fareValue = bookingSnapshot.child(fieldName).getValue();

                if (fareValue != null) {
                    Log.d(TAG, "Found fare in field '" + fieldName + "': " + fareValue + " (type: " + fareValue.getClass().getSimpleName() + ")");

                    try {
                        if (fareValue instanceof Double) {
                            return String.format(Locale.getDefault(), "%.2f", (Double) fareValue);
                        } else if (fareValue instanceof Float) {
                            return String.format(Locale.getDefault(), "%.2f", (Float) fareValue);
                        } else if (fareValue instanceof Long) {
                            return String.format(Locale.getDefault(), "%.2f", ((Long) fareValue).doubleValue());
                        } else if (fareValue instanceof Integer) {
                            return String.format(Locale.getDefault(), "%.2f", ((Integer) fareValue).doubleValue());
                        } else if (fareValue instanceof String) {
                            String fareStr = (String) fareValue;
                            // Remove currency symbols and clean the string
                            String cleanFare = fareStr.replaceAll("[^\\d.]", "");
                            if (!cleanFare.isEmpty()) {
                                double fareDouble = Double.parseDouble(cleanFare);
                                return String.format(Locale.getDefault(), "%.2f", fareDouble);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing fare from field '" + fieldName + "': " + fareValue, e);
                    }
                }
            }
        }

        Log.w(TAG, "No valid fare found in any expected field. Available fields: " +
                bookingSnapshot.getChildren().toString());
        return "0.00";
    }


    private String formatFare(String fare) {
        if (fare == null || fare.trim().isEmpty()) {
            Log.w(TAG, "Fare is null or empty, defaulting to 0.00");
            return "0.00";
        }

        try {
            // If fare is already properly formatted, return as is
            if (fare.matches("\\d+\\.\\d{2}")) {
                double fareValue = Double.parseDouble(fare);
                if (fareValue > 0) {
                    return fare;
                }
            }

            // Remove currency symbols and clean the string, but preserve decimal points
            String cleanFare = fare.replaceAll("[^\\d.]", "");

            if (cleanFare.isEmpty()) {
                Log.w(TAG, "Fare became empty after cleaning: " + fare);
                return "0.00";
            }

            double fareValue = Double.parseDouble(cleanFare);

            // Log if fare seems unusually low or high
            if (fareValue > 0 && fareValue < 1) {
                Log.w(TAG, "Low fare value detected: " + fareValue);
            } else if (fareValue > 10000) {
                Log.w(TAG, "High fare value detected: " + fareValue);
            }

            return String.format(Locale.getDefault(), "%.2f", fareValue);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error formatting fare: " + fare, e);
            return "0.00";
        }
    }


    private void storeScannedTicketData(String transactionID, String from, String to, String fare,
                                        String personCount, String time, String passengerPhone) {
        // Store in both paths for consistency
        String routeKey = from + "-" + to;
        String busConductorKey = (busNumber != null ? busNumber.replace(" ", "_") : "N_A") + "_" + conductorPhone;

        // Path 1: SmartBuss structured data
        DatabaseReference smartBussRef = databaseReference.child("SmartBuss/ScannedTickets")
                .child(routeKey).child(busConductorKey).child(transactionID);

        // Path 2: Conductor scan details
        DatabaseReference conductorRef = databaseReference.child("Conductor")
                .child(conductorPhone)
                .child("Scans")
                .child(currentDate)
                .child("details")
                .push();

        // Common data
        Map<String, Object> ticketData = new HashMap<>();
        ticketData.put("transactionId", transactionID);
        ticketData.put("fromLocation", from);
        ticketData.put("toLocation", to);
        ticketData.put("fare", fare);
        ticketData.put("personCount", personCount);
        ticketData.put("scanTime", time);
        ticketData.put("passengerPhone", passengerPhone);
        ticketData.put("conductorName", conductorName);
        ticketData.put("conductorPhone", conductorPhone);
        ticketData.put("busNumber", busNumber);
        ticketData.put("routeNumber", conductorRouteNumber);
        ticketData.put("type", "TICKET_SCAN"); // This type is for SmartBuss/ScannedTickets

        // Write to both locations
        smartBussRef.setValue(ticketData);
        // Note: The conductorRef is now handled by recordScanDetails for verified scans only
    }

    // --- Scan Counting Logic ---
    private void loadTodayScanCount() {
        if (conductorPhone == null || conductorPhone.isEmpty()) return;
        DatabaseReference scansRef = databaseReference.child("Conductor").child(conductorPhone).child("Scans").child(currentDate).child("total");
        scansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scansToday = snapshot.exists() ? snapshot.getValue(Integer.class) : 0;
                Log.d(TAG, "Loaded today's scan count: " + scansToday);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load scan count", error.toException());
            }
        });
    }

    private void incrementScanCount() {
        if (conductorPhone == null || conductorPhone.isEmpty()) return;
        scansToday++;
        databaseReference.child("Conductor").child(conductorPhone).child("Scans").child(currentDate).child("total").setValue(scansToday);
        Log.d(TAG, "Incremented scan count to: " + scansToday);
    }

    private void recordScanDetails(String scannedValue, String scanType,
                                   String fromLocation, String toLocation,
                                   String fare, String personCount) {
        if (conductorPhone == null || conductorPhone.isEmpty()) return;

        // ONLY record and increment count for verified scans (RFID_VERIFIED or QR_VERIFIED)
        if (!"RFID_VERIFIED".equals(scanType) && !"QR_VERIFIED".equals(scanType)) {
            Log.d(TAG, "Not recording scan details for unverified type: " + scanType);
            return;
        }

        // Increment scan count only for verified scans
        incrementScanCount();

        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        DatabaseReference ref = databaseReference.child("Conductor")
                .child(conductorPhone)
                .child("Scans")
                .child(currentDate)
                .child("details")
                .push();

        Map<String, Object> scanData = new HashMap<>();
        scanData.put("timestamp", currentTime);
        scanData.put("value", scannedValue);
        scanData.put("type", scanType);
        scanData.put("busNumber", busNumber);
        scanData.put("routeNumber", conductorRouteNumber);
        scanData.put("startStop", fromLocation != null ? fromLocation : "Unknown");
        scanData.put("destinationStop", toLocation != null ? toLocation : "Unknown");
        scanData.put("price", fare != null ? fare : "0.00"); // Ensure fare is stored
        scanData.put("personCount", personCount != null ? personCount : "1");

        ref.setValue(scanData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Scan details recorded successfully for type: " + scanType))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record scan details", e));
    }




    // --- Notification and Broadcast Helpers ---
    private void sendBroadcast(String action, String data) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendGpsDataBroadcast(GpsData gpsData) {
        Intent intent = new Intent(ACTION_GPS_DATA_RECEIVED);
        intent.putExtra(EXTRA_LATITUDE, gpsData.latitude);
        intent.putExtra(EXTRA_LONGITUDE, gpsData.longitude);
        intent.putExtra(EXTRA_SPEED, gpsData.speedKmph);
        // You can add more GPS data extras if needed (e.g., date, timestamp, satellites, altitude)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "GPS data broadcasted: Lat=" + gpsData.latitude + ", Lon=" + gpsData.longitude);
    }

    private void sendValidationBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendServiceStatusUpdateBroadcast() {
        Intent intent = new Intent(ACTION_SERVICE_STATUS_UPDATE);
        intent.putExtra(EXTRA_STATUS, isServiceRunning);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Service status broadcasted: " + isServiceRunning);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smart Bus Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Provides continuous Bluetooth connection and GPS updates for Smart Bus.");
            channel.setSound(null, null); // No sound for low importance
            channel.enableVibration(false);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String contentText) {
        // Intent to open QRScannerActivity when notification is tapped
        Intent notificationIntent = new Intent(this, QRScannerActivity.class); // Or whatever main activity you want to open
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Bus GPS Tracker")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_busnumber_placeholder) // Use an appropriate icon
                .setContentIntent(pendingIntent)
                .setOngoing(true) // Makes the notification non-dismissible
                .setOnlyAlertOnce(true) // Avoids repeated sounds/vibrations for updates
                .build();
    }

    // This method is called frequently by the ConnectedThread to update the notification
    private void updateNotification(String contentText) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(contentText));
        }
    }
}