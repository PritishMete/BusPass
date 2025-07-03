package com.pritish.smartbuss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder> {

    private List<Passenger> passengerList;

    public PassengerAdapter(List<Passenger> passengerList) {
        this.passengerList = passengerList;
    }

    @NonNull
    @Override
    public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_passenger, parent, false);
        return new PassengerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
        Passenger passenger = passengerList.get(position);
        holder.priceTextView.setText("Price: " + passenger.getPrice());
        holder.routeTextView.setText("Route: " + passenger.getRoute());
        holder.transactionIDTextView.setText("Transaction ID: " + passenger.getTransactionId());
    }

    @Override
    public int getItemCount() {
        return passengerList.size();
    }

    public static class PassengerViewHolder extends RecyclerView.ViewHolder {

        TextView priceTextView;
        TextView routeTextView;
        TextView transactionIDTextView;

        public PassengerViewHolder(View itemView) {
            super(itemView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            routeTextView = itemView.findViewById(R.id.routeTextView);
            transactionIDTextView = itemView.findViewById(R.id.transactionIdTextView);
        }
    }
}
