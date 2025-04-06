package com.example.trivia;

public class User {
    public String email;
    public String password; // לא מאובטח! עדיף להצפין

    public User() {
        // נדרש על ידי Firebase
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
