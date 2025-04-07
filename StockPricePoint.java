package com.example.trivia.store;
public class StockPricePoint {
    private String date;
    private double price;

    public StockPricePoint(String date, double price) {
        this.date = date;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public double getPrice() {
        return price;
    }
}