package com.pritish.smartbuss;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class JourneyAdapter extends RecyclerView.Adapter<JourneyAdapter.JourneyViewHolder> {

    private List<Journey> journeyList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Journey journey, View view);
    }

    public JourneyAdapter(Context context, List<Journey> journeyList, OnItemClickListener listener) {
        this.context = context;
        this.journeyList = journeyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JourneyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.buslocation_item, parent, false);
        return new JourneyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JourneyViewHolder holder, int position) {
        Journey journey = journeyList.get(position);

        // Bind data to views
        holder.routeText.setText("Route: " + journey.getRouteNumber());
        holder.statusText.setText(journey.getStatus());
        holder.startLocationText.setText(journey.getStartLocation());
        holder.endLocationText.setText(journey.getEndLocation());
        holder.timeText.setText("Time: " + journey.getTime());
        holder.priceText.setText(journey.getPrice());

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(journey, holder.itemView);
            }

            // Start bus_location activity with all journey data
            Intent intent = new Intent(context, bus_location.class);
            intent.putExtra("routeNumber", journey.getRouteNumber());
            intent.putExtra("status", journey.getStatus());
            intent.putExtra("startLocation", journey.getStartLocation());
            intent.putExtra("endLocation", journey.getEndLocation());
            intent.putExtra("time", journey.getTime());
            intent.putExtra("price", journey.getPrice());

            // Add smooth transition animation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation((Activity) context,
                                holder.itemView, "journey_card");
                context.startActivity(intent, options.toBundle());
            } else {
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return journeyList.size();
    }

    public void updateJourneys(List<Journey> newJourneys) {
        journeyList.clear();
        journeyList.addAll(newJourneys);
        notifyDataSetChanged();
    }

    static class JourneyViewHolder extends RecyclerView.ViewHolder {
        TextView routeText, statusText, startLocationText,
                endLocationText, timeText, priceText;

        public JourneyViewHolder(@NonNull View itemView) {
            super(itemView);
            routeText = itemView.findViewById(R.id.tv_route);
            statusText = itemView.findViewById(R.id.tv_status);
            startLocationText = itemView.findViewById(R.id.tv_start_location);
            endLocationText = itemView.findViewById(R.id.tv_end_location);
            timeText = itemView.findViewById(R.id.tv_time);
            priceText = itemView.findViewById(R.id.tv_price);
        }
    }
}