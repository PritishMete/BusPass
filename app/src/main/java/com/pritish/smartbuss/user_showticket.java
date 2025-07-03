package com.pritish.smartbuss;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class user_showticket extends AppCompatActivity {

    private RecyclerView bookingHistoryRecyclerView;
    private user_ShowTicketAdapter bookingHistoryAdapter;
    private ArrayList<BookingWrapper> bookingHistoryList;
    private DatabaseReference bookingsRef;
    private ProgressBar loadingProgressBar;
    private String mainUserPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_showticket);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Initialize views
        bookingHistoryRecyclerView = findViewById(R.id.bookingHistoryRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        // Initialize Firebase - Only bookings reference now
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        // Setup RecyclerView
        bookingHistoryList = new ArrayList<>();
        bookingHistoryAdapter = new user_ShowTicketAdapter(bookingHistoryList);
        bookingHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookingHistoryRecyclerView.setAdapter(bookingHistoryAdapter);

        // Load data
        mainUserPhone = getIntent().getStringExtra("mainuserPhone");
        if (mainUserPhone != null && !mainUserPhone.isEmpty()) {
            loadBookingHistory(mainUserPhone);
        } else {
            Toast.makeText(this, "User phone number not found.", Toast.LENGTH_LONG).show();
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    private void loadBookingHistory(String userPhone) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        bookingHistoryList.clear();

        // Change from 12 hours to 1 hour
        long oneHourAgoMillis = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);

        // Load only regular bookings for the user
        bookingsRef.child(userPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot transactionSnapshot : dataSnapshot.getChildren()) {
                    Booking booking = transactionSnapshot.getValue(Booking.class);
                    if (booking != null && booking.getTransactionId() != null) {
                        if (getBookingTimeMillis(booking.getDate(), booking.getTime()) >= oneHourAgoMillis) {
                            bookingHistoryList.add(new BookingWrapper(booking, false));
                        }
                    }
                }

                if (!bookingHistoryList.isEmpty()) {
                    sortBookingsByDateTime();
                }
                bookingHistoryAdapter.notifyDataSetChanged();
                loadingProgressBar.setVisibility(View.GONE);

                if (bookingHistoryList.isEmpty()) {
                    Toast.makeText(user_showticket.this,
                            "No recent bookings found (last 1 hour).",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(user_showticket.this,
                        "Failed to load your bookings: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                loadingProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private long getBookingTimeMillis(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null) return 0;
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date dateTime = dateTimeFormat.parse(dateStr + " " + timeStr);
            return dateTime != null ? dateTime.getTime() : 0;
        } catch (ParseException e) {
            try {
                SimpleDateFormat dateTimeFormatAlt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date dateTime = dateTimeFormatAlt.parse(dateStr + " " + timeStr);
                return dateTime != null ? dateTime.getTime() : 0;
            } catch (ParseException ex) {
                android.util.Log.e("user_showticket", "Error parsing date/time (alt): " + dateStr + " " + timeStr, ex);
                return 0;
            }
        }
    }

    private void sortBookingsByDateTime() {
        Collections.sort(bookingHistoryList, (b1, b2) -> {
            long time1 = getBookingTimeMillis(b1.getDate(), b1.getTime());
            long time2 = getBookingTimeMillis(b2.getDate(), b2.getTime());
            return Long.compare(time2, time1); // Sort newest first
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingHistoryList != null) {
            bookingHistoryList.clear();
        }
    }
}