package com.pritish.smartbuss;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginFragment extends Fragment {

    private EditText phoneEditText, passEditText;
    private Button loginBtnActual, forgotBtn;
    private CheckBox rememberCheckBox;
    private SharedPreferences sharedPreferences;

    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_REMEMBER_ME_PHONE = "rememberMePhone";
    private static final String KEY_REMEMBER_ME_PASSWORD = "rememberMePassword";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";
    private static final String KEY_USERNAME = "userName";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private static final String TAG = "LoginFragment";
    private DatabaseReference travelerRef, conductorRef;

    // Callback interface for login success
    public interface OnLoginSuccessListener {
        void onLoginSuccess();
    }
    private OnLoginSuccessListener loginSuccessListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnLoginSuccessListener) {
            loginSuccessListener = (OnLoginSuccessListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnLoginSuccessListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);

        travelerRef = FirebaseDatabase.getInstance("https://smartbuss-8c1cb-default-rtdb.firebaseio.com/")
                .getReference("Traveler");
        conductorRef = FirebaseDatabase.getInstance("https://smartbuss-8c1cb-default-rtdb.firebaseio.com/")
                .getReference("Conductor");

        phoneEditText = view.findViewById(R.id.phone);
        passEditText = view.findViewById(R.id.pass);
        loginBtnActual = view.findViewById(R.id.login);
        forgotBtn = view.findViewById(R.id.forgot);
        rememberCheckBox = view.findViewById(R.id.rememberCheckBox);

        ColorStateList colorStateList = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{-android.R.attr.state_checked}},
                new int[]{Color.parseColor("#1570EF"), Color.parseColor("#667085")});
        rememberCheckBox.setButtonTintList(colorStateList);

        String savedPhoneForRememberMe = sharedPreferences.getString(KEY_REMEMBER_ME_PHONE, "");
        String savedPasswordForRememberMe = sharedPreferences.getString(KEY_REMEMBER_ME_PASSWORD, "");
        if (!savedPhoneForRememberMe.isEmpty() && !savedPasswordForRememberMe.isEmpty()) {
            phoneEditText.setText(savedPhoneForRememberMe);
            passEditText.setText(savedPasswordForRememberMe);
            rememberCheckBox.setChecked(true);
        }

        rememberCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
                editor.putString(KEY_REMEMBER_ME_PHONE, phoneEditText.getText().toString().trim());
                editor.putString(KEY_REMEMBER_ME_PASSWORD, passEditText.getText().toString().trim());
                Log.d(TAG, "Remember me checked.");
            } else {
                editor.remove(KEY_REMEMBER_ME_PHONE);
                editor.remove(KEY_REMEMBER_ME_PASSWORD);
                Log.d(TAG, "Remember me unchecked.");
            }
            editor.apply();
        });

        loginBtnActual.setOnClickListener(v -> {
            String userPhone = phoneEditText.getText().toString().trim();
            String userPassword = passEditText.getText().toString().trim();
            if (TextUtils.isEmpty(userPhone)) {
                phoneEditText.setError("Phone number is required"); return;
            }
            if (TextUtils.isEmpty(userPassword)) {
                passEditText.setError("Password is required"); return;
            }
            Log.d(TAG, "Login button clicked for phone: " + userPhone);
            loginUser(userPhone, userPassword);
        });

        forgotBtn.setOnClickListener(v -> startActivity(new Intent(getActivity(), forgot.class)));

        return view;
    }

    /**
     * Hashes the password using SHA-256 with salt for verification
     */
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

    /**
     * Verifies if the entered password matches the stored hashed password
     */
    private boolean verifyPassword(String enteredPassword, String storedHashedPassword, String salt) {
        if (salt == null || salt.isEmpty()) {
            // Handle legacy users who don't have salt (plain text passwords)
            // This is for backward compatibility
            Log.d(TAG, "No salt found, checking as plain text password");
            return enteredPassword.equals(storedHashedPassword);
        }

        String hashedEnteredPassword = hashPassword(enteredPassword, salt);
        if (hashedEnteredPassword == null) {
            return false;
        }

        // Remove any newlines/whitespace that might be added by Base64 encoding
        return hashedEnteredPassword.trim().equals(storedHashedPassword.trim());
    }

    private void loginUser(final String userPhone, final String userPassword) {
        Log.d(TAG, "loginUser: Checking Traveler node for " + userPhone);
        travelerRef.child(userPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    String storedSalt = snapshot.child("salt").getValue(String.class);
                    String fetchedUserName = snapshot.child("name").getValue(String.class);

                    Log.d(TAG, "Traveler data found. Has password: " + (storedPassword != null) +
                            ", Has salt: " + (storedSalt != null) + ", Username: " + fetchedUserName);

                    if (storedPassword != null && verifyPassword(userPassword, storedPassword, storedSalt)) {
                        Log.i(TAG, "Traveler login successful for " + userPhone);
                        String welcomeName = fetchedUserName != null ? fetchedUserName : "User";
                        saveLoginState(userPhone, welcomeName, "Traveler");

                        String welcomeMessage = "Welcome back, " + welcomeName + "!";
                        if (getActivity() != null) {
                            NotificationHelper.saveNotification(getActivity(), welcomeMessage);
                        }
                        Log.d(TAG, "Welcome notification triggered for Traveler: " + welcomeName);
                        if (loginSuccessListener != null) {
                            loginSuccessListener.onLoginSuccess();
                        }
                    } else {
                        Log.w(TAG, "Incorrect password for Traveler " + userPhone);
                        Toast.makeText(getActivity(), "Incorrect password!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "User " + userPhone + " not found in Traveler. Checking Conductor...");
                    checkConductorLogin(userPhone, userPassword);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Traveler DB error: " + error.getMessage());
                Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkConductorLogin(final String userPhone, final String userPassword) {
        Log.d(TAG, "checkConductorLogin: Checking Conductor node for " + userPhone);
        conductorRef.child(userPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    String storedSalt = snapshot.child("salt").getValue(String.class);
                    String fetchedUserName = snapshot.child("name").getValue(String.class);

                    Log.d(TAG, "Conductor data found. Has password: " + (storedPassword != null) +
                            ", Has salt: " + (storedSalt != null) + ", Username: " + fetchedUserName);

                    if (storedPassword != null && verifyPassword(userPassword, storedPassword, storedSalt)) {
                        Log.i(TAG, "Conductor login successful for " + userPhone);
                        String welcomeName = fetchedUserName != null ? fetchedUserName : "Conductor";
                        saveLoginState(userPhone, welcomeName, "Conductor");

                        String welcomeMessage = "Welcome back, " + welcomeName + "!";
                        if (getActivity() != null) {
                            NotificationHelper.saveNotification(getActivity(), welcomeMessage);
                        }
                        Log.d(TAG, "Welcome notification triggered for Conductor: " + welcomeName);
                        if (loginSuccessListener != null) {
                            loginSuccessListener.onLoginSuccess();
                        }
                    } else {
                        Log.w(TAG, "Incorrect password for Conductor " + userPhone);
                        Toast.makeText(getActivity(), "Incorrect password!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "User " + userPhone + " not found in Conductor either.");
                    Toast.makeText(getActivity(), "User not found!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Conductor DB error: " + error.getMessage());
                Toast.makeText(getActivity(), "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLoginState(String userPhone, String userNameToSave, String userType) {
        Log.d(TAG, "saveLoginState: Phone: " + userPhone + ", UserName: " + userNameToSave + ", UserType: " + userType);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LOGGED_IN_PHONE, userPhone);
        editor.putString(KEY_USERNAME, userNameToSave != null ? userNameToSave : (userType.equals("Traveler") ? "User" : "Conductor"));
        editor.putString(KEY_USER_TYPE, userType);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
        Log.i(TAG, "Login state saved successfully for " + userPhone);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        loginSuccessListener = null;
    }
}