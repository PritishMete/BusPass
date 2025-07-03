package com.pritish.smartbuss;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class user_wallet extends Fragment {

    private static final String TAG = "UserWalletFragment";
    private static final String WALLET_PREFS = "WalletPrefs";
    private static final String WALLET_BALANCE_KEY = "wallet_balance";
    private static final String USER_DETAILS_PREFS = "smartbus_pref";
    private static final String KEY_LOGGED_IN_PHONE_FOR_SESSION = "loggedInPhone";

    // UI Elements
    private TextView walletBalanceText;
    private MaterialButton rechargeWalletButton;
    private MaterialCardView  passbookCardButton;
    private LineChart spendInsightsLineChart;
    private TextView ticketsBookedValueText, moneySpentValueText, topDestinationValueText;
    private TextView graphLabelMarText, graphLabelAprText;

    // Firebase References
    private DatabaseReference userRef;
    private DatabaseReference userBookingsRef;
    private String currentUserPhoneNumber;
    private ValueEventListener walletBalanceListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView CALLED.");
        View view = inflater.inflate(R.layout.user_wallet, container, false);

        initializeViews(view);
        currentUserPhoneNumber = getCurrentUserPhoneNumber();

        if (currentUserPhoneNumber == null || currentUserPhoneNumber.isEmpty()) {
            Log.w(TAG, "onCreateView: CurrentUserPhoneNumber is null or empty.");
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_LONG).show();
            setDefaultInsights();
        } else {
            Log.d(TAG, "onCreateView: CurrentUserPhoneNumber: " + currentUserPhoneNumber);
            initializeFirebaseReferences();
        }

        setupClickListeners();
        setupEmptySpendInsightsChart();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume CALLED.");
        loadWalletBalance();
        if (currentUserPhoneNumber != null && !currentUserPhoneNumber.isEmpty()) {
            fetchAndProcessSpendInsights();
            setupWalletBalanceListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause CALLED.");
        if (walletBalanceListener != null && userRef != null) {
            Log.d(TAG, "onPause: Removing walletBalanceListener.");
            userRef.child("walletBalance").removeEventListener(walletBalanceListener);
        }
    }

    private void initializeViews(View view) {
        Log.d(TAG, "initializeViews CALLED.");
        walletBalanceText = view.findViewById(R.id.walletBalanceText);
        rechargeWalletButton = view.findViewById(R.id.rechargeWalletButton);
        passbookCardButton = view.findViewById(R.id.passbookCardButton);
        if (passbookCardButton == null) {
            Log.e(TAG, "CRITICAL: passbookCardButton is NULL after findViewById!");
        } else {
            Log.d(TAG, "passbookCardButton was found successfully.");
        }

        spendInsightsLineChart = view.findViewById(R.id.spendInsightsLineChart);
        ticketsBookedValueText = view.findViewById(R.id.ticketsBookedValue);
        moneySpentValueText = view.findViewById(R.id.moneySpentValue);
        topDestinationValueText = view.findViewById(R.id.topDestinationValue);
        graphLabelMarText = view.findViewById(R.id.graphLabelMar);
        graphLabelAprText = view.findViewById(R.id.graphLabelApr);
        Log.d(TAG, "initializeViews FINISHED.");
    }

    private void initializeFirebaseReferences() {
        Log.d(TAG, "initializeFirebaseReferences CALLED for user: " + currentUserPhoneNumber);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Traveler").child(currentUserPhoneNumber);
        userBookingsRef = database.getReference("Bookings").child(currentUserPhoneNumber);
    }

    private void setupClickListeners() {
        Log.d(TAG, "setupClickListeners CALLED.");

        if (rechargeWalletButton != null) {
            rechargeWalletButton.setOnClickListener(v -> {
                Log.d(TAG, "RechargeWalletButton CLICKED.");
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), UserWalletRechargeActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "RechargeWalletButton: Context is NULL.");
                }
            });
        } else {
            Log.e(TAG, "setupClickListeners: rechargeWalletButton is NULL.");
        }

        if (passbookCardButton != null) {
            passbookCardButton.setOnClickListener(v -> {
                Log.d(TAG, "PassbookCardButton CLICKED!");
                if (getContext() != null) {
                    Log.d(TAG, "PassbookCardButton: Context is NOT null, attempting to start WalletPassbookActivity.");
                    Intent intent = new Intent(getContext(), WalletPassbookActivity.class);
                    try {
                        startActivity(intent);
                        Log.d(TAG, "PassbookCardButton: startActivity(WalletPassbookActivity) CALLED successfully.");
                    } catch (Exception e) {
                        Log.e(TAG, "PassbookCardButton: EXCEPTION trying to startActivity: " + e.getMessage(), e);
                        if(getActivity() != null) {
                            Toast.makeText(getActivity(), "Error launching passbook: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Log.e(TAG, "PassbookCardButton: Context IS NULL when trying to start WalletPassbookActivity!");
                    if(getActivity() != null) {
                        Toast.makeText(getActivity(), "Error opening passbook (null context).", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Log.e(TAG, "setupClickListeners: passbookCardButton is NULL, cannot set listener.");
        }
        Log.d(TAG, "setupClickListeners FINISHED.");
    }

    private String getCurrentUserPhoneNumber() {
        if (getContext() == null) {
            Log.w(TAG, "Context is null in getCurrentUserPhoneNumber, returning null.");
            return null;
        }
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(USER_DETAILS_PREFS, Context.MODE_PRIVATE);
        String phone = sharedPreferences.getString(KEY_LOGGED_IN_PHONE_FOR_SESSION, null);
        Log.d(TAG, "getCurrentUserPhoneNumber: Retrieved phone: " + phone);
        return phone;
    }

    private void setupWalletBalanceListener() {
        if (userRef == null) {
            Log.w(TAG, "setupWalletBalanceListener: userRef is NULL, cannot set listener.");
            return;
        }
        Log.d(TAG, "setupWalletBalanceListener CALLED.");

        walletBalanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && getContext() != null) {
                    Log.d(TAG, "walletBalanceListener: onDataChange: snapshot exists = " + snapshot.exists());
                    if (snapshot.exists()) {
                        Float firebaseBalanceFloat = snapshot.getValue(Float.class);
                        if (firebaseBalanceFloat != null) {
                            Log.d(TAG, "walletBalanceListener: Fetched Firebase balance: " + firebaseBalanceFloat);
                            updateWalletBalanceLocally(firebaseBalanceFloat);
                            updateWalletBalanceUI(firebaseBalanceFloat);
                        } else {
                            Log.w(TAG, "walletBalanceListener: Firebase balance is null in snapshot.");
                        }
                    }
                } else {
                    Log.w(TAG, "walletBalanceListener: onDataChange: Fragment not added or context is null.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "walletBalanceListener: Failed to listen for wallet balance updates", error.toException());
            }
        };

        userRef.child("walletBalance").addValueEventListener(walletBalanceListener);
    }

    private void loadWalletBalance() {
        if (getContext() == null) {
            Log.w(TAG, "loadWalletBalance: Context is null. Cannot load balance.");
            return;
        }
        if (walletBalanceText == null) {
            Log.w(TAG, "loadWalletBalance: walletBalanceText is null. Cannot update UI.");
            return;
        }
        if (currentUserPhoneNumber == null || currentUserPhoneNumber.isEmpty()) {
            Log.w(TAG, "loadWalletBalance: No user logged in.");
            setDefaultInsights();
            return;
        }

        String walletPrefsKey = "WalletPrefs_" + currentUserPhoneNumber;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(walletPrefsKey, Context.MODE_PRIVATE);
        float currentBalance = sharedPreferences.getFloat(WALLET_BALANCE_KEY, -1.0f);

        boolean isFirstLoad = (currentBalance == -1.0f);
        Log.d(TAG, "loadWalletBalance: isFirstLoad = " + isFirstLoad + ", currentBalance = " + currentBalance);

        if (isFirstLoad && userRef != null) {
            Log.d(TAG, "loadWalletBalance: First load, fetching from Firebase");
            userRef.child("walletBalance").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded() || getContext() == null) return;

                    float firebaseBalance;
                    if (snapshot.exists() && snapshot.getValue(Float.class) != null) {
                        firebaseBalance = snapshot.getValue(Float.class);
                        Log.d(TAG, "loadWalletBalance: Firebase balance exists: " + firebaseBalance);
                    } else {
                        firebaseBalance = 500.00f;
                        Log.d(TAG, "loadWalletBalance: Setting default balance: " + firebaseBalance);
                        if (userRef != null) {
                            userRef.child("walletBalance").setValue(firebaseBalance);
                        }
                    }
                    updateWalletBalanceLocally(firebaseBalance);
                    updateWalletBalanceUI(firebaseBalance);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "loadWalletBalance: Firebase error", error.toException());
                    if (isAdded() && getContext() != null) {
                        float defaultBalance = 500.00f;
                        updateWalletBalanceLocally(defaultBalance);
                        updateWalletBalanceUI(defaultBalance);
                    }
                }
            });
        } else if (userRef == null && isFirstLoad) {
            Log.w(TAG, "loadWalletBalance: userRef is null and no local balance");
            float defaultBalance = 500.00f;
            updateWalletBalanceLocally(defaultBalance);
            updateWalletBalanceUI(defaultBalance);
        } else {
            Log.d(TAG, "loadWalletBalance: Using existing local balance: " + currentBalance);
            updateWalletBalanceUI(currentBalance);
        }
    }

    private void updateWalletBalanceLocally(float balance) {
        if (getContext() == null) {
            Log.w(TAG, "updateWalletBalanceLocally: Context is null");
            return;
        }
        if (currentUserPhoneNumber == null || currentUserPhoneNumber.isEmpty()) {
            Log.w(TAG, "updateWalletBalanceLocally: No user logged in");
            return;
        }

        String walletPrefsKey = "WalletPrefs_" + currentUserPhoneNumber;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(walletPrefsKey, Context.MODE_PRIVATE);

        float oldBalance = sharedPreferences.getFloat(WALLET_BALANCE_KEY, 0.0f);

        sharedPreferences.edit()
                .putFloat(WALLET_BALANCE_KEY, balance)
                .apply();
        Log.d(TAG, "updateWalletBalanceLocally: Updated balance from " + oldBalance + " to " + balance);

        if (Math.abs(oldBalance - balance) > 0.001f) {
            sendWalletUpdateNotification(balance, oldBalance);

            if (isAdded() && walletBalanceText != null) {
                getActivity().runOnUiThread(() ->
                        updateWalletBalanceUI(balance));
            }
        }
    }


    private void updateWalletBalanceUI(float balance) {
        if (walletBalanceText != null) {
            Log.d(TAG, "updateWalletBalanceUI: Setting text to " + balance);
            walletBalanceText.setText(String.format(Locale.getDefault(), "₹%.2f", balance));
        } else {
            Log.w(TAG, "updateWalletBalanceUI: walletBalanceText is null.");
        }
    }

    public static void updateWalletBalance(Context context, String phoneNumber, float newBalance) {
        if (context == null || phoneNumber == null) {
            Log.e(TAG, "updateWalletBalance: Context or phoneNumber is null");
            return;
        }

        String prefsKey = "WalletPrefs_" + phoneNumber;
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsKey, Context.MODE_PRIVATE);
    }

    private void sendWalletUpdateNotification(float newBalance, float oldBalance) {
        if (getContext() == null) {
            Log.w(TAG, "sendWalletUpdateNotification: Context is null.");
            return;
        }

        String message;
        if (newBalance > oldBalance) {
            message = String.format(Locale.getDefault(),
                    "Wallet recharged! +₹%.2f. New balance: ₹%.2f",
                    (newBalance - oldBalance), newBalance);
        } else if (oldBalance > newBalance) {
            message = String.format(Locale.getDefault(),
                    "Amount debited: -₹%.2f. New balance: ₹%.2f",
                    (oldBalance - newBalance), newBalance);
        } else {
            Log.d(TAG, "sendWalletUpdateNotification: No balance change.");
            return;
        }
        Log.d(TAG, "sendWalletUpdateNotification: Message: " + message);
        NotificationHelper.saveNotification(getContext(), message);
    }


    private void setDefaultInsights() {
        Log.d(TAG, "setDefaultInsights CALLED.");
        if (ticketsBookedValueText != null) ticketsBookedValueText.setText("0");
        if (moneySpentValueText != null) moneySpentValueText.setText("₹0.00");
        if (topDestinationValueText != null) topDestinationValueText.setText("N/A");
        if (spendInsightsLineChart != null) {
            spendInsightsLineChart.clear();
            spendInsightsLineChart.invalidate();
        }
        updateMonthLabelsForChart();
    }

    private void updateMonthLabelsForChart() {
        Log.d(TAG, "updateMonthLabelsForChart CALLED.");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        if (graphLabelMarText != null) {
            graphLabelMarText.setText(monthFormat.format(cal.getTime()));
        }
        if (graphLabelAprText != null) {
            graphLabelAprText.setText("Daily Spend");
        }
    }


    private void fetchAndProcessSpendInsights() {
        if (userBookingsRef == null) {
            Log.w(TAG, "fetchAndProcessSpendInsights: userBookingsRef is NULL.");
            setDefaultInsights();
            return;
        }
        Log.d(TAG, "fetchAndProcessSpendInsights CALLED.");

        userBookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "fetchAndProcessSpendInsights: onDataChange: Data received, snapshot exists = " + dataSnapshot.exists());
                if (!isAdded() || getContext() == null) {
                    Log.w(TAG, "fetchAndProcessSpendInsights: onDataChange: Fragment not added or context null.");
                    return;
                }

                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                int currentMonth = calendar.get(Calendar.MONTH);
                int daysInCurrentMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

                Map<Integer, Double> dailySpending = new HashMap<>();
                for (int i = 1; i <= daysInCurrentMonth; i++) {
                    dailySpending.put(i, 0.0);
                }

                int totalTicketsBooked = 0;
                double totalMoneySpent = 0.0;
                Map<String, Integer> destinationFrequencies = new HashMap<>();
                SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                for (DataSnapshot ticketSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "fetchAndProcessSpendInsights: Processing ticket: " + ticketSnapshot.getKey());
                    String dateStr = ticketSnapshot.child("date").getValue(String.class);

                    // ##### CORRECTED LINE #####
                    // Changed Number.class to Float.class to prevent deserialization error
                    Float priceNum = ticketSnapshot.child("price").getValue(Float.class);

                    String destination = ticketSnapshot.child("destinationStop").getValue(String.class);

                    if (dateStr == null || priceNum == null) {
                        Log.w(TAG, "fetchAndProcessSpendInsights: Skipping ticket due to null date or price. Date: " + dateStr);
                        continue;
                    }

                    try {
                        Date bookingDate = firebaseDateFormat.parse(dateStr);
                        Calendar bookingCal = Calendar.getInstance();
                        if (bookingDate != null) {
                            bookingCal.setTime(bookingDate);
                        } else {
                            Log.w(TAG, "fetchAndProcessSpendInsights: Parsed bookingDate is null for dateStr: " + dateStr);
                            continue;
                        }

                        if (bookingCal.get(Calendar.YEAR) == currentYear && bookingCal.get(Calendar.MONTH) == currentMonth) {
                            totalTicketsBooked++;

                            double price = priceNum.doubleValue();
                            totalMoneySpent += price;

                            int dayOfMonth = bookingCal.get(Calendar.DAY_OF_MONTH);
                            dailySpending.put(dayOfMonth, dailySpending.getOrDefault(dayOfMonth, 0.0) + price);

                            if (destination != null && !destination.isEmpty()) {
                                destinationFrequencies.put(destination, destinationFrequencies.getOrDefault(destination, 0) + 1);
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "fetchAndProcessSpendInsights: Error parsing booking date string: '" + dateStr + "'", e);
                    }
                }
                Log.d(TAG, "fetchAndProcessSpendInsights: Finished processing tickets. Total booked: " + totalTicketsBooked);
                updateSpendInsightsUI(dailySpending, totalTicketsBooked, totalMoneySpent, destinationFrequencies, daysInCurrentMonth);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "fetchAndProcessSpendInsights: Firebase data fetch cancelled: ", databaseError.toException());
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Failed to load spend insights.", Toast.LENGTH_SHORT).show();
                setDefaultInsights();
            }
        });
    }

    private void updateSpendInsightsUI(Map<Integer, Double> dailySpending, int totalTickets, double totalSpent,
                                       Map<String, Integer> destFreq, int daysInMonth) {
        Log.d(TAG, "updateSpendInsightsUI CALLED. TotalTickets: " + totalTickets + ", TotalSpent: " + totalSpent);
        if (!isAdded() || ticketsBookedValueText == null || moneySpentValueText == null || topDestinationValueText == null) {
            Log.w(TAG, "updateSpendInsightsUI: Fragment not added or UI elements are null.");
            return;
        }

        ticketsBookedValueText.setText(String.valueOf(totalTickets));
        moneySpentValueText.setText(String.format(Locale.getDefault(), "₹%.2f", totalSpent));

        String topDestination = "N/A";
        if (!destFreq.isEmpty()) {
            topDestination = Collections.max(destFreq.entrySet(), Map.Entry.comparingByValue()).getKey();
        }
        Log.d(TAG, "updateSpendInsightsUI: Top destination: " + topDestination);
        topDestinationValueText.setText(topDestination);

        ArrayList<Entry> chartEntries = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            chartEntries.add(new Entry(day, dailySpending.getOrDefault(day, 0.0).floatValue()));
        }
        Log.d(TAG, "updateSpendInsightsUI: Chart entries count: " + chartEntries.size());

        updateSpendInsightsChart(chartEntries);
        updateMonthLabelsForChart();
    }

    private void setupEmptySpendInsightsChart() {
        Log.d(TAG, "setupEmptySpendInsightsChart CALLED.");
        if (spendInsightsLineChart == null || getContext() == null) {
            Log.w(TAG, "setupEmptySpendInsightsChart: Chart or context is null.");
            return;
        }

        spendInsightsLineChart.setViewPortOffsets(0f, 0f, 0f, 0f);
        spendInsightsLineChart.setBackgroundColor(Color.TRANSPARENT);
        spendInsightsLineChart.getDescription().setEnabled(false);
        spendInsightsLineChart.setTouchEnabled(false);
        spendInsightsLineChart.setDrawGridBackground(false);
        spendInsightsLineChart.setDragEnabled(false);
        spendInsightsLineChart.setScaleEnabled(false);
        spendInsightsLineChart.setPinchZoom(false);

        XAxis xAxis = spendInsightsLineChart.getXAxis();
        xAxis.setEnabled(false);

        YAxis yAxisLeft = spendInsightsLineChart.getAxisLeft();
        yAxisLeft.setEnabled(false);

        YAxis yAxisRight = spendInsightsLineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        spendInsightsLineChart.getLegend().setEnabled(false);
        spendInsightsLineChart.clear();
        spendInsightsLineChart.invalidate();
    }


    private void updateSpendInsightsChart(ArrayList<Entry> entries) {
        Log.d(TAG, "updateSpendInsightsChart CALLED with " + (entries != null ? entries.size() : "null") + " entries.");
        if (getContext() == null || spendInsightsLineChart == null) {
            Log.w(TAG, "updateSpendInsightsChart: Context or chart is null.");
            return;
        }

        if (entries == null || entries.isEmpty()) {
            Log.d(TAG, "updateSpendInsightsChart: Entries are null or empty, setting up empty chart.");
            setupEmptySpendInsightsChart();
            return;
        }

        LineDataSet lineDataSet = new LineDataSet(entries, "Daily Spend");
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setCubicIntensity(0.2f);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setColor(ContextCompat.getColor(getContext(), R.color.graph_line_color));
        lineDataSet.setLineWidth(3f);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawFilled(true);

        Drawable fillDrawable = ContextCompat.getDrawable(getContext(), R.drawable.graph_fill_gradient);
        if (fillDrawable != null) {
            lineDataSet.setFillDrawable(fillDrawable);
        } else {
            Log.w(TAG, "updateSpendInsightsChart: graph_fill_gradient drawable not found.");
            lineDataSet.setFillColor(ContextCompat.getColor(getContext(), R.color.graph_fill_color_alpha));
        }
        lineDataSet.setFillFormatter((dataSet, dataProvider) ->
                spendInsightsLineChart.getAxisLeft().getAxisMinimum());


        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSet);
        LineData lineData = new LineData(dataSets);

        spendInsightsLineChart.setData(lineData);
        spendInsightsLineChart.invalidate();
        spendInsightsLineChart.animateX(1000);
        Log.d(TAG, "updateSpendInsightsChart: Chart updated and animated.");
    }
}