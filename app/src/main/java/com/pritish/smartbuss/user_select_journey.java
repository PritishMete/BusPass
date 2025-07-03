package com.pritish.smartbuss;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class user_select_journey extends AppCompatActivity
        implements JourneyAdapter.OnItemClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_select_journey);

        // Get data from intent
        String startLocation = getIntent().getStringExtra("selectedStart");
        String endLocation = getIntent().getStringExtra("selectedStop");
        String routeNumber = getIntent().getStringExtra("routeName");

        // Create journey data
        List<Journey> journeyList = new ArrayList<>();
        journeyList.add(new Journey(
                routeNumber != null ? routeNumber : "S12",
                "VALID",
                startLocation != null ? startLocation : "CIT More",
                endLocation != null ? endLocation : "Howrah Railway Station",
                "12:17:44",
                "₹32.84"
        ));

        // Set up RecyclerView
        RecyclerView recyclerView = findViewById(R.id.routeRecyclerView);
        JourneyAdapter adapter = new JourneyAdapter(this, journeyList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onItemClick(Journey journey, View view) {
        // Handle card click - navigate to bus_location activity
        Intent intent = new Intent(this, bus_location.class);

        // Pass all journey data
        intent.putExtra("routeNumber", journey.getRouteNumber());
        intent.putExtra("status", journey.getStatus());
        intent.putExtra("startLocation", journey.getStartLocation());
        intent.putExtra("endLocation", journey.getEndLocation());
        intent.putExtra("time", journey.getTime());
        intent.putExtra("price", journey.getPrice());

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}