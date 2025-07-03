package com.pritish.smartbuss;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class user_profile extends Fragment {

    private static final String TAG_PROFILE = "UserProfileFragment";

    // SharedPreferences constants
    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "userName";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";
    private static final String KEY_PROFILE_PIC_URI = "profilePicUri";
    private static final String KEY_DOB = "dateOfBirth";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_CITY = "userCity";

    // Firebase Node constants
    private static final String FIREBASE_NODE_TRAVELER = "Traveler";

    private ImageView profilePictureImageView;
    private TextView userNameTextView;
    private EditText phoneNumberEditText, cityEditText, emailEditText, dobEditText;
    private ImageButton iconEditSaveProfileImageButton;
    private Button logoutButton;

    private boolean isEditingMode = false;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;
    private Calendar dateOfBirthCalendar;

    private int originalEmailTextColor;
    private int originalPhoneTextColor;
    private int originalCityTextColor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        dateOfBirthCalendar = Calendar.getInstance();

        // Initialize ActivityResultLauncher for image cropping
        cropImageLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri croppedImageUri = result.getUriContent();
                if (croppedImageUri != null && profilePictureImageView != null) {
                    profilePictureImageView.setImageURI(croppedImageUri);
                    sharedPreferences.edit().putString(KEY_PROFILE_PIC_URI, croppedImageUri.toString()).apply();
                    Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG_PROFILE, "Profile picture cropped and saved: " + croppedImageUri.toString());
                } else {
                    Log.e(TAG_PROFILE, "Cropped image URI or ImageView is null.");
                }
            } else {
                Exception error = result.getError();
                Log.e(TAG_PROFILE, "Image cropping failed: " + (error != null ? error.getMessage() : "Cancelled by user"));
            }
        });

        // Initialize ActivityResultLauncher for permission request
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG_PROFILE, "Storage permission granted.");
                        startImageCrop();
                    } else {
                        Toast.makeText(getContext(), "Permission denied to access gallery.", Toast.LENGTH_LONG).show();
                        Log.w(TAG_PROFILE, "Storage permission denied.");
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_profile, container, false);
        initializeViews(view);
        storeOriginalTextColors();
        loadUserProfileData();
        setupListeners(view); // CORRECTED: Pass the inflated view here
        updateFieldsAndIconUI(false); // Initial state: view mode
        return view;
    }

    private void initializeViews(View view) {
        profilePictureImageView = view.findViewById(R.id.profile_picture);
        userNameTextView = view.findViewById(R.id.user_name);
        emailEditText = view.findViewById(R.id.email_text_view);
        phoneNumberEditText = view.findViewById(R.id.phone_number_text_view);
        cityEditText = view.findViewById(R.id.citydetail);
        dobEditText = view.findViewById(R.id.dob_text_view);
        iconEditSaveProfileImageButton = view.findViewById(R.id.icon_edit_save_profile);
        logoutButton = view.findViewById(R.id.btn_logout);
    }

    private void storeOriginalTextColors() {
        if (emailEditText != null) originalEmailTextColor = emailEditText.getCurrentTextColor();
        if (phoneNumberEditText != null) originalPhoneTextColor = phoneNumberEditText.getCurrentTextColor();
        if (cityEditText != null) originalCityTextColor = cityEditText.getCurrentTextColor();
    }

    // CORRECTED: Method now accepts a View parameter
    private void setupListeners(View view) {
        profilePictureImageView.setOnClickListener(v -> checkPermissionAndStartCrop());

        dobEditText.setOnClickListener(v -> {
            if (isEditingMode) {
                showDatePickerDialog();
            }
        });
        dobEditText.setFocusable(false); // Disable keyboard input

        iconEditSaveProfileImageButton.setOnClickListener(v -> toggleEditSaveMode());
        logoutButton.setOnClickListener(v -> logoutUser());

        // Other buttons
        // CORRECTED: Use the passed 'view' object, not getView()
        MaterialButton helpButton = view.findViewById(R.id.helpButton);
        helpButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), HelpActivity.class)));

        MaterialButton referralButton = view.findViewById(R.id.referralButton);
        referralButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), ReferralActivity.class)));

        MaterialButton issueButton = view.findViewById(R.id.technicalIssueButton);
        issueButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), IssueActivity.class)));
    }

    private void toggleEditSaveMode() {
        if (isEditingMode) { // If currently in "Edit" mode, try to "Save"
            if (validateInputs()) {
                saveUserProfileData();
                isEditingMode = false;
                updateFieldsAndIconUI(false);
            }
        } else { // If in "View" mode, switch to "Edit"
            isEditingMode = true;
            updateFieldsAndIconUI(true);
            Toast.makeText(getContext(), "Editing enabled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFieldsAndIconUI(boolean enable) {
        // Enable or disable fields for editing
        emailEditText.setEnabled(enable);
        cityEditText.setEnabled(enable);
        // Phone number is typically not editable, so we keep it disabled
        phoneNumberEditText.setEnabled(false);

        // Update text colors to indicate editability
        if (getContext() != null) {
            emailEditText.setTextColor(enable ? ContextCompat.getColor(getContext(), R.color.text_color_editable) : originalEmailTextColor);
            cityEditText.setTextColor(enable ? ContextCompat.getColor(getContext(), R.color.text_color_editable) : originalCityTextColor);
        }

        // Change the icon from "Pencil" to "Save" and back
        iconEditSaveProfileImageButton.setImageResource(enable ? R.drawable.ic_save : R.drawable.ic_pencil);
        iconEditSaveProfileImageButton.setContentDescription(enable ? "Save Profile" : "Edit Profile");
    }

    private void loadUserProfileData() {
        // Load data from SharedPreferences and display it
        String savedUserName = sharedPreferences.getString(KEY_USERNAME, "User Name");
        userNameTextView.setText(savedUserName);

        String imageUriString = sharedPreferences.getString(KEY_PROFILE_PIC_URI, null);
        if (imageUriString != null) {
            profilePictureImageView.setImageURI(Uri.parse(imageUriString));
        } else {
            profilePictureImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }

        phoneNumberEditText.setText(sharedPreferences.getString(KEY_LOGGED_IN_PHONE, ""));
        emailEditText.setText(sharedPreferences.getString(KEY_EMAIL, ""));
        cityEditText.setText(sharedPreferences.getString(KEY_CITY, ""));
        dobEditText.setText(sharedPreferences.getString(KEY_DOB, ""));
    }

    private void saveUserProfileData() {
        String phoneNumber = sharedPreferences.getString(KEY_LOGGED_IN_PHONE, "");
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(getContext(), "Error: User not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get updated values from UI
        String email = emailEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();

        // Create a Map with only the data that needs to be updated
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("email", email);
        userUpdates.put("city", city);
        userUpdates.put("dob", dob);
        // Note: We are not putting 'name', 'password', or 'salt' here.

        // Get reference to the specific user in Firebase
        DatabaseReference databaseRef = FirebaseDatabase.getInstance()
                .getReference(FIREBASE_NODE_TRAVELER)
                .child(phoneNumber);

        databaseRef.updateChildren(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update local SharedPreferences as well
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_EMAIL, email);
                    editor.putString(KEY_CITY, city);
                    editor.putString(KEY_DOB, dob);
                    editor.apply();

                    Log.d(TAG_PROFILE, "Profile updated successfully in Firebase and SharedPreferences.");
                    Toast.makeText(getContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG_PROFILE, "Failed to update profile in Firebase.", e);
                    Toast.makeText(getContext(), "Failed to save profile. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void logoutUser() {
        // Clear all session data
        sharedPreferences.edit().clear().apply();

        // Navigate to the login screen
        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finishAffinity();
        Toast.makeText(getActivity(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
    }

    private void checkPermissionAndStartCrop() {
        if (getContext() == null) return;
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            startImageCrop();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void startImageCrop() {
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.cropShape = CropImageView.CropShape.OVAL;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;
        cropOptions.fixAspectRatio = true;
        cropOptions.guidelines = CropImageView.Guidelines.ON;
        cropOptions.outputCompressQuality = 70;
        cropOptions.activityTitle = "Crop Profile Picture";
        cropOptions.autoZoomEnabled = true;

        CropImageContractOptions contractOptions = new CropImageContractOptions(null, cropOptions);
        cropImageLauncher.launch(contractOptions);
    }


    private void showDatePickerDialog() {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            dateOfBirthCalendar.set(Calendar.YEAR, year);
            dateOfBirthCalendar.set(Calendar.MONTH, month);
            dateOfBirthCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateOfBirthLabel();
        },
                dateOfBirthCalendar.get(Calendar.YEAR),
                dateOfBirthCalendar.get(Calendar.MONTH),
                dateOfBirthCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDateOfBirthLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dobEditText.setText(sdf.format(dateOfBirthCalendar.getTime()));
    }

    private boolean validateInputs() {
        // Simple validation for email format
        String email = emailEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            return false;
        }
        return true;
    }
}