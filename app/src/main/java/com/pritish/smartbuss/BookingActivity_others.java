package com.pritish.smartbuss;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Locale;
import java.util.UUID;
import java.util.Date;

public class BookingActivity_others extends DialogFragment {

    private TextView ticketPriceTextView, selectedStartStopTextView, selectedDestinationStopTextView;
    private TextView personCountTextView, walletBalanceTextView, passengerPhoneTextView;
    private Button confirmBookingButton;
    private ImageButton plusButton, minusButton;

    private DatabaseReference bookingsRef, bookingForOthersRef, userRef, nfcTagsRef, databaseReference, smsTicketsRef;
    private static final String WALLET_PREFS = "WalletPrefs";
    private static final String WALLET_BALANCE_KEY = "wallet_balance";

    private String selectedStartStop, selectedDestinationStop, passengerPhone, mainuserPhone;
    private float baseTicketPrice, totalTicketPrice, currentBalance;
    private int personCount = 1;
    private String passengerNfcUid;
    private boolean passengerIsRegistered = false;

    public BookingActivity_others() {
    }

    public static BookingActivity_others newInstance(String selectedStartStop, String selectedDestinationStop,
                                                     String passengerPhone, String mainuserPhone, double ticketPrice) {
        BookingActivity_others fragment = new BookingActivity_others();
        Bundle args = new Bundle();
        args.putString("selectedStartStop", selectedStartStop);
        args.putString("selectedDestinationStop", selectedDestinationStop);
        args.putString("passengerPhone", passengerPhone);
        args.putString("mainuserPhone", mainuserPhone);
        args.putFloat("ticketPrice", (float) ticketPrice);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedStartStop = getArguments().getString("selectedStartStop");
            selectedDestinationStop = getArguments().getString("selectedDestinationStop");
            passengerPhone = getArguments().getString("passengerPhone");
            mainuserPhone = getArguments().getString("mainuserPhone");
            baseTicketPrice = getArguments().getFloat("ticketPrice", 0.0f);
        }
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
        View view = inflater.inflate(R.layout.user_activity_booking_others, null);

        initializeViews(view);
        initializeFirebaseReferences();
        checkIfPassengerIsRegistered();
        loadWalletBalance();
        setupClickListeners();

        selectedStartStopTextView.setText(selectedStartStop);
        selectedDestinationStopTextView.setText(selectedDestinationStop);
        passengerPhoneTextView.setText(passengerPhone);
        updatePriceAndCountUI();

