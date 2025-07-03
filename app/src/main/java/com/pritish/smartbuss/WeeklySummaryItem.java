package com.pritish.smartbuss;

import java.time.LocalDate; // If you store LocalDate
import java.util.List;

public class WeeklySummaryItem {
    public String weekRangeDisplay;
    public double totalWeeklyIncentive;
    public LocalDate weekStartDate; // To identify the week
    public List<DailySummaryItem> dailyBreakdown;

    public WeeklySummaryItem(String weekRangeDisplay, double totalWeeklyIncentive, LocalDate weekStartDate, List<DailySummaryItem> dailyBreakdown) {
        this.weekRangeDisplay = weekRangeDisplay;
        this.totalWeeklyIncentive = totalWeeklyIncentive;
        this.weekStartDate = weekStartDate;
        this.dailyBreakdown = dailyBreakdown;
    }

    // You might want getters if you make fields private
}