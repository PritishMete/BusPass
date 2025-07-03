package com.pritish.smartbuss;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.ViewHolder> {
    private final List<DayInfo> dayInfoList;
    private final OnDayClickListener listener;
    private int selectedPosition = -1;

    public interface OnDayClickListener { void onDayClicked(int position, DayInfo dayInfo); }

    public DayAdapter(List<DayInfo> dayInfoList, OnDayClickListener listener) {
        this.dayInfoList = dayInfoList;
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_button, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayInfo dayInfo = dayInfoList.get(position);
        holder.tvDayNumber.setText(dayInfo.dayNumber);
        holder.tvMonthAbbr.setText(dayInfo.monthAbbr);
        holder.tvDayNameAbbr.setText(dayInfo.dayNameAbbr);

        holder.itemView.setSelected(selectedPosition == position);

        if (selectedPosition == position) {
            holder.tvDayNumber.setTextColor(Color.WHITE);
            holder.tvMonthAbbr.setTextColor(Color.WHITE);
            holder.tvDayNameAbbr.setTextColor(Color.WHITE);
        } else {
            int unselectedColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black);
            holder.tvDayNumber.setTextColor(unselectedColor);
            holder.tvMonthAbbr.setTextColor(unselectedColor);
            holder.tvDayNameAbbr.setTextColor(unselectedColor);
        }
    }

    @Override public int getItemCount() { return dayInfoList.size(); }

    public void setSelectedPosition(int position) {
        if (position < 0 || position >= dayInfoList.size()) return;
        int previousSelected = selectedPosition;
        selectedPosition = position;
        if (previousSelected != -1) notifyItemChanged(previousSelected);
        notifyItemChanged(selectedPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber, tvMonthAbbr, tvDayNameAbbr;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
            tvMonthAbbr = itemView.findViewById(R.id.tv_month_abbr);
            tvDayNameAbbr = itemView.findViewById(R.id.tv_day_name_abbr);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        setSelectedPosition(position);
                        listener.onDayClicked(position, dayInfoList.get(position));
                    }
                }
            });
        }
    }
}