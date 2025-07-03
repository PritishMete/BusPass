package com.pritish.smartbuss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class conductor_home extends Fragment {

    // CardView declarations
    CardView viewPassengerListButtonCard;
    CardView currentBookingCard;
    CardView scanTicketButtonCard;
    CardView manageRouteButtonCard;
    CardView emergencyCard;
    CardView viewMapCard; // Add this for the map view card

    // Regular Button declaration
    Button emergencyButton;

    TextView conductorNameTextView;
    TextView busNumberTextView;
    TextView phoneNumberText;
    TextView name;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conductor_home, container, false);

        // Initialize views with correct types
        currentBookingCard = view.findViewById(R.id.CurrentBooking);
        scanTicketButtonCard = view.findViewById(R.id.scanTicketButton);
        emergencyCard = view.findViewById(R.id.emergencyButton);
        viewMapCard = view.findViewById(R.id.CurrentBooking); // Initialize the map view card

        name = view.findViewById(R.id.name);
        busNumberTextView = view.findViewById(R.id.busNo);
        phoneNumberText = view.findViewById(R.id.phone_number_text_view);

        // Get phone number from arguments
        String mainuserPhone = getArguments() != null ? getArguments().getString("mainuserPhone") : null;
        if (phoneNumberText != null) {
            phoneNumberText.setText(mainuserPhone != null ? "Phone: " + mainuserPhone : "Phone: Not available");
        }
        String userName = getArguments() != null ? getArguments().getString("userName") : null;
        if (name != null) {
            name.setText(userName != null ? "name: " + userName : "name: Not available");
        }
        if (busNumberTextView != null) {
            busNumberTextView.setText(userName != null ? "bus number: " + "s12" : "bus number: Not available");
        }

        // Set click listeners
        if (viewPassengerListButtonCard != null) {
            viewPassengerListButtonCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "View Passengers", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (currentBookingCard != null) {
            currentBookingCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    startActivity(new Intent(getActivity(), conductor_current_booking.class));
                }
            });
        }

        if (scanTicketButtonCard != null) {
            scanTicketButtonCard.setOnClickListener(v -> {
                if (getActivity() != null  && mainuserPhone != null) {
                    Intent intent = new Intent(getActivity(), QRScannerActivity.class);
                    intent.putExtra("mainuserPhone", mainuserPhone);
                    intent.putExtra("conductorName", userName);
                    startActivity(intent);
                }
            });
        }

        if (manageRouteButtonCard != null) {
            manageRouteButtonCard.setOnClickListener(v -> {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Manage Route", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Add click listener for the map view card
        if (viewMapCard != null) {
            viewMapCard.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    Intent intent = new Intent(getActivity(), conductor_map.class);
                    intent.putExtra("mainuserPhone", mainuserPhone);
                    startActivity(intent);
                }
            });
        }

        // Emergency card click listener to open user_emergency dialog
        if (emergencyCard != null) {
            emergencyCard.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    user_emergency emergencyDialog = user_emergency.newInstance(mainuserPhone);
                    if (getParentFragmentManager() != null) {
                        emergencyDialog.show(getParentFragmentManager(), "EmergencyDialog");
                    }
                }
            });
        }

        // Emergency button click listener (if using regular button)
        if (emergencyButton != null) {
            emergencyButton.setOnClickListener(v -> {
                if (getActivity() != null && mainuserPhone != null) {
                    user_emergency emergencyDialog = user_emergency.newInstance(mainuserPhone);
                    if (getParentFragmentManager() != null) {
                        emergencyDialog.show(getParentFragmentManager(), "EmergencyDialog");
                    }
                }
            });
        }

        return view;
    }
}