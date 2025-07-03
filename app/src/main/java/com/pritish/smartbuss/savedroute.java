package com.pritish.smartbuss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class savedroute extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FrequentRouteAdapter adapter;
    private List<FrequentRoute> frequentRoutes;
    private DatabaseReference bookingsRef;
    private String userPhone;
    private TextView emptyMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.savedroute);

        Intent intent = getIntent();
        String mainuserPhone = intent.getStringExtra("mainuserPhone");
        if (mainuserPhone != null) {
            Log.d("saved route", "mainuserPhone: " + mainuserPhone);
        }

        recyclerView = findViewById(R.id.frequentRoutesRecyclerView);
        emptyMessageText = findViewById(R.id.emptyMessageText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        frequentRoutes = new ArrayList<>();
        adapter = new FrequentRouteAdapter(frequentRoutes);
        recyclerView.setAdapter(adapter);

        // Get user phone from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("smartbus_pref", MODE_PRIVATE);
        userPhone = sharedPreferences.getString("loggedInPhone", null);

        if (userPhone != null) {
            loadBookingHistory();
        } else {
            showEmptyState();
        }
    }

    private void loadBookingHistory() {
        // Reference to user's bookings in Firebase
        bookingsRef = FirebaseDatabase.getInstance()
                .getReference("Bookings")
                .child(userPhone);

        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> routeCountMap = new HashMap<>();

                // Iterate through all booking tickets for this user
                for (DataSnapshot ticketSnapshot : snapshot.getChildren()) {
                    String startStop = null;
                    String destinationStop = null;

                    // Get startStop and destinationStop from ticket details
                    if (ticketSnapshot.hasChild("startStop")) {
                        startStop = ticketSnapshot.child("startStop").getValue(String.class);
                    }
                    if (ticketSnapshot.hasChild("destinationStop")) {
                        destinationStop = ticketSnapshot.child("destinationStop").getValue(String.class);
                    }

                    // If both stops are available, count this route
                    if (startStop != null && destinationStop != null &&
                            !startStop.trim().isEmpty() && !destinationStop.trim().isEmpty()) {

                        String routeKey = startStop.trim() + " -> " + destinationStop.trim();
                        routeCountMap.put(routeKey, routeCountMap.getOrDefault(routeKey, 0) + 1);

                        Log.d("BookingRoute", "Found route: " + routeKey +
                                " Count: " + routeCountMap.get(routeKey));
                    }
                }

                // Filter routes with count >= 5 and update UI
                updateFrequentRoutes(routeCountMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BookingHistory", "Failed to load booking history", error.toException());
                Toast.makeText(savedroute.this,
                        "Failed to load booking history. Please check your connection.",
                        Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void updateFrequentRoutes(Map<String, Integer> routeCountMap) {
        frequentRoutes.clear();

        // Add routes that have been traveled 5 or more times
        for (Map.Entry<String, Integer> entry : routeCountMap.entrySet()) {
            if (entry.getValue() >= 5) {
                String[] stops = entry.getKey().split(" -> ");
                if (stops.length == 2) {
                    frequentRoutes.add(new FrequentRoute(stops[0], stops[1], entry.getValue()));
                }
            }
        }

        // Update UI based on whether we have frequent routes
        if (frequentRoutes.isEmpty()) {
            showEmptyState();
        } else {
            showFrequentRoutes();
        }
    }

    private void showEmptyState() {
        if (emptyMessageText != null) {
            emptyMessageText.setText("No saved routes\nBook a ticket for more than 5 times to see saved routes");
            emptyMessageText.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
    }

    private void showFrequentRoutes() {
        if (emptyMessageText != null) {
            emptyMessageText.setVisibility(View.GONE);
        }
        recyclerView.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();

        Log.d("FrequentRoutes", "Showing " + frequentRoutes.size() + " frequent routes");
    }

    // FrequentRoute model class
    public static class FrequentRoute {
        public String startStop;
        public String endStop;
        public int tripCount;

        public FrequentRoute(String startStop, String endStop, int tripCount) {
            this.startStop = startStop;
            this.endStop = endStop;
            this.tripCount = tripCount;
        }
    }

    // RecyclerView Adapter
    public class FrequentRouteAdapter extends RecyclerView.Adapter<FrequentRouteAdapter.ViewHolder> {

        private List<FrequentRoute> routes;

        public FrequentRouteAdapter(List<FrequentRoute> routes) {
            this.routes = routes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_frequent_route, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FrequentRoute route = routes.get(position);
            holder.startStopText.setText(route.startStop);
            holder.endStopText.setText(route.endStop);
            holder.tripCountText.setText(String.format("Trips: %d", route.tripCount));

            // Add click listener for the card
            holder.cardView.setOnClickListener(v -> {
                // You can add functionality here to book this route again
                Toast.makeText(savedroute.this,
                        "Selected: " + route.startStop + " to " + route.endStop,
                        Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView startStopText, endStopText, tripCountText;
            CardView cardView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                startStopText = itemView.findViewById(R.id.startStopText);
                endStopText = itemView.findViewById(R.id.endStopText);
                tripCountText = itemView.findViewById(R.id.tripCountText);
            }
        }
    }
}