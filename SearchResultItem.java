package com.example.trivia.store;
public class SearchResultItem {
    private String symbol;
    private String name;
    private double price;

    public SearchResultItem(String symbol, String name, double price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }
}