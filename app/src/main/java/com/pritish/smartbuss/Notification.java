package com.pritish.smartbuss;

public class Notification {
    private String message;
    private String dateTime;

    public Notification(String message, String dateTime) {
        this.message = message;
        this.dateTime = dateTime;
    }

    public String getMessage() {
        return message;
    }

    public String getDateTime() {
        return dateTime;
    }
}