        builder.setView(view);
        return builder.create();
    }

    private void initializeViews(View view) {
        ticketPriceTextView = view.findViewById(R.id.ticketPriceTextView);
        selectedStartStopTextView = view.findViewById(R.id.selectedStartTextView);
        selectedDestinationStopTextView = view.findViewById(R.id.selectedStopTextView);
        passengerPhoneTextView = view.findViewById(R.id.phone_number_text_view);
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
        smsTicketsRef = databaseReference.child("SmsTickets");
        if (mainuserPhone != null && !mainuserPhone.isEmpty()) {
            userRef = databaseReference.child("Traveler").child(mainuserPhone);
        }
    }

    private void checkIfPassengerIsRegistered() {
        if (passengerPhone == null || passengerPhone.isEmpty()) return;

        databaseReference.child("Traveler").child(passengerPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d("BookingActivity_others", "Passenger " + passengerPhone + " is a registered user.");
                    passengerIsRegistered = true;
                    loadPassengerNfcUid();
                } else {
                    Log.d("BookingActivity_others", "Passenger " + passengerPhone + " is not a registered user.");
                    passengerIsRegistered = false;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BookingActivity_others", "Failed to check passenger registration status.", error.toException());
                passengerIsRegistered = false;
            }
        });
    }

    private void loadPassengerNfcUid() {
        if (passengerPhone != null && !passengerPhone.isEmpty()) {
            databaseReference.child("Traveler").child(passengerPhone).child("uid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        passengerNfcUid = snapshot.getValue(String.class);
                        Log.d("BookingActivity_others", "Registered Passenger NFC UID: " + passengerNfcUid);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("BookingActivity_others", "Failed to load passenger NFC UID.", error.toException());
                }
            });
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
                    Log.e("BookingActivity_others", "Failed to load wallet balance from Firebase", error.toException());
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
                .setMessage(String.format(Locale.getDefault(), "Confirm booking for %s (%d person(s)) for a total of ₹%.2f?", passengerPhone, personCount, totalTicketPrice))
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
                        Log.e("BookingActivity_others", "Wallet balance update failed", e);
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

        Booking_for_others bookerRecord = new Booking_for_others(
                transactionId, selectedStartStop, selectedDestinationStop,
                passengerPhone, totalTicketPrice, currentDate, currentTime, personCount, mainuserPhone
        );

        Booking passengerBooking = new Booking(
                transactionId, selectedStartStop, selectedDestinationStop, passengerPhone,
                totalTicketPrice, currentDate, currentTime, personCount
        );

        bookingForOthersRef.child(mainuserPhone).child(transactionId).setValue(bookerRecord);

        if (passengerIsRegistered) {
            bookingsRef.child(passengerPhone).child(transactionId).setValue(passengerBooking)
                    .addOnSuccessListener(aVoidPassenger -> {
                        if (passengerNfcUid != null && !passengerNfcUid.isEmpty()) {
                            nfcTagsRef.child(passengerNfcUid).child("transactionId").setValue(transactionId);
                        }
                        databaseReference.child("SmartBus").child("TicketKeys").child(transactionId).setValue(transactionId);

                        Toast.makeText(getContext(), "Booking Confirmed! Preparing SMS...", Toast.LENGTH_LONG).show();
                        updateWalletBalanceUI();
                        dismiss();
                        broadcastBookingConfirmation();

                        String ticketKey = passengerPhone.substring(passengerPhone.length() - 4);
                        launchSmsIntent(transactionId, ticketKey);

                    }).addOnFailureListener(ePassenger -> {
                        Toast.makeText(getContext(), "Failed to save passenger booking: " + ePassenger.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("BookingActivity_others", "Passenger booking creation failed", ePassenger);
                        confirmBookingButton.setEnabled(true);
                    });
        } else {
            bookingsRef.child(passengerPhone).child(transactionId).setValue(passengerBooking)
                    .addOnSuccessListener(aVoid -> {
                        String ticketKey = passengerPhone.substring(passengerPhone.length() - 4);
                        bookerRecord.setTicketKey(ticketKey);

                        smsTicketsRef.child(transactionId).setValue(bookerRecord)
                                .addOnSuccessListener(aVoidSms -> {
                                    Toast.makeText(getContext(), "Booking Confirmed! Preparing SMS...", Toast.LENGTH_LONG).show();
                                    updateWalletBalanceUI();
                                    dismiss();
                                    broadcastBookingConfirmation();
                                    launchSmsIntent(transactionId, ticketKey);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("BookingActivity_others", "SMS Booking creation failed", e);
                                    confirmBookingButton.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("BookingActivity_others", "Booking creation failed", e);
                        confirmBookingButton.setEnabled(true);
                    });
        }
    }

    private void launchSmsIntent(String transactionId, String ticketKey) {
        String route = selectedStartStop + " -> " + selectedDestinationStop;
        String price = String.format(Locale.getDefault(), "₹%.2f", totalTicketPrice);

        String message = "Your bus ticket is confirmed.\n\n" +
                "Route: " + route + "\n" +
                "Price: " + price + "\n" +
                "Ticket ID: " + transactionId + "\n\n" +
                "IMPORTANT: To use this ticket, show the Ticket ID to the conductor and tell them the last 4 digits of this mobile number (" + ticketKey + ").";

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + passengerPhone));
        smsIntent.putExtra("sms_body", message);

        if (getActivity() != null && smsIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(smsIntent);
        } else {
            Toast.makeText(getContext(), "Could not open SMS app. Please send the ticket details manually.", Toast.LENGTH_LONG).show();
        }
    }

    private void broadcastBookingConfirmation() {
        Intent intent = new Intent("com.pritish.smartbuss.BOOKING_CONFIRMED");
        intent.putExtra("passengerPhone", passengerPhone);
        intent.putExtra("bookedBy", mainuserPhone);
        if (getActivity() != null) {
            getActivity().sendBroadcast(intent);
        }
    }
}