package com.pritish.smartbuss;

public class RegUserDet {
    private String name;
    private String phone;
    private String password; // This will store the hashed password
    private String salt;     // Salt for password hashing

    // Default constructor required for Firebase
    public RegUserDet() {
    }

    // Constructor with all fields
    public RegUserDet(String name, String phone, String password, String salt) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.salt = salt;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}