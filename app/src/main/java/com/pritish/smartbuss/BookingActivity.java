package com.pritish.smartbuss;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import android.net.Uri;

public class BookingActivity extends DialogFragment {

    // --- Views ---
    private TextView ticketPriceTextView, selectedStartStopTextView, selectedDestinationStopTextView;
    private TextView personCountTextView, walletBalanceTextView;
    private Button confirmBookingButton;
    private ImageButton plusButton, minusButton;

    // --- Firebase & SharedPreferences ---
    private DatabaseReference bookingsRef;
    private DatabaseReference bookingForOthersRef;
    private DatabaseReference userRef;
    private DatabaseReference nfcTagsRef;
    private DatabaseReference databaseReference;
    private static final String WALLET_PREFS = "WalletPrefs";
    private static final String WALLET_BALANCE_KEY = "wallet_balance";

    // --- Data ---
    private String selectedStartStop;
    private String selectedDestinationStop;
    private String userPhone;
    private float baseTicketPrice;
    private float totalTicketPrice;
    private float currentBalance;
    private int personCount = 1;
    private String userNfcUid;

    public BookingActivity(String selectedStartStop, String selectedDestinationStop, float ticketPrice, String userPhone) {
        this.selectedStartStop = selectedStartStop;
        this.selectedDestinationStop = selectedDestinationStop;
        this.userPhone = userPhone;
        this.baseTicketPrice = ticketPrice;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.user_activity_booking, null);

        initializeViews(view);
        initializeFirebaseReferences();
        loadWalletBalance();
        loadUserNfcUid();
        setupClickListeners();

        selectedStartStopTextView.setText(selectedStartStop);
        selectedDestinationStopTextView.setText(selectedDestinationStop);
        updatePriceAndCountUI();

