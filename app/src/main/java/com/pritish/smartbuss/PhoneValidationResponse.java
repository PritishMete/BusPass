package com.pritish.smartbuss;

public class PhoneValidationResponse {
    private boolean valid;
    private String country_name;
    private String country_code;
    private String location;
    private String carrier;

    public boolean isValid() {
        return valid;
    }

    public String getCountryName() {
        return country_name;
    }

    public String getLocation() {
        return location;
    }

    public String getCarrier() {
        return carrier;
    }
}

