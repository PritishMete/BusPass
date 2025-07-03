package com.pritish.smartbuss;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.concurrent.TimeUnit;

public class forgot extends AppCompatActivity {

    EditText etPhone, etOtp, etNewPassword;
    Button btnSendOtp, btnVerifyOtp, btnUpdatePassword;

    FirebaseAuth mAuth;
    String verificationId;
    DatabaseReference travelerRef, conductorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot);

        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        mAuth = FirebaseAuth.getInstance();


        FirebaseDatabase db = FirebaseDatabase.getInstance("https://smartbuss-8c1cb-default-rtdb.firebaseio.com/");
        travelerRef = db.getReference("Traveler");
        conductorRef = db.getReference("Conductor");

        btnSendOtp.setOnClickListener(v -> sendOtp());
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void sendOtp() {
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(phone) || phone.length() != 10) {
            Toast.makeText(this, "Enter valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullPhone = "+91" + phone;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(fullPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential credential) {

        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(forgot.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
            verificationId = id;
            etOtp.setVisibility(View.VISIBLE);
            btnVerifyOtp.setVisibility(View.VISIBLE);
            Toast.makeText(forgot.this, "OTP Sent", Toast.LENGTH_SHORT).show();
        }
    };

    private void verifyOtp() {
        String otp = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) {
            Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Phone Verified", Toast.LENGTH_SHORT).show();
                        etNewPassword.setVisibility(View.VISIBLE);
                        btnUpdatePassword.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePassword() {
        String phone = etPhone.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }


        travelerRef.child(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    travelerRef.child(phone).child("password").setValue(newPassword);
                    Toast.makeText(forgot.this, "Traveler password updated", Toast.LENGTH_SHORT).show();
                } else {
                    // If not found in Traveler, check in Conductor
                    checkConductor(phone, newPassword);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(forgot.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkConductor(String phone, String newPassword) {
        conductorRef.child(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    conductorRef.child(phone).child("password").setValue(newPassword);
                    Toast.makeText(forgot.this, "Conductor password updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(forgot.this, "User not found in Traveler or Conductor", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(forgot.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}