package com.pritish.smartbuss;

public class DailySummaryItem {
    public String dayDisplay; // e.g., "Mon, Apr 21"
    public double totalDailyIncentive;

    public DailySummaryItem(String dayDisplay, double totalDailyIncentive) {
        this.dayDisplay = dayDisplay;
        this.totalDailyIncentive = totalDailyIncentive;
    }

    // You might want getters if you make fields private
}