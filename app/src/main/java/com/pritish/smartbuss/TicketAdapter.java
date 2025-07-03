package com.pritish.smartbuss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {
    private final List<conductor_incentive.TicketItem> ticketList;
    public TicketAdapter(List<conductor_incentive.TicketItem> ticketList) { this.ticketList = ticketList; }

    @NonNull @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket_card, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        conductor_incentive.TicketItem ticket = ticketList.get(position);

        holder.routeTextView.setText(String.format("%s to %s", ticket.getFrom(), ticket.getTo()));
        holder.fareTextView.setText(String.format(Locale.getDefault(), "₹%.2f", ticket.getFare()));
        holder.scannedAtTextView.setText(ticket.getScannedAt());
        holder.personCountTextView.setText(
                ticket.getPersonCount() == 1 ?
                        "1 Person" :
                        String.format(Locale.getDefault(), "%d Persons", ticket.getPersonCount()));
        holder.busNumberTextView.setText(String.format("Bus: %s", ticket.getBusNumber()));
        holder.transactionIdTextView.setText(String.format("ID: %s", ticket.getTransactionId()));
    }

    @Override public int getItemCount() { return ticketList.size(); }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView routeTextView, fareTextView, scannedAtTextView,
                personCountTextView, busNumberTextView, transactionIdTextView;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            routeTextView = itemView.findViewById(R.id.routeTextView);
            fareTextView = itemView.findViewById(R.id.fareTextView);
            scannedAtTextView = itemView.findViewById(R.id.scannedAtTextView);
            personCountTextView = itemView.findViewById(R.id.personCountTextView);
            busNumberTextView = itemView.findViewById(R.id.busNumberTextView);
            transactionIdTextView = itemView.findViewById(R.id.transactionIdTextView);
        }
    }
}