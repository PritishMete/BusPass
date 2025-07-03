package com.pritish.smartbuss;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class user_send_emergency_dialog extends DialogFragment {

    private static final String ARG_USER_PHONE = "userPhone";

    private EditText titleEditText;
    private EditText contentEditText;
    private RadioGroup severityRadioGroup;
    private Button sendButton;
    private Button cancelButton;

    private String userPhone;
    private DatabaseReference emergencyRef;

    public static user_send_emergency_dialog newInstance(String userPhone) {
        user_send_emergency_dialog fragment = new user_send_emergency_dialog();
        Bundle args = new Bundle();
        args.putString(ARG_USER_PHONE, userPhone);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            userPhone = getArguments().getString(ARG_USER_PHONE);
        }

        emergencyRef = FirebaseDatabase.getInstance().getReference("emergency");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_send_emergency, container, false);

        initViews(view);
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        titleEditText = view.findViewById(R.id.emergency_title_edit);
        contentEditText = view.findViewById(R.id.emergency_content_edit);
        severityRadioGroup = view.findViewById(R.id.severity_radio_group);
        sendButton = view.findViewById(R.id.send_emergency_btn);
        cancelButton = view.findViewById(R.id.cancel_emergency_btn);

        // Log which views are null to help debug
        Log.d("SendEmergencyDialog", "titleEditText: " + (titleEditText != null));
        Log.d("SendEmergencyDialog", "contentEditText: " + (contentEditText != null));
        Log.d("SendEmergencyDialog", "severityRadioGroup: " + (severityRadioGroup != null));
        Log.d("SendEmergencyDialog", "sendButton: " + (sendButton != null));
        Log.d("SendEmergencyDialog", "cancelButton: " + (cancelButton != null));
    }

    private void setupClickListeners() {
        if (sendButton != null) {
            sendButton.setOnClickListener(v -> sendEmergencyReport());
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dismiss());
        }
    }

    private void sendEmergencyReport() {
        // Validate inputs
        if (titleEditText == null || contentEditText == null) {
            Toast.makeText(getContext(), "Form elements not properly initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            return;
        }

        if (TextUtils.isEmpty(content)) {
            contentEditText.setError("Message content is required");
            return;
        }

        String severity = getSelectedSeverity();

        // Create emergency report
        Map<String, Object> emergencyReport = new HashMap<>();
        emergencyReport.put("title", title);
        emergencyReport.put("content", content);
        emergencyReport.put("severity", severity);
        emergencyReport.put("sender", userPhone != null ? userPhone : "Anonymous");
        emergencyReport.put("senderType", "User");
        emergencyReport.put("status", "pending"); // Will be approved by admin
        emergencyReport.put("timestamp", System.currentTimeMillis());
        emergencyReport.put("dateCreated", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        // Disable send button to prevent multiple submissions
        if (sendButton != null) {
            sendButton.setEnabled(false);
            sendButton.setText("Sending...");
        }

        // Send to Firebase
        emergencyRef.push().setValue(emergencyReport)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Emergency report sent successfully! Awaiting admin approval.", Toast.LENGTH_LONG).show();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e("SendEmergencyDialog", "Failed to send emergency report", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to send emergency report. Please try again.", Toast.LENGTH_SHORT).show();
                    }

                    // Re-enable send button
                    if (sendButton != null) {
                        sendButton.setEnabled(true);
                        sendButton.setText("Send Emergency Report");
                    }
                });
    }

    private String getSelectedSeverity() {
        // Check if severityRadioGroup exists
        if (severityRadioGroup == null) {
            Log.w("SendEmergencyDialog", "Severity RadioGroup is null, defaulting to 'medium'");
            return "medium";
        }

        int selectedId = severityRadioGroup.getCheckedRadioButtonId();

        // If no radio button is selected, default to medium
        if (selectedId == -1) {
            Log.w("SendEmergencyDialog", "No severity selected, defaulting to 'medium'");
            return "medium";
        }

        RadioButton selectedRadioButton = severityRadioGroup.findViewById(selectedId);
        if (selectedRadioButton == null) {
            Log.w("SendEmergencyDialog", "Selected RadioButton is null, defaulting to 'medium'");
            return "medium";
        }

        String selectedText = selectedRadioButton.getText().toString().toLowerCase();

        // Map the text to severity levels
        switch (selectedText) {
            case "high":
            case "urgent":
            case "critical":
                return "high";
            case "low":
            case "info":
            case "information":
                return "low";
            case "medium":
            case "normal":
            default:
                return "medium";
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Make dialog wider
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}