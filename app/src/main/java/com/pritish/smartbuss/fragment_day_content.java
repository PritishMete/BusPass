package com.pritish.smartbuss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class fragment_day_content extends Fragment {
    private static final String ARG_DAY = "day";
    private static final String ARG_INCENTIVES = "incentives";

    public static fragment_day_content newInstance(String day, ArrayList<Incentive> incentives) {
        fragment_day_content fragment = new fragment_day_content();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        args.putSerializable(ARG_INCENTIVES, incentives);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_content, container, false);

        if (getArguments() == null) {
            return view; // Or handle error
        }

        String dayTitle = getArguments().getString(ARG_DAY);
        ArrayList<Incentive> incentives = (ArrayList<Incentive>) getArguments().getSerializable(ARG_INCENTIVES);

        TextView tvTitle = view.findViewById(R.id.tv_day_title);
        RecyclerView rvIncentives = view.findViewById(R.id.rv_incentives);
        TextView tvEmptyState = view.findViewById(R.id.tv_empty_state);
        LinearLayout layoutDailyTotal = view.findViewById(R.id.layout_daily_total);
        TextView tvDailyItemCount = view.findViewById(R.id.tv_daily_item_count); // Get reference to the new TextView
        TextView tvDailyTotalAmount = view.findViewById(R.id.tv_daily_total_amount);

        tvTitle.setText(dayTitle);

        if (incentives != null && !incentives.isEmpty()) {
            tvEmptyState.setVisibility(View.GONE);
            rvIncentives.setVisibility(View.VISIBLE);
            layoutDailyTotal.setVisibility(View.VISIBLE);

            rvIncentives.setLayoutManager(new LinearLayoutManager(getContext()));
            rvIncentives.setAdapter(new IncentiveAdapter(incentives)); // Your existing IncentiveAdapter

            // Calculate total amount
            double totalAmount = 0;
            for (Incentive incentive : incentives) {
                totalAmount += incentive.getAmount();
            }
            tvDailyTotalAmount.setText(String.format(Locale.getDefault(), "+₹%.2f", totalAmount));

            // Set item count
            int itemCount = incentives.size();
            tvDailyItemCount.setText(String.format(Locale.getDefault(), "(%d tickets)", itemCount)); // Display count

        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvIncentives.setVisibility(View.GONE);
            layoutDailyTotal.setVisibility(View.GONE);
        }
        return view;
    }
}