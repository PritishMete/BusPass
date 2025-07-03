package com.pritish.smartbuss;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.Button; // Button is removed
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity; // For accessing SupportActionBar
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper; // Added
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
// import java.util.Collections; // Sorting is still good
// import java.util.Comparator; // Sorting is still good
// import java.util.Date; // Not directly used here anymore for saving
import java.util.List;
// import java.util.Locale; // Not directly used here anymore for saving
// import java.text.SimpleDateFormat; // Not directly used here anymore for saving


public class user_notification extends Fragment {

    private static final String NOTIFICATION_PREFS = "NotificationPrefs"; // From NotificationHelper
    private static final String NOTIFICATION_LIST_KEY = "notification_list"; // From NotificationHelper
    private static final String TAG = "UserNotificationFrag"; // Updated TAG

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    // private Button clearButton; // Removed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_notification, container, false);


        final String mainuserPhone = (getArguments() != null) ? getArguments().getString("mainuserPhone") : null;
        if (mainuserPhone != null) {

            Log.d("user_notification", "user phone number is: "+mainuserPhone);
        }
        notificationRecyclerView = view.findViewById(R.id.recyclerView_notifications);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyView = view.findViewById(R.id.emptyView);
        // clearButton = view.findViewById(R.id.clearNotificationsButton); // Reference removed

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        // Pass the list to the adapter constructor
        notificationAdapter = new NotificationAdapter(notificationList);
        notificationRecyclerView.setAdapter(notificationAdapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Clear button logic removed
        // if (clearButton != null) {
        //     clearButton.setOnClickListener(v -> clearAllNotifications());
        // }

        // Setup ItemTouchHelper for swipe-to-dismiss
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(notificationRecyclerView);

        loadNotifications();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof user_menu) {
            ((user_menu) getActivity()).setNavigationDrawerEnabled(false); // Hide drawer icon
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                // You can set a specific title for this fragment
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Notifications");
            }
        }
        loadNotifications(); // Refresh list
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof user_menu) {
            ((user_menu) getActivity()).setNavigationDrawerEnabled(true); // Restore drawer icon
            // Optionally reset title if user_menu doesn't do it per fragment
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name); // Or your default
            }
        }
    }

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false; // Not using drag & drop
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && position < notificationList.size()) {
                Notification removedNotification = notificationList.get(position);

                // 1. Delete from SharedPreferences using NotificationHelper
                NotificationHelper.deleteSpecificNotification(requireContext(),
                        removedNotification.getMessage(), removedNotification.getDateTime());

                // 2. Remove from the local list
                notificationList.remove(position);

                // 3. Notify the adapter
                notificationAdapter.notifyItemRemoved(position);
                // To avoid issues with positions if multiple items are swiped quickly before UI updates:
                // notificationAdapter.notifyDataSetChanged(); // Or more specific notifyItemRangeChanged

                Toast.makeText(getContext(), "Notification removed", Toast.LENGTH_SHORT).show();
                updateEmptyView();
            }
        }
    };

    private void loadNotifications() {
        // Use NotificationHelper to get notifications to ensure consistency
        // if NotificationHelper's getNotifications method is static and public.
        // For now, assuming the direct SharedPreferences access is what you have.
        if (getContext() == null) return; // Prevent crash if context is null

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        String existingNotificationsJson = sharedPreferences.getString(NOTIFICATION_LIST_KEY, "[]");
        notificationList.clear(); // Clear before loading

        try {
            JSONArray notificationArray = new JSONArray(existingNotificationsJson);
            for (int i = 0; i < notificationArray.length(); i++) {
                JSONObject notificationObj = notificationArray.getJSONObject(i);
                String message = notificationObj.getString("message");
                String dateTime = notificationObj.getString("dateTime");
                notificationList.add(new Notification(message, dateTime));
            }

            // Sort by newest first (already in your original code, keeping it)
            notificationList.sort((n1, n2) -> n2.getDateTime().compareTo(n1.getDateTime()));

            if (notificationAdapter != null) {
                notificationAdapter.notifyDataSetChanged();
            }
            updateEmptyView();

        } catch (JSONException e) {
            Log.e(TAG, "Error loading notifications: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading notifications", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateEmptyView() {
        if (emptyView == null || notificationRecyclerView == null) return; // Null check
        if (notificationList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            notificationRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            notificationRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // clearAllNotifications() is no longer needed if the button is removed.
    // If you still want it accessible programmatically, you can keep it.
    /*
    private void clearAllNotifications() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putString(NOTIFICATION_LIST_KEY, "[]")
                .apply();
        notificationList.clear();
        notificationAdapter.notifyDataSetChanged();
        updateEmptyView();
        Toast.makeText(getContext(), "All notifications cleared", Toast.LENGTH_SHORT).show();
    }
    */

    // saveNotification static methods are in NotificationHelper now.
    // They should not be duplicated here.

    // Inner classes Notification and NotificationAdapter
    // (These were in your provided user_notification.java, keeping them as is)
    public static class Notification {
        private final String message;
        private final String dateTime;

        public Notification(String message, String dateTime) {
            this.message = message;
            this.dateTime = dateTime;
        }
        public String getMessage() { return message; }
        public String getDateTime() { return dateTime; }
    }

    public static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
        private List<Notification> notifications; // Ensure this list is the one managed by the fragment

        public NotificationAdapter(List<Notification> notifications) {
            this.notifications = notifications; // This should be the same list instance as in the fragment
        }

        // updateNotifications method might not be needed if loadNotifications directly modifies
        // the list instance held by both fragment and adapter and then calls notifyDataSetChanged.
        // However, if you use it, ensure it's used correctly.
        public void updateNotifications(List<Notification> newNotifications) {
            this.notifications.clear();
            this.notifications.addAll(newNotifications);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notification_item, parent, false); // Ensure R.layout.notification_item exists
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            Notification notification = notifications.get(position);
            holder.bind(notification);
            // Item click listener removed as swipe is the primary interaction for removal
            // holder.itemView.setOnClickListener(v -> {
            //     Toast.makeText(v.getContext(), notification.getMessage(), Toast.LENGTH_SHORT).show();
            // });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        public static class NotificationViewHolder extends RecyclerView.ViewHolder {
            private final TextView messageTextView;
            private final TextView dateTimeTextView;

            public NotificationViewHolder(View itemView) {
                super(itemView);
                messageTextView = itemView.findViewById(R.id.notification_message); // Ensure these IDs exist in notification_item.xml
                dateTimeTextView = itemView.findViewById(R.id.notification_datetime); // Ensure these IDs exist in notification_item.xml
            }

            public void bind(Notification notification) {
                messageTextView.setText(notification.getMessage());
                dateTimeTextView.setText(notification.getDateTime());
            }
        }
    }
}