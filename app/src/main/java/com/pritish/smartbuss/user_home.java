package com.pritish.smartbuss;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class user_home extends Fragment {

    private static final String ACTION_EMERGENCY_UPDATE = "com.pritish.smartbuss.EMERGENCY_UPDATE";

    private View notificationDot;
    private TextView notificationCount;
    private DatabaseReference emergencyRef;
    private String mainuserPhone;
    private BroadcastReceiver emergencyReceiver;

    public user_home() {

        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.user_home, container, false);

        // Retrieve arguments once
        final Bundle args = getArguments();
        mainuserPhone = args != null ? args.getString("mainuserPhone") : null;
        final String selectedStart = args != null ? args.getString("selectedStart") : null;
        final String selectedStop = args != null ? args.getString("selectedStop") : null;
        final String routes = args != null ? args.getString("routes") : null;
        final float ticketPrice = args != null ? args.getFloat("ticketPrice", 0.0f) : 0.0f;

        // Initialize Firebase reference
        emergencyRef = FirebaseDatabase.getInstance().getReference("emergency");

        // Initialize notification elements
        initializeNotificationElements(view);

        // --- Standard Buttons ---
        Button normalBookingButton = view.findViewById(R.id.normalBookingButton);
        Button quickBookingButton = view.findViewById(R.id.quickBookingButton);
        View buslocation = view.findViewById(R.id.buslocation);

        // --- CardView elements that act as buttons ---
        CardView showTicketCard = view.findViewById(R.id.show_ticket_card);
        CardView bookingHistoryCard = view.findViewById(R.id.booking_history_card);
        CardView emergencyButton = view.findViewById(R.id.emergencyButton);
        CardView savedRoutesCard = view.findViewById(R.id.savedroute);

        // --- Setting up Phone Number TextView ---
        final TextView phoneNumberText = view.findViewById(R.id.phone_number_text_view);
        if (mainuserPhone != null) {
            phoneNumberText.setText("Phone Number: " + mainuserPhone);
        } else {
            phoneNumberText.setText("Phone Number: Not available");
        }

        // Set up all click listeners
        setupClickListeners(normalBookingButton, quickBookingButton, showTicketCard,
                bookingHistoryCard, emergencyButton, savedRoutesCard, buslocation);

        // Setup broadcast receiver for emergency updates
        setupBroadcastReceiver();

        // Load initial emergency message count
        loadEmergencyMessageCount();

        // Log received arguments
        Log.d("user_home", "Received arguments - " +
                "Start: " + selectedStart + ", " +
                "Stop: " + selectedStop + ", " +
                "Route: " + routes);

        return view;
    }

    private void initializeNotificationElements(View view) {
        // Find the notification dot and count views
        // You'll need to add these to your emergency card layout

        // Initially hide notification elements
        if (notificationDot != null) {
            notificationDot.setVisibility(View.GONE);
        }
        if (notificationCount != null) {
            notificationCount.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners(Button normalBookingButton, Button quickBookingButton,
                                     CardView showTicketCard, CardView bookingHistoryCard,
                                     CardView emergencyButton, CardView savedRoutesCard,
                                     View buslocation) {

        // --- OnClickListener for Normal Booking button ---
        if (normalBookingButton != null) {
            normalBookingButton.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    user_NormalBooking dialog = user_NormalBooking.newInstance(mainuserPhone);
                    if (getParentFragmentManager() != null) {
                        dialog.show(getParentFragmentManager(), "NormalBookingDialog");
                    }
                }
            });
        }

        // --- OnClickListener for Quick Booking button ---
        if (quickBookingButton != null) {
            quickBookingButton.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    Intent intent = new Intent(getActivity(), user_map.class);
                    intent.putExtra("mainuserPhone", mainuserPhone);
                    startActivity(intent);
                }
            });
        }

        // --- OnClickListener for show_ticket_card ---
        if (showTicketCard != null) {
            showTicketCard.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    Intent intent = new Intent(getActivity(), user_showticket.class);
                    intent.putExtra("mainuserPhone", mainuserPhone);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().overridePendingTransition(0, 0);
                    }
                }
            });
        }

        if (bookingHistoryCard != null) {
            bookingHistoryCard.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    Intent intent = new Intent(getActivity(), user_showticket.class);
                    intent.putExtra("mainuserPhone", mainuserPhone);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().overridePendingTransition(0, 0);
                    }
                }
            });
        }

        // --- OnClickListener for savedRoutesCard ---
        if (savedRoutesCard != null) {
            savedRoutesCard.setOnClickListener(v -> {
                Log.d("user_home", "Saved Routes Card clicked!");
                Intent intent = new Intent(getActivity(), savedroute.class);
                if (mainuserPhone != null) {
                    intent.putExtra("mainuserPhone", mainuserPhone);
                }
                startActivity(intent);
            });
        }

        // --- Enhanced OnClickListener for Emergency Button ---
        if (emergencyButton != null) {
            emergencyButton.setOnClickListener(v -> {
                Log.d("user_home", "Emergency Card clicked!");

                // Hide notification dot when emergency is clicked
                hideNotificationDot();

                // Show emergency dialog instead of starting activity
                if (getActivity() != null && mainuserPhone != null) {
                    user_emergency emergencyDialog = user_emergency.newInstance(mainuserPhone);
                    if (getParentFragmentManager() != null) {
                        emergencyDialog.show(getParentFragmentManager(), "EmergencyDialog");
                    }
                }
            });
        }

        // --- OnClickListener for the Bus Location view ---
        if (buslocation != null) {
            buslocation.setOnClickListener(v -> {
                Log.d("user_home", "buslocation view clicked!");
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), bus_location.class);
                    if (mainuserPhone != null) {
                        intent.putExtra("userPhone", mainuserPhone);
                    }
                    startActivity(intent);
                }
            });
        }
    }

    private void setupBroadcastReceiver() {
        emergencyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_EMERGENCY_UPDATE.equals(intent.getAction())) {
                    int messageCount = intent.getIntExtra("messageCount", 0);
                    boolean hasNewMessages = intent.getBooleanExtra("hasNewMessages", false);

                    if (hasNewMessages && messageCount > 0) {
                        showNotificationDot(messageCount);

                        // Show toast for new emergency message
                        Toast.makeText(context, "New emergency message received!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // Register the receiver
        if (getContext() != null) {
            LocalBroadcastManager.getInstance(getContext())
                    .registerReceiver(emergencyReceiver, new IntentFilter(ACTION_EMERGENCY_UPDATE));
        }
    }

    private void loadEmergencyMessageCount() {
        if (emergencyRef != null) {
            emergencyRef.orderByChild("status").equalTo("approved")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            int messageCount = (int) snapshot.getChildrenCount();

                            if (messageCount > 0) {
                                showNotificationDot(messageCount);
                            } else {
                                hideNotificationDot();
                            }

                            Log.d("user_home", "Emergency message count: " + messageCount);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("user_home", "Failed to load emergency count: " + error.getMessage());
                        }
                    });
        }
    }

    private void showNotificationDot(int count) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (notificationDot != null) {
                notificationDot.setVisibility(View.VISIBLE);

                // Add a subtle animation
                notificationDot.setScaleX(0f);
                notificationDot.setScaleY(0f);
                notificationDot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start();
            }

            if (notificationCount != null && count > 0) {
                notificationCount.setText(String.valueOf(Math.min(count, 99))); // Cap at 99
                notificationCount.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideNotificationDot() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (notificationDot != null) {
                notificationDot.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(200)
                        .withEndAction(() -> notificationDot.setVisibility(View.GONE))
                        .start();
            }

            if (notificationCount != null) {
                notificationCount.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Unregister broadcast receiver
        if (emergencyReceiver != null && getContext() != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(emergencyReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload emergency count when fragment resumes
        loadEmergencyMessageCount();
    }
}