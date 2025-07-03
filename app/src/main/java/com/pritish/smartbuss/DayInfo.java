package com.pritish.smartbuss;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DayInfo {
    public final String dayNumber, monthAbbr, dayNameAbbr;
    public final LocalDate date;

    public DayInfo(String dayNumber, String monthAbbr, String dayNameAbbr, LocalDate date) {
        this.dayNumber = dayNumber;
        this.monthAbbr = monthAbbr;
        this.dayNameAbbr = dayNameAbbr;
        this.date = date;
    }

    public String getFullDateTitle() {
        return date.format(DateTimeFormatter.ofPattern("dd MMMM, EEE", Locale.getDefault()));
    }
}