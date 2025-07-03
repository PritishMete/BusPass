package com.pritish.smartbuss;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassbookAdapter extends RecyclerView.Adapter<PassbookAdapter.ViewHolder> {

    private static final String ADAPTER_TAG = "PassbookAdapter";
    private List<PassbookEntry> internalPassbookEntriesList;
    private Context context;

    public PassbookAdapter(Context context, List<PassbookEntry> initialEntries) {
        this.context = context;
        this.internalPassbookEntriesList = new ArrayList<>();
        if (initialEntries != null) {
            this.internalPassbookEntriesList.addAll(initialEntries);
        }
        Log.d(ADAPTER_TAG, "Constructor called. Initial internal list size: " + this.internalPassbookEntriesList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(ADAPTER_TAG, "onCreateViewHolder CALLED, viewType: " + viewType);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passbook_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (internalPassbookEntriesList == null || position < 0 || position >= internalPassbookEntriesList.size()) {
            Log.e(ADAPTER_TAG, "onBindViewHolder: Invalid position or null internal list. Position: " + position);
            return;
        }
        PassbookEntry entry = internalPassbookEntriesList.get(position);
        Log.d(ADAPTER_TAG, "onBindViewHolder: Position: " + position + ", Type: " + entry.getType() + ", Amt: " + entry.getAmount() + ", Remark: " + entry.getRemark());

        if (holder.tvTransactionTime != null) {
            holder.tvTransactionTime.setText(String.format("%s %s", entry.getDate(), entry.getTime()));
        } else { Log.e(ADAPTER_TAG, "tvTransactionTime is NULL for position " + position); }

        if (holder.tvTransactionRemark != null) {
            holder.tvTransactionRemark.setText(entry.getRemark());
        } else { Log.e(ADAPTER_TAG, "tvTransactionRemark is NULL for position " + position); }

        if (holder.tvTransactionBalance != null) {
            holder.tvTransactionBalance.setText(String.format(Locale.getDefault(), "₹%.2f", entry.getBalanceAfterTransaction()));
        } else { Log.e(ADAPTER_TAG, "tvTransactionBalance is NULL for position " + position); }


        // Standard Debit/Credit Handling for Type and Amount
        if ("DEBIT".equalsIgnoreCase(entry.getType())) {
            if (holder.tvTransactionType != null) {
                holder.tvTransactionType.setText("Debit");
                holder.tvTransactionType.setBackgroundResource(R.drawable.bg_debit_tag);
            }
            if (holder.tvTransactionAmount != null) {
                holder.tvTransactionAmount.setText(String.format(Locale.getDefault(), "- ₹%.2f", entry.getAmount()));
                if (context != null) holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.red));
            }
        } else if ("CREDIT".equalsIgnoreCase(entry.getType())) {
            if (holder.tvTransactionType != null) {
                holder.tvTransactionType.setText("Credit");
                holder.tvTransactionType.setBackgroundResource(R.drawable.bg_credit_tag);
            }
            if (holder.tvTransactionAmount != null) {
                holder.tvTransactionAmount.setText(String.format(Locale.getDefault(), "+ ₹%.2f", entry.getAmount()));
                if (context != null) holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, R.color.green));
            }
        } else { // Fallback for unknown types
            if (holder.tvTransactionType != null) holder.tvTransactionType.setText("N/A");
            if (holder.tvTransactionAmount != null) {
                holder.tvTransactionAmount.setText(String.format(Locale.getDefault(), "₹%.2f", entry.getAmount()));
                if (context != null) holder.tvTransactionAmount.setTextColor(ContextCompat.getColor(context, android.R.color.black)); // Default color
            }
        }

        // --- Handle additional booking details if available in PassbookEntry ---
        // ASSUMPTION: Your PassbookEntry.java now has methods like:
        // String getRefBookingId();
        // String getRefBookingRoute();
        // String getRefBookingStartStop();
        // String getRefBookingDestinationStop();
        // These methods should return null if the entry is not booking-related or data is absent.

        String refBookingId = entry.getRefBookingId(); // Replace with your actual method
        String refBookingRoute = entry.getRefBookingRoute(); // Replace with your actual method
        String refBookingStartStop = entry.getRefBookingStartStop(); // Replace with your actual method
        String refBookingDestinationStop = entry.getRefBookingDestinationStop(); // Replace with your actual method

        boolean hasBookingDetailsToShow = (refBookingId != null && !refBookingId.isEmpty()) ||
                (refBookingRoute != null && !refBookingRoute.isEmpty()) ||
                (refBookingStartStop != null && !refBookingStartStop.isEmpty()) ||
                (refBookingDestinationStop != null && !refBookingDestinationStop.isEmpty());

        if (hasBookingDetailsToShow) {
            if (holder.tvBookingDetailsLabel != null) {
                holder.tvBookingDetailsLabel.setVisibility(View.VISIBLE);
            }

            if (holder.tvRelatedBookingId != null) {
                if (refBookingId != null && !refBookingId.isEmpty()) {
                    holder.tvRelatedBookingId.setText("Booking ID: " + refBookingId);
                    holder.tvRelatedBookingId.setVisibility(View.VISIBLE);
                } else {
                    holder.tvRelatedBookingId.setVisibility(View.GONE);
                }
            }

            if (holder.tvBookingRoute != null) {
                if (refBookingRoute != null && !refBookingRoute.isEmpty()) {
                    holder.tvBookingRoute.setText("Route: " + refBookingRoute);
                    holder.tvBookingRoute.setVisibility(View.VISIBLE);
                } else {
                    holder.tvBookingRoute.setVisibility(View.GONE);
                }
            }

            if (holder.tvBookingStops != null) {
                if (refBookingStartStop != null && !refBookingStartStop.isEmpty() &&
                        refBookingDestinationStop != null && !refBookingDestinationStop.isEmpty()) {
                    holder.tvBookingStops.setText("From: " + refBookingStartStop + " To: " + refBookingDestinationStop);
                    holder.tvBookingStops.setVisibility(View.VISIBLE);
                } else if (refBookingStartStop != null && !refBookingStartStop.isEmpty()) { // Only start stop available
                    holder.tvBookingStops.setText("Boarding At: " + refBookingStartStop);
                    holder.tvBookingStops.setVisibility(View.VISIBLE);
                } else if (refBookingDestinationStop != null && !refBookingDestinationStop.isEmpty()) { // Only destination stop available
                    holder.tvBookingStops.setText("Alighting At: " + refBookingDestinationStop);
                    holder.tvBookingStops.setVisibility(View.VISIBLE);
                }
                else {
                    holder.tvBookingStops.setVisibility(View.GONE);
                }
            }
        } else {
            // Hide all booking-specific fields if no details are available
            if (holder.tvBookingDetailsLabel != null) holder.tvBookingDetailsLabel.setVisibility(View.GONE);
            if (holder.tvRelatedBookingId != null) holder.tvRelatedBookingId.setVisibility(View.GONE);
            if (holder.tvBookingRoute != null) holder.tvBookingRoute.setVisibility(View.GONE);
            if (holder.tvBookingStops != null) holder.tvBookingStops.setVisibility(View.GONE);
        }

        // Set color for the main transaction remark based on Debit/Credit type
        // This color styling is independent of the booking details section.
        if (holder.tvTransactionRemark != null && context != null) {
            if ("DEBIT".equalsIgnoreCase(entry.getType())) {
                holder.tvTransactionRemark.setTextColor(ContextCompat.getColor(context, R.color.red));
            } else if ("CREDIT".equalsIgnoreCase(entry.getType())) {
                holder.tvTransactionRemark.setTextColor(ContextCompat.getColor(context, R.color.green));
            } else {
                // Fallback color for remark if type is neither DEBIT nor CREDIT
                holder.tvTransactionRemark.setTextColor(ContextCompat.getColor(context, android.R.color.black)); // Or your app's default text color
            }
        }
        // --- END: Handle additional booking details ---

        Log.d(ADAPTER_TAG, "onBindViewHolder: Finished binding for position " + position);
    }

    @Override
    public int getItemCount() {
        int count = (internalPassbookEntriesList != null ? internalPassbookEntriesList.size() : 0);
        Log.d(ADAPTER_TAG, "getItemCount CALLED. Returning: " + count);
        return count;
    }

    public void updateEntries(List<PassbookEntry> newExternalEntries) {
        Log.d(ADAPTER_TAG, "updateEntries CALLED. New external entries count: " + (newExternalEntries != null ? newExternalEntries.size() : "null"));
        this.internalPassbookEntriesList.clear();
        if (newExternalEntries != null) {
            this.internalPassbookEntriesList.addAll(newExternalEntries);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance with large lists
        Log.d(ADAPTER_TAG, "updateEntries: notifyDataSetChanged called. Current internal list size: " + this.internalPassbookEntriesList.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTransactionType, tvTransactionAmount, tvTransactionAmountLabel,
                tvTransactionTime, tvTransactionTimeLabel,
                tvTransactionRemark, tvRemarkLabel,
                tvTransactionBalance, tvBalanceLabel;

        // TextViews for Booking Details (IDs should match item_passbook_entry.xml)
        TextView tvBookingDetailsLabel, tvRelatedBookingId, tvBookingRoute, tvBookingStops;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(ADAPTER_TAG, "ViewHolder constructor CALLED.");
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            tvTransactionAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvTransactionAmountLabel = itemView.findViewById(R.id.tvTransactionAmountLabel);
            tvTransactionTime = itemView.findViewById(R.id.tvTransactionTime);
            tvTransactionTimeLabel = itemView.findViewById(R.id.tvTransactionTimeLabel);
            tvTransactionRemark = itemView.findViewById(R.id.tvTransactionRemark);
            tvRemarkLabel = itemView.findViewById(R.id.tvRemarkLabel);
            tvTransactionBalance = itemView.findViewById(R.id.tvTransactionBalance);
            tvBalanceLabel = itemView.findViewById(R.id.tvBalanceLabel);

            // Initialize NEW TextViews
            tvBookingDetailsLabel = itemView.findViewById(R.id.tvBookingDetailsLabel);
            tvRelatedBookingId = itemView.findViewById(R.id.tvRelatedBookingId);
            tvBookingRoute = itemView.findViewById(R.id.tvBookingRoute);
            tvBookingStops = itemView.findViewById(R.id.tvBookingStops);

            // Optional: Log if any new views are null after findViewById
            if (tvBookingDetailsLabel == null) Log.e(ADAPTER_TAG, "ViewHolder: tvBookingDetailsLabel is NULL.");
            if (tvRelatedBookingId == null) Log.e(ADAPTER_TAG, "ViewHolder: tvRelatedBookingId is NULL.");
            if (tvBookingRoute == null) Log.e(ADAPTER_TAG, "ViewHolder: tvBookingRoute is NULL.");
            if (tvBookingStops == null) Log.e(ADAPTER_TAG, "ViewHolder: tvBookingStops is NULL.");

            Log.d(ADAPTER_TAG, "ViewHolder: Finished findViewById calls.");
        }
    }
}