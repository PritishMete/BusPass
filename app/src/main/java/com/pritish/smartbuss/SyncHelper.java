package com.pritish.smartbuss;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SyncHelper {

    private final DatabaseReference bookingsRef;
    private final DatabaseReference bookingForOthersRef;

    public SyncHelper() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        bookingsRef = database.getReference("Bookings");
        bookingForOthersRef = database.getReference("Booking_for_others");
    }

    /**
     * Sync updates from Bookings → Booking_for_others
     */
    public void syncBookingToForOthers(String passengerPhone, String transactionId) {
        bookingsRef.child(passengerPhone).child(transactionId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Get the updated booking data
                            Booking booking = snapshot.getValue(Booking.class);
                            if (booking != null) {
                                // Find the corresponding booking in Booking_for_others
                                bookingForOthersRef.orderByChild("transactionId").equalTo(transactionId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                                                    // Update Booking_for_others
                                                    bookingForOthersRef.child(bookingSnapshot.getKey())
                                                            .setValue(booking);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Log.e("SyncHelper", "Failed to sync Booking → Booking_for_others", error.toException());
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SyncHelper", "Failed to listen to Bookings updates", error.toException());
                    }
                });
    }

    /**
     * Sync updates from Booking_for_others → Bookings
     */
    public void syncForOthersToBooking(String mainUserPhone, String transactionId) {
        bookingForOthersRef.child(mainUserPhone).child(transactionId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Booking_for_others bookingForOthers = snapshot.getValue(Booking_for_others.class);
                            if (bookingForOthers != null) {
                                // Convert to Booking object (if needed)
                                Booking booking = new Booking(
                                        bookingForOthers.getTransactionId(),
                                        bookingForOthers.getStartStop(),
                                        bookingForOthers.getDestinationStop(),
                                        bookingForOthers.getPassengerPhone(),
                                        bookingForOthers.getPrice(),  // Changed from getTicketPrice() to getPrice()
                                        bookingForOthers.getDate(),
                                        bookingForOthers.getTime(),
                                        bookingForOthers.getPersonCount()
                                );

                                // Update Bookings node
                                bookingsRef.child(bookingForOthers.getPassengerPhone())
                                        .child(transactionId)
                                        .setValue(booking);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("SyncHelper", "Failed to listen to Booking_for_others updates", error.toException());
                    }
                });
    }
}