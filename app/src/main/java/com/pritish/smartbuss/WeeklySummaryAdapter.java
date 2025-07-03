package com.pritish.smartbuss; // Your package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

// Assuming WeeklySummaryItem is a class in your package (com.pritish.smartbuss.WeeklySummaryItem)
// If it's an inner static class of conductor_incentive, you might not need a direct import
// or would use conductor_incentive.WeeklySummaryItem.

public class WeeklySummaryAdapter extends RecyclerView.Adapter<WeeklySummaryAdapter.ViewHolder> {
    private final List<WeeklySummaryItem> weeklySummaries;
    private final OnWeekClickListener listener;

    // Interface for click events
    public interface OnWeekClickListener {
        void onWeekClicked(WeeklySummaryItem item);
    }

    public WeeklySummaryAdapter(List<WeeklySummaryItem> weeklySummaries, OnWeekClickListener listener) {
        this.weeklySummaries = weeklySummaries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weekly_summary_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeeklySummaryItem item = weeklySummaries.get(position);
        holder.tvWeekRange.setText(item.weekRangeDisplay);
        holder.tvWeekTotalIncentive.setText(String.format(Locale.getDefault(), "+₹%.2f", item.totalWeeklyIncentive));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWeekClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return weeklySummaries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWeekRange, tvWeekTotalIncentive;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeekRange = itemView.findViewById(R.id.tv_week_range);
            tvWeekTotalIncentive = itemView.findViewById(R.id.tv_week_total_incentive);
        }
    }
}