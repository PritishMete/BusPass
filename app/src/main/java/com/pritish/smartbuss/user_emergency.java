// Enhanced user_emergency.java with notification dot and broadcast functionality
package com.pritish.smartbuss;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class user_emergency extends DialogFragment {

    private static final String ARG_USER_PHONE = "userPhone";
    private static final String CHANNEL_ID = "emergency_channel";
    private static final String ACTION_EMERGENCY_UPDATE = "com.pritish.smartbuss.EMERGENCY_UPDATE";

    private RecyclerView recyclerView;
    private EmergencyAdapter adapter;
    private List<EmergencyMessage> emergencyMessages;
    private DatabaseReference emergencyRef;
    private String userPhone;
    private TextView noMessagesText;
    private int lastMessageCount = 0;

    public static user_emergency newInstance(String userPhone) {
        user_emergency fragment = new user_emergency();
        Bundle args = new Bundle();
        args.putString(ARG_USER_PHONE, userPhone);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use built-in full screen dialog style or create custom one
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        if (getArguments() != null) {
            userPhone = getArguments().getString(ARG_USER_PHONE);
        }

        emergencyMessages = new ArrayList<>();
        emergencyRef = FirebaseDatabase.getInstance().getReference("emergency");

        // Create notification channel
        createNotificationChannel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_emergency_messages, container, false);

        setupViews(view);
        loadEmergencyMessages();

        return view;
    }

    private void setupViews(View view) {
        TextView titleText = view.findViewById(R.id.emergency_title);
        titleText.setText("🚨 Emergency Messages");

        Button closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());

        Button sendEmergencyButton = view.findViewById(R.id.send_emergency_button);
        sendEmergencyButton.setOnClickListener(v -> showSendEmergencyDialog());

        recyclerView = view.findViewById(R.id.emergency_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Add no messages text view
        noMessagesText = view.findViewById(R.id.no_messages_text);
        if (noMessagesText == null) {
            // Create programmatically if not in layout
            noMessagesText = new TextView(getContext());
            noMessagesText.setText("No emergency messages at the moment");
            noMessagesText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            noMessagesText.setPadding(32, 32, 32, 32);
            noMessagesText.setVisibility(View.GONE);
            ((ViewGroup) view).addView(noMessagesText);
        }

        adapter = new EmergencyAdapter(emergencyMessages);
        recyclerView.setAdapter(adapter);
    }

    private void loadEmergencyMessages() {
        emergencyRef.orderByChild("status").equalTo("approved")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        emergencyMessages.clear();

                        for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                            EmergencyMessage message = messageSnapshot.getValue(EmergencyMessage.class);
                            if (message != null) {
                                message.setId(messageSnapshot.getKey());
                                emergencyMessages.add(message);
                            }
                        }

                        // Sort by timestamp (newest first)
                        Collections.sort(emergencyMessages, (m1, m2) ->
                                Long.compare(m2.getTimestamp(), m1.getTimestamp()));

                        // Check for new messages
                        if (emergencyMessages.size() > lastMessageCount && lastMessageCount > 0) {
                            showNotification(emergencyMessages.get(0)); // Show notification for newest message
                            sendBroadcastUpdate(emergencyMessages.size());
                        }
                        lastMessageCount = emergencyMessages.size();

                        // Update UI
                        updateUI();
                        adapter.notifyDataSetChanged();

                        Log.d("EmergencyDialog", "Loaded " + emergencyMessages.size() + " messages");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("EmergencyDialog", "Failed to load messages: " + error.getMessage());
                        Toast.makeText(getContext(), "Failed to load emergency messages", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI() {
        if (emergencyMessages.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noMessagesText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noMessagesText.setVisibility(View.GONE);
        }
    }

    private void showSendEmergencyDialog() {
        user_send_emergency_dialog sendDialog = user_send_emergency_dialog.newInstance(userPhone);
        sendDialog.show(getParentFragmentManager(), "SendEmergencyDialog");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Emergency Notifications";
            String description = "Channel for emergency message notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getContext().getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(EmergencyMessage message) {
        if (getContext() == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_emergency) // Make sure you have this icon
                .setContentTitle("🚨 Emergency Alert")
                .setContentText(message.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message.getContent()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void sendBroadcastUpdate(int messageCount) {
        if (getContext() == null) return;

        Intent intent = new Intent(ACTION_EMERGENCY_UPDATE);
        intent.putExtra("messageCount", messageCount);
        intent.putExtra("hasNewMessages", true);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    // Inner class for Emergency Message data model
    public static class EmergencyMessage {
        private String id;
        private String title;
        private String content;
        private String status;
        private String sender;
        private String senderType;
        private long timestamp;
        private String dateCreated;
        private String severity; // New field for message severity

        public EmergencyMessage() {
            // Default constructor required for Firebase
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }

        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public String getDateCreated() { return dateCreated; }
        public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }

    // Enhanced RecyclerView Adapter with better styling
    private static class EmergencyAdapter extends RecyclerView.Adapter<EmergencyAdapter.ViewHolder> {
        private List<EmergencyMessage> messages;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public EmergencyAdapter(List<EmergencyMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_emergency_message, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EmergencyMessage message = messages.get(position);

            holder.titleText.setText(message.getTitle());
            holder.contentText.setText(message.getContent());

            // Format timestamp
            String dateStr = message.getTimestamp() > 0 ?
                    dateFormat.format(new Date(message.getTimestamp())) : "Unknown date";
            holder.dateText.setText(dateStr);

            // Show sender info if available
            if (message.getSender() != null && message.getSenderType() != null) {
                String senderInfo = "From: " + message.getSender() + " (" + message.getSenderType() + ")";
                holder.senderText.setText(senderInfo);
                holder.senderText.setVisibility(View.VISIBLE);
            } else {
                holder.senderText.setVisibility(View.GONE);
            }

            // Set severity indicator
            if (message.getSeverity() != null) {
                switch (message.getSeverity().toLowerCase()) {
                    case "high":
                        holder.severityIndicator.setBackgroundColor(0xFFFF0000); // Red
                        break;
                    case "medium":
                        holder.severityIndicator.setBackgroundColor(0xFFFF9800); // Orange
                        break;
                    case "low":
                        holder.severityIndicator.setBackgroundColor(0xFFFFEB3B); // Yellow
                        break;
                    default:
                        holder.severityIndicator.setBackgroundColor(0xFF9E9E9E); // Gray
                        break;
                }
                holder.severityIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.severityIndicator.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, contentText, dateText, senderText;
            View severityIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.message_title);
                contentText = itemView.findViewById(R.id.message_content);
                dateText = itemView.findViewById(R.id.message_date);
                senderText = itemView.findViewById(R.id.message_sender);
                severityIndicator = itemView.findViewById(R.id.severity_indicator);
            }
        }
    }
}