package com.pritish.smartbuss;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class user_NormalBooking extends DialogFragment {

    private EditText numberEditText;
    private ImageButton selectContactBtn;
    private String mainuserPhone; // Field to store the mainuserPhone

    private static final int CONTACT_PICKER_REQUEST = 1001;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1002;

    // Factory method to create a new instance of the dialog with mainuserPhone
    public static user_NormalBooking newInstance(String mainuserPhone) {
        user_NormalBooking fragment = new user_NormalBooking();
        Bundle args = new Bundle();
        args.putString("mainuserPhone", mainuserPhone);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the mainuserPhone from arguments
        if (getArguments() != null) {
            mainuserPhone = getArguments().getString("mainuserPhone");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this dialog
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View view = inflater.inflate(R.layout.user_normal_booking, container, false);

        // Initialize views
        numberEditText = view.findViewById(R.id.number);
        selectContactBtn = view.findViewById(R.id.selectContactBtn);

        // Set up the book ticket button click listener
        view.findViewById(R.id.bookticket).setOnClickListener(v -> checkIfNumberExistsInDatabase());

        // Set up the cancel button click listener
        view.findViewById(R.id.cancelBtn).setOnClickListener(v -> dismiss());

        // Set up the select contact button click listener
        selectContactBtn.setOnClickListener(v -> selectContactFromPhoneBook());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set dialog width and height
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Method to select contact from phone book
    private void selectContactFromPhoneBook() {
        // Check if we have permission to read contacts
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            // Permission already granted, open contact picker
            openContactPicker();
        }
    }

    // Method to open contact picker
    private void openContactPicker() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open contact picker
                openContactPicker();
            } else {
                // Permission denied
                Toast.makeText(getContext(), "Permission denied. Cannot access contacts.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACT_PICKER_REQUEST && resultCode == getActivity().RESULT_OK) {
            if (data != null) {
                Uri contactUri = data.getData();
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

                try (Cursor cursor = getActivity().getContentResolver().query(contactUri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        String phoneNumber = cursor.getString(numberIndex);

                        // Clean the phone number (remove spaces, dashes, etc.)
                        phoneNumber = cleanPhoneNumber(phoneNumber);

                        // Set the phone number in the EditText
                        numberEditText.setText(phoneNumber);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error reading contact", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Method to clean phone number (remove non-digit characters except +)
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");

        // If it starts with +91, remove the country code for Indian numbers
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        }

        // If it's longer than 10 digits and doesn't start with +, take last 10 digits
        if (cleaned.length() > 10 && !cleaned.startsWith("+")) {
            cleaned = cleaned.substring(cleaned.length() - 10);
        }

        return cleaned;
    }

    // Method to check if the entered number exists in Firebase Realtime Database
    private void checkIfNumberExistsInDatabase() {
        String enteredNumber = numberEditText.getText().toString().trim();

        if (enteredNumber.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a number or select from contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate the phone number
        if (!isValidPhoneNumber(enteredNumber)) {
            Toast.makeText(getContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the entered number is the same as main user's number
        if (enteredNumber.equals(mainuserPhone)) {
            Toast.makeText(getContext(), "The number belongs to this account. Please go for quick booking.", Toast.LENGTH_LONG).show();
            dismiss(); // Dismiss the current dialog
            return; // Stop further execution of this method
        }

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Traveler");

        databaseReference.child(enteredNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Toast.makeText(getContext(), "Traveler is an existing user. You can proceed with booking.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Number not found in the database.", Toast.LENGTH_SHORT).show();
                }

                // Pass the mainuserPhone and enteredNumber to user_map_others
                Intent intent = new Intent(getContext(), user_map_others.class);
                intent.putExtra("mainuserPhone", mainuserPhone); // Pass the mainuserPhone
                intent.putExtra("enteredNumber", enteredNumber); // Pass the entered number
                startActivity(intent);
                dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to check the number. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to validate phone number (example: 10 digits long)
    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("\\d{10}");
    }
}