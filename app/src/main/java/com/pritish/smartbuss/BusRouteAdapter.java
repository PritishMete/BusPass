package com.pritish.smartbuss;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BusRouteAdapter extends RecyclerView.Adapter<BusRouteAdapter.ViewHolder> {

    private List<String> busRoutes;
    private OnRouteClickListener routeClickListener;

    // Constructor to initialize the busRoutes and listener
    public BusRouteAdapter(List<String> busRoutes, OnRouteClickListener routeClickListener) {
        this.busRoutes = busRoutes;
        this.routeClickListener = routeClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the view for each route item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bus_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String route = busRoutes.get(position);
        holder.routeTextView.setText(route);

        // Set a click listener for the "Select Route" button
        holder.selectRouteButton.setOnClickListener(v -> {
            Log.d("BusRouteAdapter", "Selected Route: " + route); // Log the selected route
            routeClickListener.onRouteClick(route); // Notify the listener
        });
    }

    @Override
    public int getItemCount() {
        return busRoutes.size();
    }

    // ViewHolder to hold the views for each item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView routeTextView;
        Button selectRouteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            routeTextView = itemView.findViewById(R.id.routeTextView);
            selectRouteButton = itemView.findViewById(R.id.selectRouteButton); // Initialize the button
        }
    }

    // Interface to handle route item click
    public interface OnRouteClickListener {
        void onRouteClick(String routeName);
    }
}