        builder.setView(view);
        return builder.create();
    }

    private void initializeViews(View view) {
        ticketPriceTextView = view.findViewById(R.id.ticketPriceTextView);
        selectedStartStopTextView = view.findViewById(R.id.selectedStartTextView);
        selectedDestinationStopTextView = view.findViewById(R.id.selectedStopTextView);
        personCountTextView = view.findViewById(R.id.icon_person_count);
        walletBalanceTextView = view.findViewById(R.id.walletBalanceTextView);
        confirmBookingButton = view.findViewById(R.id.confirmBookingButton);
        plusButton = view.findViewById(R.id.button_plus);
        minusButton = view.findViewById(R.id.button_minus);
    }

    private void setupClickListeners() {
        confirmBookingButton.setOnClickListener(v -> handleBookingConfirmation());

        plusButton.setOnClickListener(v -> {
            personCount++;
            updatePriceAndCountUI();
        });

        minusButton.setOnClickListener(v -> {
            if (personCount > 1) {
                personCount--;
                updatePriceAndCountUI();
            }
        });
    }

    private void updatePriceAndCountUI() {
        personCountTextView.setText(String.valueOf(personCount));
        totalTicketPrice = baseTicketPrice * personCount;
        ticketPriceTextView.setText(String.format(Locale.getDefault(), "₹%.2f", totalTicketPrice));
    }

    private void initializeFirebaseReferences() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        bookingsRef = databaseReference.child("Bookings");
        bookingForOthersRef = databaseReference.child("Booking_for_others");
        nfcTagsRef = databaseReference.child("NFCtags");
        if (userPhone != null && !userPhone.isEmpty()) {
            userRef = databaseReference.child("Traveler").child(userPhone);
        }
    }

    private void loadWalletBalance() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(WALLET_PREFS, Context.MODE_PRIVATE);
        currentBalance = sharedPreferences.getFloat(WALLET_BALANCE_KEY, 0.0f);
        updateWalletBalanceUI();

        if (userRef != null) {
            userRef.child("walletBalance").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Float firebaseBalance = snapshot.getValue(Float.class);
                        if (firebaseBalance != null && firebaseBalance != currentBalance) {
                            currentBalance = firebaseBalance;
                            updateWalletBalanceUI();
                            sharedPreferences.edit().putFloat(WALLET_BALANCE_KEY, currentBalance).apply();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("BookingActivity", "Failed to load wallet balance from Firebase", error.toException());
                }
            });
        }
    }

    private void loadUserNfcUid() {
        if (userRef != null) {
            userRef.child("uid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        userNfcUid = snapshot.getValue(String.class);
                        Log.d("BookingActivity", "User NFC UID: " + userNfcUid);
                    } else {
                        Log.d("BookingActivity", "NFC UID not found for user: " + userPhone);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("BookingActivity", "Failed to load NFC UID from Firebase", error.toException());
                }
            });
        }
    }

    private void updateWalletBalanceUI() {
        walletBalanceTextView.setText(String.format(Locale.getDefault(), "₹%.2f", currentBalance));
    }

    private void handleBookingConfirmation() {
        if (currentBalance < totalTicketPrice) {
            Toast.makeText(getContext(), "Insufficient balance in your wallet.", Toast.LENGTH_SHORT).show();
            return;
        }

        confirmBookingButton.setEnabled(false);

        new AlertDialog.Builder(requireActivity())
                .setTitle("Confirm Booking")
                .setMessage(String.format(Locale.getDefault(), "Confirm booking for %d person(s) for a total of ₹%.2f?", personCount, totalTicketPrice))
                .setPositiveButton("Yes", (dialog, which) -> processBooking())
                .setNegativeButton("No", (dialog, which) -> confirmBookingButton.setEnabled(true))
                .setOnCancelListener(dialog -> confirmBookingButton.setEnabled(true))
                .show();
    }

    private void processBooking() {
        float newBalance = currentBalance - totalTicketPrice;

        if (userRef != null) {
            userRef.child("walletBalance").setValue(newBalance)
                    .addOnSuccessListener(aVoid -> {
                        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(WALLET_PREFS, Context.MODE_PRIVATE);
                        sharedPreferences.edit().putFloat(WALLET_BALANCE_KEY, newBalance).apply();
                        createBooking();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update wallet balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("BookingActivity", "Wallet balance update failed", e);
                        confirmBookingButton.setEnabled(true);
                    });
        } else {
            Toast.makeText(getContext(), "User reference not found. Cannot proceed.", Toast.LENGTH_SHORT).show();
            confirmBookingButton.setEnabled(true);
        }
    }

    private void createBooking() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date now = new Date();
        String currentDate = dateFormat.format(now);
        String currentTime = timeFormat.format(now);
        String transactionId = UUID.randomUUID().toString().substring(0, 10);

        Booking booking = new Booking(
                transactionId,
                selectedStartStop,
                selectedDestinationStop,
                userPhone,
                totalTicketPrice,
                currentDate,
                currentTime,
                personCount
        );

        if (userPhone != null) {
            bookingsRef.child(userPhone).child(transactionId).setValue(booking)
                    .addOnSuccessListener(aVoid -> {
                        if (userNfcUid != null && !userNfcUid.isEmpty()) {
                            nfcTagsRef.child(userNfcUid).child("transactionId").setValue(transactionId)
                                    .addOnSuccessListener(aVoidNfc -> Log.d("BookingActivity", "Transaction ID added to NFC tag."))
                                    .addOnFailureListener(eNfc -> Log.e("BookingActivity", "Failed to add transaction ID to NFC tag.", eNfc));
                        }

                        databaseReference.child("SmartBus").child("TicketKeys").child(transactionId).setValue(transactionId)
                                .addOnSuccessListener(aVoidKey -> Log.d("BookingActivity", "Ticket key stored in SmartBus/TicketKeys."))
                                .addOnFailureListener(eKey -> Log.e("BookingActivity", "Failed to store ticket key.", eKey));

                        Toast.makeText(getContext(), "Booking Confirmed!", Toast.LENGTH_SHORT).show();
                        updateWalletBalanceUI();

                        String ticketKey = userPhone.substring(userPhone.length() - 4);

                        // Use Handler to delay SMS launch and ensure proper activity context
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            launchSmsIntent(transactionId, ticketKey);
                        }, 500); // 500ms delay

                        // Navigate to user menu after a short delay
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            dismiss();
                            Intent intent = new Intent(getActivity(), user_menu.class);
                            intent.putExtra("showTicket", true);
                            intent.putExtra("transactionId", transactionId);
                            intent.putExtra("startStop", selectedStartStop);
                            intent.putExtra("destinationStop", selectedDestinationStop);
                            intent.putExtra("ticketPrice", totalTicketPrice);
                            intent.putExtra("personCount", personCount);
                            startActivity(intent);
                        }, 1000); // 1 second delay
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to save booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("BookingActivity", "Booking creation failed", e);
                        confirmBookingButton.setEnabled(true);
                    });
        }
    }

    private void launchSmsIntent(String transactionId, String ticketKey) {
        try {
            String route = selectedStartStop + " -> " + selectedDestinationStop;
            String price = String.format(Locale.getDefault(), "₹%.2f", totalTicketPrice);

            String message = "Your bus ticket is confirmed.\n\n" +
                    "Route: " + route + "\n" +
                    "Price: " + price + "\n" +
                    "Ticket ID: " + transactionId + "\n\n" +
                    "IMPORTANT: To use this ticket, show the Ticket ID to the conductor and tell them the last 4 digits of this mobile number (" + ticketKey + ").";

            Log.d("BookingActivity", "Preparing SMS for: " + userPhone);
            Log.d("BookingActivity", "SMS Message: " + message);

            // Try multiple SMS intent approaches
            Intent smsIntent = null;

            // First try - Direct SMS intent
            try {
                smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + userPhone));
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (getActivity() != null && smsIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    Log.d("BookingActivity", "Launching SMS app with ACTION_SENDTO");
                    startActivity(smsIntent);
                    return;
                }
            } catch (Exception e) {
                Log.e("BookingActivity", "First SMS intent failed", e);
            }

            // Second try - Generic VIEW intent
            try {
                smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setData(Uri.parse("sms:" + userPhone));
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (getActivity() != null && smsIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    Log.d("BookingActivity", "Launching SMS app with ACTION_VIEW");
                    startActivity(smsIntent);
                    return;
                }
            } catch (Exception e) {
                Log.e("BookingActivity", "Second SMS intent failed", e);
            }

            // Third try - SEND intent
            try {
                smsIntent = new Intent(Intent.ACTION_SEND);
                smsIntent.setType("text/plain");
                smsIntent.putExtra("address", userPhone);
                smsIntent.putExtra(Intent.EXTRA_TEXT, message);
                smsIntent.putExtra("sms_body", message);
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (getActivity() != null) {
                    Log.d("BookingActivity", "Launching SMS app with ACTION_SEND");
                    startActivity(Intent.createChooser(smsIntent, "Send SMS"));
                    return;
                }
            } catch (Exception e) {
                Log.e("BookingActivity", "Third SMS intent failed", e);
            }

            // If all attempts fail
            Log.e("BookingActivity", "All SMS intent attempts failed");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Could not open SMS app. Ticket details:\n" +
                        "ID: " + transactionId + "\nKey: " + ticketKey, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e("BookingActivity", "Error in launchSmsIntent", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening SMS app. Please send ticket details manually.", Toast.LENGTH_LONG).show();
            }
        }
    }
}