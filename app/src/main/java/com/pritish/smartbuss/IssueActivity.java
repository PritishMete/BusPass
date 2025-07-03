package com.pritish.smartbuss;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class IssueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue);

        // Back button

        // Initialize issue type spinner
        Spinner issueTypeSpinner = findViewById(R.id.issue_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.issue_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        issueTypeSpinner.setAdapter(adapter);

        // Submit button
        Button submitButton = findViewById(R.id.btn_submit);
        submitButton.setOnClickListener(v -> {
            if (validateForm()) {
                submitIssue();
            }
        });
    }

    private boolean validateForm() {
        EditText descriptionEditText = findViewById(R.id.issue_description);
        if (descriptionEditText.getText().toString().trim().isEmpty()) {
            descriptionEditText.setError("Please describe your issue");
            return false;
        }
        return true;
    }

    private void submitIssue() {
        // Implement your issue submission logic here
        Toast.makeText(this, "Issue submitted successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}