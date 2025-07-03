package com.pritish.smartbuss;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";

    // Views
    private Button startScanButton;
    private Button startRfidScanButton;
    private TextView scanResultTextView;
    private TextView scansTodayTextView;
    private TextView conductorInfoTextView;
    private EditText ticketIdEditText;
    private EditText ticketKeyEditText;
    private Button verifyManualButton;

    // State management
    private boolean isValidationInProgress = false;
    private boolean isServiceBound = false;

    // Data
    private DatabaseReference databaseReference;
    private String currentDate;
    private String mainuserPhone;
    private String conductorName = "";
    private String busNumber = "";
    private String conductorRouteNumber = "";

    // Service connection
    private BluetoothService bluetoothService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.BluetoothServiceBinder binder = (BluetoothService.BluetoothServiceBinder) service;
            bluetoothService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "BluetoothService bound.");
            updateBluetoothConnectionUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            bluetoothService = null;
            Log.d(TAG, "BluetoothService unbound.");
        }
    };

    private BroadcastReceiver serviceMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                // ... other cases ...
                case BluetoothService.ACTION_VALIDATION_SUCCESS:
                    isValidationInProgress = false;
                    hideLoadingState();
                    scanResultTextView.setText(intent.getStringExtra(BluetoothService.EXTRA_MESSAGE));
                    break;
                case BluetoothService.ACTION_VALIDATION_FAILURE:
                    isValidationInProgress = false;
                    hideLoadingState();
                    scanResultTextView.setText(intent.getStringExtra(BluetoothService.EXTRA_MESSAGE));
                    break;
                case BluetoothService.ACTION_SERVICE_STATUS_UPDATE:
                    if (intent.getBooleanExtra(BluetoothService.EXTRA_STATUS, false)) {
                        showLoadingState(intent.getStringExtra(BluetoothService.EXTRA_MESSAGE));
                    } else {
                        hideLoadingState();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initializeViews();
        initializeFirebase();
        getIntentExtras();
        setupClickListeners();
        setupServiceMessageReceiver();
        fetchConductorInformation();
    }

    private void initializeViews() {
        startScanButton = findViewById(R.id.startScanButton);
        startRfidScanButton = findViewById(R.id.startRfidScanButton);
        scanResultTextView = findViewById(R.id.scanResultTextView);
        scansTodayTextView = findViewById(R.id.scansTodayTextView);
        conductorInfoTextView = findViewById(R.id.conductorInfoTextView);
        ticketIdEditText = findViewById(R.id.ticketIdEditText);
        ticketKeyEditText = findViewById(R.id.ticketKeyEditText);
        verifyManualButton = findViewById(R.id.verifyManualButton);
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void getIntentExtras() {
        mainuserPhone = getIntent().getStringExtra("mainuserPhone");
        if (mainuserPhone == null || mainuserPhone.isEmpty()) {
            Toast.makeText(this, "Error: Conductor phone number missing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        startScanButton.setOnClickListener(v -> startQRScanner());

        startRfidScanButton.setOnClickListener(v -> {
            if (isServiceBound && bluetoothService != null) {
                if (bluetoothService.isConnected()) {
                    showLoadingState("Waiting for RFID scan...");
                } else {
                    Toast.makeText(this, "Attempting to reconnect...", Toast.LENGTH_SHORT).show();
                    bluetoothService.connectToESP32();
                }
            } else {
                Toast.makeText(this, "Service not ready. Please wait.", Toast.LENGTH_SHORT).show();
            }
        });



        verifyManualButton.setOnClickListener(v -> {
            hideKeyboard();
            String ticketId = ticketIdEditText.getText().toString().trim();

            if (ticketId.isEmpty()) {
                Toast.makeText(this, "Please enter the Ticket ID", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyTicketWithIdOnly(ticketId);
        });    }

    private void verifyTicketWithIdOnly(String ticketId) {
        showLoadingState("Verifying ticket...");

        // First check SmsTickets
        DatabaseReference smsTicketRef = databaseReference.child("SmsTickets").child(ticketId);
        smsTicketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Found in SmsTickets
                    Booking_for_others ticket = snapshot.getValue(Booking_for_others.class);
                    if (ticket != null && "valid".equals(ticket.getStatus())) {
                        handleSuccessfulVerification(ticketId, ticket.getPassengerPhone(),
                                ticket.getPersonCount(), "MANUAL", snapshot.getRef());
                    } else if (ticket != null) {
                        hideLoadingState();
                        scanResultTextView.setText("Ticket is already " + ticket.getStatus());
                    } else {
                        hideLoadingState();
                        scanResultTextView.setText("Invalid ticket data");
                    }
                } else {
                    // Not in SmsTickets, check Bookings
                    checkRegisteredUserTicketWithoutKey(ticketId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingState();
                scanResultTextView.setText("Database error. Please try again.");
            }
        });
    }

    private void checkRegisteredUserTicketWithoutKey(String ticketId) {
        DatabaseReference bookingsRef = databaseReference.child("Bookings");
        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    DataSnapshot ticketSnapshot = userSnapshot.child(ticketId);
                    if (ticketSnapshot.exists()) {
                        found = true;
                        Booking ticket = ticketSnapshot.getValue(Booking.class);
                        if (ticket != null) {
                            if (ticket.getScannedAt() == null || ticket.getScannedAt().isEmpty()) {
                                // Found valid ticket - verify it
                                handleSuccessfulVerification(ticketId, ticket.getPhoneNumber(),
                                        ticket.getPersonCount(), "MANUAL", ticketSnapshot.getRef());
                            } else {
                                hideLoadingState();
                                scanResultTextView.setText("Ticket already scanned at " + ticket.getScannedAt());
                            }
                        }
                        break;
                    }
                }

                if (!found) {
                    hideLoadingState();
                    scanResultTextView.setText("Invalid Ticket ID");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingState();
                scanResultTextView.setText("Database error. Please try again.");
            }
        });
    }



    private void searchTicketsByKey(String ticketKey) {
        showLoadingState("Searching for matching tickets...");

        DatabaseReference smsTicketsRef = databaseReference.child("SmsTickets");
        smsTicketsRef.orderByChild("ticketKey").equalTo(ticketKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        hideLoadingState();
                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            scanResultTextView.setText("No tickets found with this key");
                            return;
                        }

                        DataSnapshot mostRecentTicket = null;
                        for (DataSnapshot ticketSnapshot : snapshot.getChildren()) {
                            if (mostRecentTicket == null ||
                                    ticketSnapshot.child("date").getValue(String.class).compareTo(
                                            mostRecentTicket.child("date").getValue(String.class)) > 0) {
                                mostRecentTicket = ticketSnapshot;
                            }
                        }

                        if (mostRecentTicket != null) {
                            Booking_for_others ticket = mostRecentTicket.getValue(Booking_for_others.class);
                            if (ticket != null && "valid".equals(ticket.getStatus())) {
                                ticketIdEditText.setText(mostRecentTicket.getKey());
                                verifyTicketManually(mostRecentTicket.getKey(), ticketKey);
                            } else {
                                scanResultTextView.setText("Ticket is already " +
                                        (ticket != null ? ticket.getStatus() : "invalid"));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideLoadingState();
                        scanResultTextView.setText("Database error. Please try again.");
                        Log.e(TAG, "Ticket search error", error.toException());
                    }
                });
    }

    private void verifyTicketManually(String ticketId, String ticketKey) {
        showLoadingState("Verifying manual entry...");

        DatabaseReference ticketRef = databaseReference.child("SmsTickets").child(ticketId);

        ticketRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Booking_for_others ticket = snapshot.getValue(Booking_for_others.class);
                    if (ticket == null) {
                        hideLoadingState();
                        scanResultTextView.setText("Verification Failed:\nError reading ticket data.");
                        return;
                    }

                    if ("valid".equals(ticket.getStatus())) {
                        if (ticketKey.equals(ticket.getTicketKey())) {
                            handleSuccessfulVerification(ticketId, ticket.getPassengerPhone(),
                                    ticket.getPersonCount(), "MANUAL", snapshot.getRef());
                        } else {
                            hideLoadingState();
                            scanResultTextView.setText("Verification Failed:\nThe Ticket Key is incorrect.");
                        }
                    } else {
                        hideLoadingState();
                        scanResultTextView.setText("Verification Failed:\nTicket is already " + ticket.getStatus() + ".");
                    }
                } else {
                    checkRegisteredUserTicket(ticketId, ticketKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingState();
                scanResultTextView.setText("Database error. Please try again.");
                Log.e(TAG, "Manual verification database error", error.toException());
            }
        });
    }

    private void checkRegisteredUserTicket(String ticketId, String ticketKey) {
        DatabaseReference bookingsRef = databaseReference.child("Bookings");
        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean found = false;
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    DataSnapshot ticketSnapshot = userSnapshot.child(ticketId);
                    if (ticketSnapshot.exists()) {
                        found = true;
                        Booking ticket = ticketSnapshot.getValue(Booking.class);
                        if (ticket != null) {
                            if (ticket.getScannedAt() == null || ticket.getScannedAt().isEmpty()) {
                                databaseReference.child("SmartBus/TicketKeys").child(ticketId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot keySnapshot) {
                                                if (keySnapshot.exists() && ticketKey.equals(keySnapshot.getValue(String.class))) {
                                                    handleSuccessfulVerification(ticketId, ticket.getPhoneNumber(),
                                                            ticket.getPersonCount(), "MANUAL", ticketSnapshot.getRef());
                                                } else {
                                                    hideLoadingState();
                                                    scanResultTextView.setText("Verification Failed:\nThe Ticket Key is incorrect.");
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                hideLoadingState();
                                                scanResultTextView.setText("Database error. Please try again.");
                                            }
                                        });
                            } else {
                                hideLoadingState();
                                scanResultTextView.setText("Verification Failed:\nTicket already scanned at " + ticket.getScannedAt());
                            }
                        }
                        break;
                    }
                }

                if (!found) {
                    hideLoadingState();
                    scanResultTextView.setText("Verification Failed:\nInvalid Ticket ID.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoadingState();
                scanResultTextView.setText("Database error. Please try again.");
            }
        });
    }

    private void handleSuccessfulVerification(String ticketId, String passengerPhone,
                                              int personCount, String scanMethod, DatabaseReference ticketRef) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "used");
        updates.put("scannedAt", currentTime);
        updates.put("scannedByConductorPhone", mainuserPhone);
        updates.put("scannedByConductorName", conductorName);
        updates.put("scannedOnBus", busNumber);

        ticketRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    scanResultTextView.setText("Success! Ticket Verified for " + personCount + " person(s).");
                    ticketIdEditText.setText("");
                    incrementScanCount();
                    logConductorScan(ticketId, passengerPhone, personCount, scanMethod, currentTime);
                })
                .addOnFailureListener(e -> {
                    scanResultTextView.setText("Verification succeeded but failed to update status.");
                    Log.e(TAG, "Failed to update ticket status", e);
                });
    }



    private void logConductorScan(String ticketId, String passengerPhone, int personCount,
                                  String scanMethod, String scanTime) {
        DatabaseReference scanRef = databaseReference.child("Conductor")
                .child(mainuserPhone)
                .child("Scans")
                .child(currentDate)
                .child("verified_tickets")
                .child(ticketId);

        Map<String, Object> scanData = new HashMap<>();
        scanData.put("ticketId", ticketId);
        scanData.put("passengerPhone", passengerPhone);
        scanData.put("personCount", personCount);
        scanData.put("scanMethod", scanMethod);
        scanData.put("scanTime", scanTime);
        scanData.put("conductorName", conductorName);
        scanData.put("busNumber", busNumber);

        scanRef.setValue(scanData);
    }

    private void startQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan a bus ticket QR code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    private void fetchConductorInformation() {
        databaseReference.child("Conductor").child(mainuserPhone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            conductorName = snapshot.child("name").getValue(String.class);
                            // Use the same field names as in your database
                            busNumber = snapshot.child("bus").getValue(String.class);  // Changed from "busNumber"
                            conductorRouteNumber = snapshot.child("route").getValue(String.class);  // Changed from "routeNumber"
                            updateConductorInfoUI();
                            loadTodayScanCount();
                            startAndBindBluetoothService();
                        } else {
                            Toast.makeText(QRScannerActivity.this, "Conductor information not found", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(QRScannerActivity.this, "Failed to load conductor data", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }


    private void startAndBindBluetoothService() {
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        serviceIntent.putExtra("CONDUCTOR_PHONE", mainuserPhone);
        serviceIntent.putExtra("CONDUCTOR_NAME", conductorName);
        serviceIntent.putExtra("BUS_NUMBER", busNumber);
        serviceIntent.putExtra("ROUTE_NUMBER", conductorRouteNumber);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupServiceMessageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECTED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_DISCONNECTED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECTION_FAILED);
        filter.addAction(BluetoothService.ACTION_VALIDATION_SUCCESS);
        filter.addAction(BluetoothService.ACTION_VALIDATION_FAILURE);
        filter.addAction(BluetoothService.ACTION_SERVICE_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceMessageReceiver, filter);
    }

    private void updateConductorInfoUI() {
        String info = "Conductor: " + (conductorName != null ? conductorName : "N/A") +
                "\nBus: " + (busNumber != null ? busNumber : "N/A") +
                "\nRoute: " + (conductorRouteNumber != null ? conductorRouteNumber : "N/A");
        conductorInfoTextView.setText(info);
    }

    private void updateBluetoothConnectionUI() {
        if (isServiceBound && bluetoothService != null && bluetoothService.isConnected()) {
            startRfidScanButton.setEnabled(true);
            startRfidScanButton.setText("Connect to RFID (ESP32) - Connected");
        } else {
            startRfidScanButton.setEnabled(true);
            startRfidScanButton.setText("Connect to RFID (ESP32) - Disconnected");
        }
    }

    private void loadTodayScanCount() {
        databaseReference.child("Conductor").child(mainuserPhone).child("Scans").child(currentDate).child("total")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Integer count = snapshot.getValue(Integer.class);
                        scansTodayTextView.setText("Scans Today: " + (count != null ? count : 0));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load scan count", error.toException());
                    }
                });
    }

    private void incrementScanCount() {
        DatabaseReference scanCountRef = databaseReference.child("Conductor")
                .child(mainuserPhone)
                .child("Scans")
                .child(currentDate)
                .child("total");

        scanCountRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Integer currentCount = mutableData.getValue(Integer.class);
                mutableData.setValue(currentCount != null ? currentCount + 1 : 1);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Failed to increment scan count", error.toException());
                }
            }
        });
    }

    private void showLoadingState(String message) {
        scanResultTextView.setText(message);
        isValidationInProgress = true;
    }

    private void hideLoadingState() {
        isValidationInProgress = false;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            showLoadingState("Validating ticket...");
            if (isServiceBound && bluetoothService != null) {
                bluetoothService.validateQRContent(result.getContents());
            } else {
                hideLoadingState();
                scanResultTextView.setText("Service not ready. Please try again.");
                Toast.makeText(this, "Service not ready. Please wait.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceMessageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceMessageReceiver);
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        Log.d(TAG, "QRScannerActivity destroyed.");
    }
}