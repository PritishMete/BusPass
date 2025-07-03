package com.pritish.smartbuss;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserWalletRechargeActivity extends AppCompatActivity implements PaymentResultListener {

    private static final String TAG = "WalletRecharge";
    private static final String WALLET_PREFS = "WalletPrefs";
    private static final String WALLET_BALANCE_KEY = "wallet_balance";
    private static final String USER_DETAILS_PREFS = "smartbus_pref";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";
    private static final String TARGET_UPI_ID = "abhradipadidas@oksbi"; // Replace
    private static final String TARGET_PAYEE_NAME = "SmartBus Wallet";
    private static final String RAZORPAY_KEY_ID = "rzp_test_P5PbrmSAAg18Hf"; // Replace
    private static final String NOTIFICATION_CHANNEL_ID = "wallet_notifications";

    private MaterialCardView payWithGooglePayButton, payWithAmazonPayButton, payWithPaytmButton, payWithPhonePeButton;
    private EditText rechargeAmountEditText;
    private MaterialButton chooseAnotherUpiAppButton, payWithDebitCardButton;
    private ActivityResultLauncher<Intent> upiPaymentActivityResultLauncher;
    // Removed 'userRef' instance variable as it's better to get fresh reference in methods using currentUserPhone
    private String currentUserPhone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_wallet_recharge_button);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recharge Wallet");
        }

        try {
            Checkout.preload(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Razorpay preload error", e);
        }

        initializeViews();
        setupPaymentListeners();
        createNotificationChannel();

        currentUserPhone = getSharedPreferences(USER_DETAILS_PREFS, MODE_PRIVATE)
                .getString(KEY_LOGGED_IN_PHONE, null);

        if (currentUserPhone == null || currentUserPhone.isEmpty()) {
            Toast.makeText(this, "User not logged in. Cannot proceed.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Wallet Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void initializeViews() {
        rechargeAmountEditText = findViewById(R.id.rechargeAmountEditText);
        payWithGooglePayButton = findViewById(R.id.payWithGooglePayButton);
        payWithAmazonPayButton = findViewById(R.id.payWithAmazonPayButton);
        payWithPaytmButton = findViewById(R.id.payWithpaytm);
        payWithPhonePeButton = findViewById(R.id.payWithPhonePeButton);
        chooseAnotherUpiAppButton = findViewById(R.id.chooseAnotherUpiAppButton);
        payWithDebitCardButton = findViewById(R.id.payWithDebitCardButton);

        upiPaymentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleUpiPaymentResult);
    }

    private void setupPaymentListeners() {
        payWithGooglePayButton.setOnClickListener(v -> initiateUpiPayment("com.google.android.apps.nbu.paisa.user"));
        payWithAmazonPayButton.setOnClickListener(v -> initiateUpiPayment("in.amazon.mShop.android.shopping"));
        payWithPaytmButton.setOnClickListener(v -> initiateUpiPayment("net.one97.paytm"));
        payWithPhonePeButton.setOnClickListener(v -> initiateUpiPayment("com.phonepe.app"));
        chooseAnotherUpiAppButton.setOnClickListener(v -> initiateUpiPayment(null));
        payWithDebitCardButton.setOnClickListener(v -> {
            String amount = getValidRechargeAmount();
            if (amount != null) {
                startRazorpayPayment(amount);
            }
        });
    }

    private String getValidRechargeAmount() {
        if (rechargeAmountEditText == null) {
            showToast("Error: Amount field not found.");
            return null;
        }
        String amountStr = rechargeAmountEditText.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            rechargeAmountEditText.setError("Amount cannot be empty");
            return null;
        }
        try {
            double amountValue = Double.parseDouble(amountStr);
            if (amountValue <= 0) {
                rechargeAmountEditText.setError("Amount must be greater than zero");
                return null;
            }
            return String.format(Locale.US, "%.2f", amountValue);
        } catch (NumberFormatException e) {
            rechargeAmountEditText.setError("Invalid amount format");
            return null;
        }
    }

    private void startRazorpayPayment(String amount) {
        final Activity activity = this;
        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(RAZORPAY_KEY_ID);

            JSONObject options = new JSONObject();
            options.put("name", TARGET_PAYEE_NAME);
            options.put("description", "Wallet Recharge");
            options.put("currency", "INR");
            options.put("amount", (int) (Double.parseDouble(amount) * 100));
            options.put("theme.color", "#2196F3");

            checkout.open(activity, options);
        } catch (Exception e) { // Catch generic Exception for unforeseen Razorpay issues
            showToast("Payment initialization error. Please try again.");
            Log.e(TAG, "Razorpay Checkout error", e);
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Log.d(TAG, "Razorpay Payment Success. ID: " + razorpayPaymentID);
        String amountStr = getValidRechargeAmount();
        if (amountStr != null) {
            try {
                final double finalRechargeAmount = Double.parseDouble(amountStr); // Final for lambda if needed, though not directly used in one here
                final String finalRazorpayPaymentID = razorpayPaymentID; // Final for lambda if needed

                updateWalletBalance(amountStr, true);
                sendNotification(true, (float) finalRechargeAmount);
                saveRechargeTransaction(finalRazorpayPaymentID, finalRechargeAmount, "Recharged via Card/NetBanking");
                showToast("Payment Successful! ₹" + amountStr + " added.");
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing amount: " + amountStr, e);
                showToast("Payment successful, error processing amount.");
            }
        } else {
            Log.e(TAG, "Razorpay success, but amount was null.");
            showToast("Payment successful, amount unclear.");
        }
        finish();
    }

    @Override
    public void onPaymentError(int code, String response) {
        Log.e(TAG, "Razorpay Error. Code: " + code + ", Response: " + response);
        sendNotification(false, 0);
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String errorMessage = jsonResponse.optJSONObject("error") != null ?
                    jsonResponse.optJSONObject("error").optString("description", "Payment Failed") :
                    "Payment Failed";
            showToast(errorMessage);
        } catch (JSONException e) {
            showToast("Payment Failed: " + response);
        }
        finish();
    }

    private void updateWalletBalance(String amountStr, boolean fromRazorpayOrSimilar) {
        if (currentUserPhone == null || currentUserPhone.isEmpty()) {
            Log.e(TAG, "currentUserPhone is null. Cannot update balance.");
            showToast("User session error.");
            return;
        }

        final float rechargeAmountToApply;
        final boolean isPaymentFromRazorpay = fromRazorpayOrSimilar; // effectively final

        try {
            rechargeAmountToApply = Float.parseFloat(amountStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing amount in updateWalletBalance: " + amountStr, e);
            showToast("Error processing recharge amount.");
            return;
        }

        DatabaseReference specificUserRef = FirebaseDatabase.getInstance().getReference("Traveler").child(currentUserPhone);

        specificUserRef.child("walletBalance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float currentBalance = 0.0f;
                if (snapshot.exists() && snapshot.getValue(Float.class) != null) {
                    currentBalance = snapshot.getValue(Float.class);
                }
                float newBalance = currentBalance + rechargeAmountToApply; // Uses final rechargeAmountToApply

                specificUserRef.child("walletBalance").setValue(newBalance)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Firebase walletBalance updated to: " + newBalance);
                            if (getApplicationContext() != null) {
                                SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(WALLET_PREFS, MODE_PRIVATE);
                                sharedPrefs.edit().putFloat(WALLET_BALANCE_KEY, newBalance).apply();
                                Log.d(TAG, "Local SharedPreferences WALLET_BALANCE_KEY updated.");
                            }
                            if (isPaymentFromRazorpay) { // Uses final isPaymentFromRazorpay
                                Log.d(TAG, "Balance update was for a Razorpay-like transaction.");
                            } else {
                                Log.d(TAG, "Balance update was for a UPI-like transaction.");
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update Firebase walletBalance", e);
                            showToast("Balance update on server failed.");
                        });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read current walletBalance", error.toException());
                showToast("Error fetching current balance.");
            }
        });
    }

    private void saveRechargeTransaction(String rechargeId, double amount, String paymentMethodRemark) {
        if (currentUserPhone == null || currentUserPhone.isEmpty()) {
            Log.e(TAG, "Cannot save recharge transaction, user phone is null.");
            return;
        }
        // Ensure rechargeId is effectively final for the listeners below
        final String finalRechargeId = (rechargeId == null || rechargeId.isEmpty()) ? "TXN_" + System.currentTimeMillis() : rechargeId;
        final double finalAmount = amount; // Parameters are already effectively final if not reassigned
        final String finalPaymentMethodRemark = paymentMethodRemark;

        DatabaseReference userRechargesRef = FirebaseDatabase.getInstance()
                .getReference("UserRecharges")
                .child(currentUserPhone)
                .child(finalRechargeId);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String currentTime = timeFormat.format(new Date());

        Map<String, Object> rechargeDetails = new HashMap<>();
        rechargeDetails.put("transactionId", finalRechargeId);
        rechargeDetails.put("amount", finalAmount);
        rechargeDetails.put("date", currentDate);
        rechargeDetails.put("time", currentTime);
        rechargeDetails.put("remark", (finalPaymentMethodRemark != null ? finalPaymentMethodRemark : "Wallet Recharge"));
        rechargeDetails.put("status", "SUCCESSFUL");

        userRechargesRef.setValue(rechargeDetails)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Recharge transaction saved successfully for ID: " + finalRechargeId)) // Uses finalRechargeId
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save recharge transaction for ID: " + finalRechargeId, e)); // Uses finalRechargeId
    }

    private void sendNotification(boolean isSuccess, float amount) {
        String title = isSuccess ? "Wallet Recharged" : "Payment Failed";
        String message = isSuccess ?
                String.format(Locale.getDefault(), "₹%.2f added.", amount) :
                "Failed to add money.";
        saveNotificationToHistory(message);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Ensure this icon exists
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveNotificationToHistory(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault());
        String dateTime = sdf.format(new Date());
        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        String existingJson = prefs.getString("notifications", "[]");
        try {
            JSONArray notificationsArray = new JSONArray(existingJson);
            JSONObject newNotification = new JSONObject();
            newNotification.put("message", message);
            newNotification.put("dateTime", dateTime);
            JSONArray newArray = new JSONArray();
            newArray.put(newNotification);
            for (int i = 0; i < notificationsArray.length() && i < 49; i++) {
                newArray.put(notificationsArray.getJSONObject(i));
            }
            prefs.edit().putString("notifications", newArray.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving notification to history", e);
        }
    }

    private void initiateUpiPayment(String packageName) {
        String amount = getValidRechargeAmount();
        if (amount == null) return;
        Uri uri = new Uri.Builder().scheme("upi").authority("pay")
                .appendQueryParameter("pa", TARGET_UPI_ID)
                .appendQueryParameter("pn", TARGET_PAYEE_NAME)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR").build();
        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);
        if (packageName != null) {
            upiPayIntent.setPackage(packageName);
            if (upiPayIntent.resolveActivity(getPackageManager()) == null) {
                showToast(packageName.substring(packageName.lastIndexOf('.') + 1) + " not installed.");
                return;
            }
            upiPaymentActivityResultLauncher.launch(upiPayIntent);
        } else {
            Intent chooser = Intent.createChooser(upiPayIntent, "Pay with UPI");
            if (chooser.resolveActivity(getPackageManager()) != null) {
                upiPaymentActivityResultLauncher.launch(chooser);
            } else {
                showToast("No UPI app found.");
            }
        }
    }

    private String extractValueFromUpiResponse(String response, String key) {
        if (response == null || key == null) return null;
        String[] params = response.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && key.equalsIgnoreCase(keyValue[0].trim())) {
                return keyValue[1].trim();
            }
        }
        return null;
    }

    private void handleUpiPaymentResult(androidx.activity.result.ActivityResult activityResult) {
        Log.d(TAG, "UPI Result Code: " + activityResult.getResultCode());
        if (activityResult.getResultCode() == Activity.RESULT_OK && activityResult.getData() != null) {
            Intent data = activityResult.getData();
            String responseString = data.getStringExtra("response");
            Log.d(TAG, "UPI Response: " + responseString);

            if (TextUtils.isEmpty(responseString)) {
                showToast("UPI response unclear.");
                sendNotification(false, 0);
                finish();
                return;
            }

            String status = extractValueFromUpiResponse(responseString, "Status");
            String txnIdFromResponse = extractValueFromUpiResponse(responseString, "txnId");
            if (txnIdFromResponse == null || txnIdFromResponse.isEmpty()) {
                txnIdFromResponse = extractValueFromUpiResponse(responseString, "ApprovalRefNo");
            }
            // Make txnId final for use in saveRechargeTransaction if needed by its inner lambdas
            final String finalTxnId = (txnIdFromResponse == null || txnIdFromResponse.isEmpty()) ? "UPI_FALLBACK_" + System.currentTimeMillis() : txnIdFromResponse;

            Log.d(TAG,"UPI Parsed Status: " + status + ", Parsed TxnID: " + finalTxnId);

            if (status != null && (status.equalsIgnoreCase("SUCCESS") || status.equalsIgnoreCase("SUBMITTED"))) {
                String amountStr = getValidRechargeAmount();
                if (amountStr != null) {
                    try {
                        final double finalRechargeAmount = Double.parseDouble(amountStr); // Final for lambda use
                        updateWalletBalance(amountStr, false);
                        sendNotification(true, (float) finalRechargeAmount);
                        // *** MODIFIED LINE (approx 329) ***
                        saveRechargeTransaction(finalTxnId, finalRechargeAmount, "Recharged via UPI");
                        showToast("Payment Successful! ₹" + amountStr + " added.");
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing amount: " + amountStr, e);
                        showToast("Payment successful, error processing amount.");
                    }
                } else {
                    Log.e(TAG, "UPI success, but amount was null.");
                    showToast("Payment successful, amount unclear for record.");
                }
            } else {
                String responseCode = extractValueFromUpiResponse(responseString, "responseCode");
                Log.e(TAG, "UPI payment not successful. Status: " + status + ", Code: " + responseCode);
                sendNotification(false, 0);
                showToast("UPI Payment Failed. Status: " + (status != null ? status : "N/A"));
            }
        } else {
            Log.w(TAG, "UPI payment cancelled or error. Result Code: " + activityResult.getResultCode());
            sendNotification(false, 0);
            showToast("UPI Payment Cancelled.");
        }
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Checkout.clearUserData(this);
    }
}