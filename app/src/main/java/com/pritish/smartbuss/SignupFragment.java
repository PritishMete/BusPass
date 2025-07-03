package com.pritish.smartbuss;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class SignupFragment extends Fragment {

    private static final String TAG = "SignupFragment";
    private static final long OTP_TIMEOUT_MILLIS = 60000; // 60 seconds

    private EditText nameEditText, phoneEditText, passEditText, repassEditText, otpEditText;
    private Button signupBtnActual, otpBtn;
    private TextView countdownText;
    private Spinner roleSpinner;
    private CheckBox showPasswordCheckbox, showRetypePasswordCheckbox;

    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private String verificationId;
    private CountDownTimer countDownTimer;
    private boolean isOtpSent = false;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    public interface OnSignupSuccessListener {
        void onSignupSuccess();
    }
    private OnSignupSuccessListener signupSuccessListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSignupSuccessListener) {
            signupSuccessListener = (OnSignupSuccessListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnSignupSuccessListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://smartbuss-8c1cb-default-rtdb.firebaseio.com/");

        // Initialize views
        initializeViews(view);

        // Setup OTP button click listener
        otpBtn.setOnClickListener(v -> {
            String phoneNumber = phoneEditText.getText().toString().trim();
            if (validatePhoneNumber(phoneNumber)) {
                sendOtp(phoneNumber);
            }
        });

        // Setup signup button click listener
        signupBtnActual.setOnClickListener(v -> {
            if (isOtpSent) {
                verifyOtpAndRegister();
            } else {
                Toast.makeText(getActivity(), "Please get OTP first", Toast.LENGTH_SHORT).show();
            }
        });

        setupPasswordCheckboxListeners();

        return view;
    }

    private void initializeViews(View view) {
        nameEditText = view.findViewById(R.id.name);
        phoneEditText = view.findViewById(R.id.phone);
        passEditText = view.findViewById(R.id.pass);
        repassEditText = view.findViewById(R.id.repass);
        otpEditText = view.findViewById(R.id.otp);
        signupBtnActual = view.findViewById(R.id.sgn);
        roleSpinner = view.findViewById(R.id.spinner);
        otpBtn = view.findViewById(R.id.otpbtn);
        countdownText = view.findViewById(R.id.otp_timer);
        showPasswordCheckbox = view.findViewById(R.id.show_password_checkbox);
        showRetypePasswordCheckbox = view.findViewById(R.id.show_retype_password_checkbox);

        // Make OTP fields visible
        otpEditText.setVisibility(View.VISIBLE);
        otpBtn.setVisibility(View.VISIBLE);
        countdownText.setVisibility(View.VISIBLE);
    }

    private boolean validatePhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneEditText.setError("Phone number is required");
            return false;
        }
        if (phoneNumber.length() != 10 || !TextUtils.isDigitsOnly(phoneNumber)) {
            phoneEditText.setError("Enter a valid 10-digit phone number");
            return false;
        }
        return true;
    }

    private void sendOtp(String phoneNumber) {
        // Show loading state
        otpBtn.setEnabled(false);
        otpBtn.setText("Sending...");

        // Format phone number with country code
        String formattedPhone = "+91" + phoneNumber;

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(formattedPhone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                // Auto-retrieval is disabled, so this won't be called
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                otpBtn.setEnabled(true);
                                otpBtn.setText("GET OTP");
                                Toast.makeText(getActivity(), "OTP sending failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId,
                                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                otpBtn.setEnabled(true);
                                otpBtn.setText("GET OTP");
                                SignupFragment.this.verificationId = verificationId;
                                resendToken = token;
                                isOtpSent = true;
                                startCountdown();
                                Toast.makeText(getActivity(), "OTP sent successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void startCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(OTP_TIMEOUT_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownText.setText("Resend OTP in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                countdownText.setText("OTP expired");
                isOtpSent = false;
            }
        }.start();
    }

    private void verifyOtpAndRegister() {
        String otp = otpEditText.getText().toString().trim();
        if (TextUtils.isEmpty(otp)) {
            otpEditText.setError("Enter OTP");
            return;
        }

        if (verificationId == null) {
            Toast.makeText(getActivity(), "OTP verification failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify OTP
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // OTP verification successful, proceed with registration
                        registerUser();
                    } else {
                        Toast.makeText(getActivity(), "Invalid OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        String userNameVal = nameEditText.getText().toString().trim();
        String userPhoneVal = phoneEditText.getText().toString().trim();
        String userPasswordVal = passEditText.getText().toString().trim();
        String userRePasswordVal = repassEditText.getText().toString().trim();
        String selectedRoleVal = (roleSpinner.getSelectedItem() != null) ? roleSpinner.getSelectedItem().toString() : "";

        // Validate all fields
        if (!validateRegistrationFields(userNameVal, userPhoneVal, userPasswordVal, userRePasswordVal, selectedRoleVal)) {
            return;
        }

        createUser(userNameVal, userPhoneVal, userPasswordVal, selectedRoleVal);
    }

    private boolean validateRegistrationFields(String name, String phone, String password, String repassword, String role) {
        if (TextUtils.isEmpty(role) || "Select Role".equals(role)) {
            Toast.makeText(getActivity(), "Please select a valid role.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(name)) { nameEditText.setError("Name is required"); return false; }
        if (TextUtils.isEmpty(phone)) { phoneEditText.setError("Phone number is required"); return false; }
        if (phone.length() != 10 || !TextUtils.isDigitsOnly(phone)) {
            phoneEditText.setError("Enter a valid 10-digit phone number");
            return false;
        }
        if (TextUtils.isEmpty(password)) { passEditText.setError("Password is required"); return false; }
        if (password.length() < 6) {
            passEditText.setError("Password must be at least 6 characters");
            return false;
        }
        if (!password.equals(repassword)) { repassEditText.setError("Passwords do not match"); return false; }
        return true;
    }

    private void setupPasswordCheckboxListeners() {
        if (showPasswordCheckbox != null && passEditText != null) {
            showPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    passEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    passEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                passEditText.setSelection(passEditText.getText().length());
            });
        }

        if (showRetypePasswordCheckbox != null && repassEditText != null) {
            showRetypePasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    repassEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    repassEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                repassEditText.setSelection(repassEditText.getText().length());
            });
        }
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.DEFAULT);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String saltedPassword = password + salt;
            byte[] hashedBytes = md.digest(saltedPassword.getBytes());
            return Base64.encodeToString(hashedBytes, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }

    private void createUser(final String userName, final String userPhone, final String userPassword, final String selectedRole) {
        DatabaseReference roleBasedRef;
        if ("Traveler".equalsIgnoreCase(selectedRole)) {
            roleBasedRef = firebaseDatabase.getReference("Traveler");
        } else if ("Conductor".equalsIgnoreCase(selectedRole)) {
            roleBasedRef = firebaseDatabase.getReference("Conductor");
        } else {
            Toast.makeText(getActivity(), "Invalid role selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        roleBasedRef.child(userPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(getActivity(), "User with this phone number already exists as a " + selectedRole + "!", Toast.LENGTH_LONG).show();
                } else {
                    // Generate salt and hash the password
                    String salt = generateSalt();
                    String hashedPassword = hashPassword(userPassword, salt);

                    if (hashedPassword == null) {
                        Toast.makeText(getActivity(), "Error processing password. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create RegUserDet object with hashed password
                    RegUserDet user = new RegUserDet();
                    user.setName(userName);
                    user.setPhone(userPhone);
                    user.setPassword(hashedPassword);
                    user.setSalt(salt);

                    roleBasedRef.child(userPhone).setValue(user).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User registered successfully in Firebase with hashed password.");
                            Toast.makeText(getActivity(), "Registration successful!", Toast.LENGTH_SHORT).show();
                            if (signupSuccessListener != null) {
                                signupSuccessListener.onSignupSuccess();
                            }
                        } else {
                            Toast.makeText(getActivity(), "Registration failed! " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Database Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        signupSuccessListener = null;
    }
}