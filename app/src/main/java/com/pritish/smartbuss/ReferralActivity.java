package com.pritish.smartbuss;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReferralActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        // Back button

        // Set referral code (replace with your logic)
        TextView referralCode = findViewById(R.id.referral_code);
        referralCode.setText("BUSPASS25"); // Example code
    }
}