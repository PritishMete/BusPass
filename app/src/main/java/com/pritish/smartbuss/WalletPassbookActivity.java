package com.pritish.smartbuss;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.RadioGroup;
import android.widget.TextView;
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

// Removed NumberFormat as we are simplifying parsing
import java.text.ParseException; // Still needed for PassbookEntry if it uses it, though less likely now for price
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletPassbookActivity extends AppCompatActivity {

    private static final String TAG = "WalletPassbook"; // Ensure this TAG is used for filtering Logcat
    private static final String USER_DETAILS_PREFS = "smartbus_pref";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";

    private RecyclerView rvPassbookEntries;
    private PassbookAdapter passbookAdapter;
    private List<PassbookEntry> allEntries;
    private List<PassbookEntry> filteredEntries;
    private RadioGroup rgTransactionFilter;
    private TextView tvDataAvailableLabel;

    private DatabaseReference bookingsRef;
    private DatabaseReference rechargesRef;
    private DatabaseReference userWalletRef;
    private String currentUserPhone;
    private double currentWalletBalance = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallet_passbook);
        Log.d(TAG, "onCreate: Activity started.");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Passbook");
        }

        SharedPreferences prefs = getSharedPreferences(USER_DETAILS_PREFS, MODE_PRIVATE);
        currentUserPhone = prefs.getString(KEY_LOGGED_IN_PHONE, null);
        Log.d(TAG, "onCreate: currentUserPhone: " + currentUserPhone);

        if (currentUserPhone == null || currentUserPhone.isEmpty()) {
            Log.e(TAG, "onCreate: User not logged in. Finishing activity.");
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvDataAvailableLabel = findViewById(R.id.tvDataAvailableLabel);
        // Example: You could set text like:
        // SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        // tvDataAvailableLabel.setText("Transactions since " + sdf.format(new Date(someStartDateMillis)));
        // For now, using the static text from XML or leaving it as is.

        rvPassbookEntries = findViewById(R.id.rvPassbookEntries);
        rgTransactionFilter = findViewById(R.id.rgTransactionFilter);

        if (rvPassbookEntries == null || rgTransactionFilter == null) {
            Log.e(TAG, "onCreate: Critical UI elements (RecyclerView or RadioGroup) not found. Check layout file.");
            Toast.makeText(this, "Error initializing page.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allEntries = new ArrayList<>();
        filteredEntries = new ArrayList<>();
        passbookAdapter = new PassbookAdapter(this, filteredEntries);

        rvPassbookEntries.setLayoutManager(new LinearLayoutManager(this));
        rvPassbookEntries.setAdapter(passbookAdapter);

        Log.d(TAG, "onCreate: Initializing Firebase references for user: " + currentUserPhone);
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings").child(currentUserPhone);
        rechargesRef = FirebaseDatabase.getInstance().getReference("UserRecharges").child(currentUserPhone);
        userWalletRef = FirebaseDatabase.getInstance().getReference("Traveler").child(currentUserPhone).child("walletBalance");

        fetchCurrentWalletBalanceAndTransactions();

        rgTransactionFilter.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "RadioGroup: Filter changed. CheckedId: " + checkedId);
            filterEntries(checkedId);
        });
        Log.d(TAG, "onCreate: Setup complete.");
    }

    private void fetchCurrentWalletBalanceAndTransactions() {
        Log.d(TAG, "fetchCurrentWalletBalanceAndTransactions: Fetching current wallet balance...");
        userWalletRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue(Double.class) != null) {
                    currentWalletBalance = snapshot.getValue(Double.class);
                    Log.d(TAG, "fetchCurrentWalletBalanceAndTransactions: Current wallet balance fetched: " + currentWalletBalance);
                } else {
                    Log.w(TAG, "fetchCurrentWalletBalanceAndTransactions: Wallet balance snapshot doesn't exist or is null, using default 0.0. Snapshot exists: " + snapshot.exists());
                    currentWalletBalance = 0.0; // Default if not found
                }
                fetchTransactions(); // Proceed to fetch transactions
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "fetchCurrentWalletBalanceAndTransactions: Failed to fetch current wallet balance: " + error.getMessage());
                Toast.makeText(WalletPassbookActivity.this, "Failed to load current balance.", Toast.LENGTH_SHORT).show();
                currentWalletBalance = 0.0; // Default on error
                fetchTransactions(); // Still attempt to fetch transactions
            }
        });
    }

    private void fetchTransactions() {
        Log.d(TAG, "fetchTransactions: Fetching booking (debit) transactions...");
        allEntries.clear(); // Clear before fetching new set

        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "fetchTransactions (Bookings): onDataChange called. Snapshot exists: " + dataSnapshot.exists() + ", Children count: " + dataSnapshot.getChildrenCount());
                for (DataSnapshot bookingSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "fetchTransactions (Bookings): Processing booking node: " + bookingSnapshot.getKey());
                    Booking booking = bookingSnapshot.getValue(Booking.class);
                    if (booking != null && booking.getDate() != null && booking.getTime() != null && booking.getTransactionId() != null) {
                        try {
                            // No need to parse since price is already float
                            double amount = booking.getPrice(); // Directly get the float value
                            Log.d(TAG, "fetchTransactions (Bookings): Parsed booking " + booking.getTransactionId() + ", Amount: " + amount);

                            allEntries.add(new PassbookEntry(
                                    booking.getTransactionId(),
                                    "DEBIT",
                                    amount,
                                    booking.getDate(),
                                    booking.getTime(),
                                    "Tkt Booking"
                            ));
                        } catch (Exception e) {
                            Log.e(TAG, "fetchTransactions (Bookings): Unexpected error processing booking ID " + booking.getTransactionId() + ": " + e.getMessage());
                        }

                    } else {
                        String tid = (booking != null && booking.getTransactionId() != null) ? booking.getTransactionId() : (bookingSnapshot.getKey() != null ? bookingSnapshot.getKey() : "UnknownKey");
                        Log.w(TAG, "fetchTransactions (Bookings): Skipping incomplete booking data for ID/Key: " + tid +
                                ". Date: " + (booking != null ? booking.getDate() : "null") +
                                ", Time: " + (booking != null ? booking.getTime() : "null"));
                    }
                }
                Log.d(TAG, "fetchTransactions (Bookings): Finished processing bookings. Debit entries processed: " + countEntriesOfType("DEBIT"));
                fetchRecharges(); // Now fetch credits
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "fetchTransactions (Bookings): Failed to fetch bookings: " + databaseError.getMessage());
                fetchRecharges(); // Still attempt to fetch recharges even if bookings fail
            }
        });
    }

    private void fetchRecharges() {
        Log.d(TAG, "fetchRecharges: Fetching recharge (credit) transactions...");
        rechargesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "fetchRecharges: onDataChange called. Snapshot exists: " + dataSnapshot.exists() + ", Children count: " + dataSnapshot.getChildrenCount());
                for (DataSnapshot rechargeSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "fetchRecharges: Processing recharge node: " + rechargeSnapshot.getKey());
                    String id = rechargeSnapshot.getKey(); // This is the unique key like "pay_Qa1..." or "TXN_..."
                    Double amount = rechargeSnapshot.child("amount").getValue(Double.class);
                    String date = rechargeSnapshot.child("date").getValue(String.class);
                    String time = rechargeSnapshot.child("time").getValue(String.class);
                    String remark = rechargeSnapshot.child("remark").getValue(String.class);
                    // String transactionIdFromData = rechargeSnapshot.child("transactionId").getValue(String.class); // This should be same as 'id'

                    if (amount != null && date != null && time != null && id != null) {
                        Log.d(TAG, "fetchRecharges: Adding CREDIT entry: ID=" + id + ", Amt=" + amount + ", Date=" + date + ", Time=" + time + ", Remark=" + remark);
                        allEntries.add(new PassbookEntry(id, "CREDIT", amount, date, time, remark != null ? remark : "Wallet Recharge"));
                    } else {
                        Log.w(TAG, "fetchRecharges: Skipped incomplete recharge data for key: " + id +
                                " Amount: " + amount + ", Date: " + date + ", Time: " + time);
                    }
                }
                Log.d(TAG, "fetchRecharges: Finished processing recharges. Total entries combined so far: " + allEntries.size());
                processAndDisplayEntries();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "fetchRecharges: Failed to fetch recharges: " + databaseError.getMessage());
                processAndDisplayEntries(); // Process whatever entries we have (might just be debits if recharges failed)
            }
        });
    }

    private void processAndDisplayEntries() {
        Log.d(TAG, "processAndDisplayEntries CALLED. Current allEntries count: " + allEntries.size());
        if (allEntries.isEmpty()) {
            Log.d(TAG, "processAndDisplayEntries: No transactions found to display.");
            Toast.makeText(this, "No transactions found.", Toast.LENGTH_SHORT).show();
            filteredEntries.clear(); // Ensure adapter list is also clear
            if(passbookAdapter != null) passbookAdapter.notifyDataSetChanged();
            return;
        }

        // Sort entries by timestamp (newest first)
        Collections.sort(allEntries, new PassbookEntry.EntryComparator());
        Log.d(TAG, "processAndDisplayEntries: Entries sorted. First entry (newest) remark (if any): " + allEntries.get(0).getRemark() + ", Amount: " + allEntries.get(0).getAmount());

        // Calculate running balance (newest to oldest)
        // The balanceAfterTransaction for the newest entry should be the currentWalletBalance.
        // Then work backwards.
        double runningBalance = this.currentWalletBalance;
        Log.d(TAG, "processAndDisplayEntries: Starting running balance calculation with currentWalletBalance = " + runningBalance);

        for (PassbookEntry entry : allEntries) { // Iterating newest to oldest because of sort order
            entry.setBalanceAfterTransaction(runningBalance);
            Log.d(TAG, "processAndDisplayEntries: Entry ID: " + entry.getTransactionId() + ", Type: " + entry.getType() + ", Amount: " + entry.getAmount() + ", Set BalanceAfter: " + runningBalance);
            if ("DEBIT".equalsIgnoreCase(entry.getType())) {
                runningBalance += entry.getAmount(); // To get balance *before* this debit, add amount back
            } else if ("CREDIT".equalsIgnoreCase(entry.getType())) {
                runningBalance -= entry.getAmount(); // To get balance *before* this credit, subtract amount
            }
        }
        Log.d(TAG, "processAndDisplayEntries: Finished calculating running balances. Balance before oldest transaction (calculated): " + runningBalance);

        // Now filter based on the initially selected radio button (or current selection)
        if (rgTransactionFilter != null) {
            filterEntries(rgTransactionFilter.getCheckedRadioButtonId());
        } else {
            Log.e(TAG, "processAndDisplayEntries: rgTransactionFilter is null, cannot apply filter. Displaying all.");
            filteredEntries.clear();
            filteredEntries.addAll(allEntries);
            if(passbookAdapter != null) passbookAdapter.notifyDataSetChanged();
        }
    }

    private void filterEntries(int checkedId) {
        Log.d(TAG, "filterEntries CALLED. CheckedId: " + checkedId + ". Current allEntries count: " + allEntries.size());
        filteredEntries.clear();
        String filterDescription = "transactions";

        if (checkedId == R.id.rbAll || checkedId == 0) { // checkedId can be 0 if nothing is checked yet
            Log.d(TAG, "filterEntries: Filtering for ALL.");
            filteredEntries.addAll(allEntries);
            filterDescription = "all";
        } else if (checkedId == R.id.rbCredit) {
            Log.d(TAG, "filterEntries: Filtering for CREDIT.");
            for (PassbookEntry entry : allEntries) {
                if ("CREDIT".equalsIgnoreCase(entry.getType())) {
                    filteredEntries.add(entry);
                }
            }
            filterDescription = "credit";
        } else if (checkedId == R.id.rbDebit) {
            Log.d(TAG, "filterEntries: Filtering for DEBIT.");
            for (PassbookEntry entry : allEntries) {
                if ("DEBIT".equalsIgnoreCase(entry.getType())) {
                    filteredEntries.add(entry);
                }
            }
            filterDescription = "debit";
        } else {
            Log.w(TAG, "filterEntries: Unknown checkedId: " + checkedId + ". Displaying all as fallback.");
            filteredEntries.addAll(allEntries); // Fallback to all
            filterDescription = "all (unknown filter)";
        }

        Log.d(TAG, "filterEntries: Filtered list size: " + filteredEntries.size() + " for " + filterDescription);
        if (passbookAdapter != null) {
            passbookAdapter.updateEntries(filteredEntries); // This calls notifyDataSetChanged()
        } else {
            Log.e(TAG, "filterEntries: passbookAdapter is null!");
        }

        if (filteredEntries.isEmpty() && !allEntries.isEmpty() && (checkedId == R.id.rbCredit || checkedId == R.id.rbDebit)) {
            Toast.makeText(this, "No " + filterDescription + " transactions found.", Toast.LENGTH_SHORT).show();
        }
    }

    private int countEntriesOfType(String type) {
        int count = 0;
        for (PassbookEntry entry : allEntries) {
            if (type.equalsIgnoreCase(entry.getType())) {
                count++;
            }
        }
        return count;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "onOptionsItemSelected: Up button pressed, finishing activity.");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopped.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed.");
    }
}