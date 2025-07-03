package com.pritish.smartbuss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList; // Added for creating new ArrayLists
import java.util.List;
import java.util.Locale; // Added for toLowerCase() with locale

public class NearestStopsAdapter extends RecyclerView.Adapter<NearestStopsAdapter.NearestStopsViewHolder> {
    private List<NearestStopModel> nearestStops; // This list will hold the filtered data to be displayed
    private List<NearestStopModel> nearestStopsFull; // This list will hold the original, complete data
    private OnStopSelectedListener listener;

    public interface OnStopSelectedListener {
        void onStopSelected(String stopName);
    }

    public NearestStopsAdapter(List<NearestStopModel> nearestStopsList, OnStopSelectedListener listener) {
        // Initialize with copies of the provided list
        this.nearestStops = new ArrayList<>(nearestStopsList);
        this.nearestStopsFull = new ArrayList<>(nearestStopsList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NearestStopsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearest_stop, parent, false);
        return new NearestStopsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NearestStopsViewHolder holder, int position) {
        NearestStopModel stop = nearestStops.get(position);
        holder.bind(stop);
    }

    @Override
    public int getItemCount() {
        return nearestStops.size();
    }

    class NearestStopsViewHolder extends RecyclerView.ViewHolder {
        TextView stopNameTextView;
        TextView stopDistanceTextView;

        public NearestStopsViewHolder(@NonNull View itemView) {
            super(itemView);
            stopNameTextView = itemView.findViewById(R.id.stopNameTextView);
            stopDistanceTextView = itemView.findViewById(R.id.stopDistanceTextView);
        }

        public void bind(NearestStopModel stop) {
            stopNameTextView.setText(stop.getStopName());
            // Assuming getDistance() returns distance in meters
            stopDistanceTextView.setText(String.format(Locale.getDefault(), "%.2f km away", stop.getDistance() / 1000.0));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStopSelected(stop.getStopName());
                }
            });
        }
    }

    // Method to filter the list based on a search query
    public void filter(String query) {
        String searchQuery = query.toLowerCase(Locale.getDefault()).trim();
        nearestStops.clear(); // Clear the current list of displayed stops

        if (searchQuery.isEmpty()) {
            // If the query is empty, show all original stops
            nearestStops.addAll(nearestStopsFull);
        } else {
            // Otherwise, filter from the full list
            for (NearestStopModel stop : nearestStopsFull) {
                if (stop.getStopName().toLowerCase(Locale.getDefault()).contains(searchQuery)) {
                    nearestStops.add(stop);
                }
            }
        }
        notifyDataSetChanged(); // Notify the adapter that the data has changed
    }
}