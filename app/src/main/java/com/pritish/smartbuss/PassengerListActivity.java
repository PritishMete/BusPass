package com.pritish.smartbuss;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class PassengerListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PassengerAdapter passengerAdapter;
    private List<Passenger> passengerList;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conductor_passanger_list);

        // Initialize RecyclerView and Firebase reference
        recyclerView = findViewById(R.id.passengerRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        passengerList = new ArrayList<>();
        passengerAdapter = new PassengerAdapter(passengerList);
        recyclerView.setAdapter(passengerAdapter);

        // Initialize Firebase database reference
        databaseReference = FirebaseDatabase.getInstance("https://smartbuss-8c1cb-default-rtdb.firebaseio.com/")
                .getReference("Bookings");

        // Fetch data from Firebase
        fetchPassengerData();
    }

    private void fetchPassengerData() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                passengerList.clear(); // Clear previous data

                // Iterate through all the BookingIDs under "Bookings"
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Get the passenger data (price, route, transactionID) under each BookingID
                    String price = snapshot.child("price").getValue(String.class);
                    String route = snapshot.child("route").getValue(String.class);
                    String transactionID = snapshot.child("transactionID").getValue(String.class);

                    if (price != null && route != null && transactionID != null) {
                        // Create a new Passenger object and add it to the list
                        Passenger passenger = new Passenger(price, route, transactionID);
                        passengerList.add(passenger);
                    }
                }

                // Notify the adapter to update the RecyclerView
                passengerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors while fetching data
                Log.e("FirebaseError", "Error fetching data: " + databaseError.getMessage());
            }
        });
    }
}
