package com.pritish.smartbuss;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

public class razorpaypayment extends AppCompatActivity implements PaymentResultListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.razorpaypayment); // Create a blank layout for now

        // Initialize Razorpay Checkout
        Checkout.preload(getApplicationContext());

        // Retrieve the amount from Intent and validate
        float amount = getIntent().getFloatExtra("rechargeAmount", 0f); // Use getFloatExtra instead of getStringExtra

        if (amount <= 0) {
            Toast.makeText(this, "Invalid amount received", Toast.LENGTH_SHORT).show();
            return;  // Exit if amount is invalid
        }

        // Start Payment Process
        startPayment(amount);
    }

    private void startPayment(float amount) {
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_fs0h2eePIySWY0"); // Replace with your Razorpay Key ID

        try {
            // Convert the amount from INR to paise (1 INR = 100 paise)
            int amountInPaise = (int) (amount * 100); // Convert to integer in paise

            // If the amount is zero or negative, show an error
            if (amountInPaise <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject options = new JSONObject();
            options.put("name", "BussPass");
            options.put("description", "Wallet recharge");
            options.put("currency", "INR");
            options.put("amount", amountInPaise); // Amount in paise
            options.put("prefill.email", "test@example.com");
            options.put("prefill.contact", "9999999999");

            // Open Razorpay Checkout
            checkout.open(razorpaypayment.this, options);

        } catch (Exception e) {
            Log.e("Razorpay Error", "Error starting Razorpay Checkout: " + e.getMessage());
            Toast.makeText(this, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful! ID: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();
        Log.d("Payment Success", "Payment ID: " + razorpayPaymentID);
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, "Payment Failed: " + response, Toast.LENGTH_SHORT).show();
        Log.e("Payment Error", ", Response: " + response);
    }
}
