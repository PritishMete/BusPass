package com.pritish.smartbuss;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class conductor_incentive extends Fragment implements DayAdapter.OnDayClickListener {

    private static final String TAG = "ConductorIncentive";
    private static final String ARG_CONDUCTOR_PHONE = "mainuserPhone";

    private String mainuserPhone;
    private TextView tvTotalScans;
    private TextView tvTotalAmount;
    private RecyclerView ticketsRecyclerView;
    private TicketAdapter ticketAdapter;
    private List<TicketItem> ticketList;
    private ProgressBar progressBar;
    private TextView tvNoDataMessage;

    // --- Views for the new Day Selection Bar ---
    private RecyclerView daySelectorRecyclerView;
    private TextView tvSelectedDateTitle;
    private DayAdapter dayAdapter;
    private List<DayInfo> last7Days = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mainuserPhone = getArguments().getString(ARG_CONDUCTOR_PHONE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conductor_incentive, container, false);

        tvTotalScans = view.findViewById(R.id.tv_weekly_detail_total_item_count);
        tvTotalAmount = view.findViewById(R.id.tv_weekly_detail_total_amount);
        ticketsRecyclerView = view.findViewById(R.id.recycler_view_incentives);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoDataMessage = view.findViewById(R.id.tv_no_data_message);
        daySelectorRecyclerView = view.findViewById(R.id.rv_day_selection);
        tvSelectedDateTitle = view.findViewById(R.id.tv_selected_date_title);

        ticketList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(ticketList);
        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ticketsRecyclerView.setAdapter(ticketAdapter);

        setupDaySelector();

        return view;
    }

    private void setupDaySelector() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            String dayNumber = date.format(DateTimeFormatter.ofPattern("dd"));
            String monthAbbr = date.format(DateTimeFormatter.ofPattern("MMM"));
            String dayNameAbbr = date.format(DateTimeFormatter.ofPattern("E"));
            last7Days.add(new DayInfo(dayNumber, monthAbbr, dayNameAbbr, date));
        }

        dayAdapter = new DayAdapter(last7Days, this);
        daySelectorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        daySelectorRecyclerView.setAdapter(dayAdapter);

        dayAdapter.setSelectedPosition(0);
        onDayClicked(0, last7Days.get(0));
    }

    @Override
    public void onDayClicked(int position, DayInfo dayInfo) {
        tvSelectedDateTitle.setText(dayInfo.getFullDateTitle());
        String dateForFirebase = dayInfo.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // Fetch tickets, which will now also calculate and set the totals.
        fetchScannedTickets(dateForFirebase);
    }

    private void fetchScannedTickets(String date) {
        if (mainuserPhone == null) {
            Log.e(TAG, "Conductor phone number is null.");
            return;
        }

        showLoading();
        // Now fetch all verified scans from the 'details' node
        DatabaseReference scansRef = FirebaseDatabase.getInstance().getReference()
                .child("Conductor").child(mainuserPhone).child("Scans").child(date).child("details");

        scansRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot detailsSnapshot) {
                List<TicketItem> allTickets = new ArrayList<>();
                for (DataSnapshot scanSnapshot : detailsSnapshot.getChildren()) {
                    String type = scanSnapshot.child("type").getValue(String.class);
                    // Process only RFID_VERIFIED and QR_VERIFIED types
                    if ("RFID_VERIFIED".equals(type) || "QR_VERIFIED".equals(type)) {
                        TicketItem ticket = createTicketItemFromScanDetailsSnapshot(scanSnapshot);
                        if (ticket != null) {
                            allTickets.add(ticket);
                        }
                    }
                }
                processTicketList(allTickets);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read scan details", error.toException());
                hideLoading();
                tvNoDataMessage.setVisibility(View.VISIBLE);
                ticketsRecyclerView.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to load tickets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processTicketList(List<TicketItem> tickets) {
        double totalAmount = 0.0;
        for (TicketItem ticket : tickets) {
            totalAmount += ticket.getFare();
            Log.d(TAG, "Processed ticket: " + ticket.getTransactionId() +
                    " Fare: " + ticket.getFare());
        }

        // Update UI
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
        tvTotalScans.setText(String.format(Locale.getDefault(), "(%d tickets)", tickets.size()));

        // Sort by most recent first
        Collections.sort(tickets, (t1, t2) -> t2.getScannedAt().compareTo(t1.getScannedAt()));

        ticketList.clear();
        ticketList.addAll(tickets);
        ticketAdapter.notifyDataSetChanged();

        hideLoading();
        tvNoDataMessage.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
        ticketsRecyclerView.setVisibility(tickets.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // New method to create TicketItem specifically from the 'details' node snapshot
    private TicketItem createTicketItemFromScanDetailsSnapshot(DataSnapshot snapshot) {
        try {
            String from = getStringValue(snapshot, "startStop");
            String to = getStringValue(snapshot, "destinationStop");
            String busNumber = getStringValue(snapshot, "busNumber");
            String transactionId = getStringValue(snapshot, "value");

            // Get fare as double from "price" field
            double fare = 0.0;
            Object fareObj = snapshot.child("price").getValue();
            if (fareObj instanceof Double) {
                fare = (Double) fareObj;
            } else if (fareObj instanceof Long) {
                fare = ((Long) fareObj).doubleValue();
            } else if (fareObj instanceof String) {
                try {
                    fare = Double.parseDouble((String) fareObj);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing fare string: " + fareObj, e);
                }
            }

            Log.d(TAG, "Creating ticket item with fare: " + fare);

            int personCount = getIntValue(snapshot, "personCount", 1);
            String scannedAtTime = extractTimeFromDateTime(getStringValue(snapshot, "timestamp"));

            return new TicketItem(
                    from != null ? from : "Unknown",
                    to != null ? to : "Unknown",
                    fare,
                    personCount,
                    scannedAtTime,
                    busNumber != null ? busNumber : "N/A",
                    transactionId != null ? transactionId : "N/A"
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating TicketItem from scan details snapshot", e);
            return null;
        }
    }



    private String getStringValue(DataSnapshot snapshot, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (key != null && snapshot.hasChild(key)) {
                return snapshot.child(key).getValue(String.class);
            }
        }
        return null;
    }
    private double getDoubleValue(DataSnapshot snapshot, String... possibleKeys) {
        for (String key : possibleKeys) {
            if (key != null && snapshot.hasChild(key)) {
                Object value = snapshot.child(key).getValue();
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid number format for key: " + key);
                    }
                }
            }
        }
        return 0.0;
    }
    private int getIntValue(DataSnapshot snapshot, String key, int defaultValue) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid integer format for key: " + key);
                }
            }
        }
        return defaultValue;
    }

    private String extractTimeFromDateTime(String dateTime) {
        if (dateTime == null) return "N/A";
        // Check if it's already just time (HH:mm:ss)
        if (dateTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
            try {
                // Parse it as just time to format it with AM/PM
                Date date = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(dateTime);
                return new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(date);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing time only: " + dateTime, e);
                return dateTime; // Fallback to original if formatting fails
            }
        }
        // Otherwise, assume full date-time format
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateTime);
            return new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing dateTime: " + dateTime, e);
            return dateTime; // Fallback to original string if parsing fails
        }
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        ticketsRecyclerView.setVisibility(View.GONE);
        tvNoDataMessage.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private static class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {
        private final List<TicketItem> ticketList;
        public TicketAdapter(List<TicketItem> ticketList) { this.ticketList = ticketList; }

        @NonNull @Override
        public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_card, parent, false);
            return new TicketViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
            TicketItem ticket = ticketList.get(position);
            holder.routeTextView.setText(String.format("%s to %s", ticket.getFrom(), ticket.getTo()));
            holder.fareTextView.setText(String.format("₹%.2f", ticket.getFare())); // Ensure 2 decimal places for fare
            holder.scannedAtTextView.setText(ticket.getScannedAt());
            holder.personCountTextView.setText(String.format("%s Persons", ticket.getPersonCount()));
            holder.busNumberTextView.setText(String.format("Bus: %s", ticket.getBusNumber()));
            holder.transactionIdTextView.setText(String.format("ID: %s", ticket.getTransactionId()));
        }

        @Override public int getItemCount() { return ticketList.size(); }

        static class TicketViewHolder extends RecyclerView.ViewHolder {
            TextView routeTextView, fareTextView, scannedAtTextView, personCountTextView, busNumberTextView, transactionIdTextView;
            public TicketViewHolder(@NonNull View itemView) {
                super(itemView);
                routeTextView = itemView.findViewById(R.id.routeTextView);
                fareTextView = itemView.findViewById(R.id.fareTextView);
                scannedAtTextView = itemView.findViewById(R.id.scannedAtTextView);
                personCountTextView = itemView.findViewById(R.id.personCountTextView);
                busNumberTextView = itemView.findViewById(R.id.busNumberTextView);
                transactionIdTextView = itemView.findViewById(R.id.transactionIdTextView);
            }
        }
    }

    public static class TicketItem {
        private String from, to, scannedAt, busNumber, transactionId;
        private double fare;
        private int personCount;

        public TicketItem() {} // Required for Firebase

        public TicketItem(String from, String to, double fare, int personCount,
                          String scannedAt, String busNumber, String transactionId) {
            this.from = from;
            this.to = to;
            this.fare = fare;
            this.personCount = personCount;
            this.scannedAt = scannedAt;
            this.busNumber = busNumber;
            this.transactionId = transactionId;
        }

        // Getters
        public String getFrom() { return from; }
        public String getTo() { return to; }
        public double getFare() { return fare; }
        public int getPersonCount() { return personCount; }
        public String getScannedAt() { return scannedAt; }
        public String getBusNumber() { return busNumber; }
        public String getTransactionId() { return transactionId; }
    }}