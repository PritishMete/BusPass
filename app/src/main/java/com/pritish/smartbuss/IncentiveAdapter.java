package com.pritish.smartbuss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

// IncentiveAdapter.java
public class IncentiveAdapter extends RecyclerView.Adapter<IncentiveAdapter.ViewHolder> {
    private final List<Incentive> incentives;

    public IncentiveAdapter(List<Incentive> incentives) {
        this.incentives = incentives;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incentive, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Incentive incentive = incentives.get(position);
        holder.tvTime.setText(incentive.getTime());
        holder.tvRoute.setText(incentive.getRoute());
        holder.tvAmount.setText(String.format(Locale.getDefault(),
                "+₹%.2f", incentive.getAmount()));
    }

    @Override
    public int getItemCount() {
        return incentives.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvRoute, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}