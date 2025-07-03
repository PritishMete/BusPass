package com.pritish.smartbuss; // Your package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

// Assuming DailySummaryItem is a class in your package (com.pritish.smartbuss.DailySummaryItem)

public class DailyBreakdownAdapter extends RecyclerView.Adapter<DailyBreakdownAdapter.ViewHolder> {
    private final List<DailySummaryItem> dailySummaries;

    public DailyBreakdownAdapter(List<DailySummaryItem> dailySummaries) {
        this.dailySummaries = dailySummaries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_summary_for_week, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailySummaryItem item = dailySummaries.get(position);
        holder.tvDayNameInWeek.setText(item.dayDisplay);
        holder.tvDayTotalIncentiveInWeek.setText(String.format(Locale.getDefault(), "+₹%.2f", item.totalDailyIncentive));
    }

    @Override
    public int getItemCount() {
        return dailySummaries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNameInWeek, tvDayTotalIncentiveInWeek;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNameInWeek = itemView.findViewById(R.id.tv_day_name_in_week);
            tvDayTotalIncentiveInWeek = itemView.findViewById(R.id.tv_day_total_incentive_in_week);
        }
    }
}