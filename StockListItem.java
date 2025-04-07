package com.example.trivia.store;
public class StockListItem {
    private String symbol;
    private int quantity;
    private double averagePurchasePrice;
    private double currentPrice;
    private String purchaseDate;  // זו התוספת החדשה - תאריך רכישה

    public StockListItem(String symbol, int quantity, double averagePurchasePrice, double currentPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentPrice = currentPrice;
        this.purchaseDate = "";
    }

    public StockListItem(String symbol, int quantity, double averagePurchasePrice, double currentPrice, String purchaseDate) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentPrice = currentPrice;
        this.purchaseDate = purchaseDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public double getProfitLoss() {
        return (currentPrice - averagePurchasePrice) * quantity;
    }

    public double getProfitLossPercentage() {
        if (averagePurchasePrice == 0) return 0;
        return ((currentPrice / averagePurchasePrice) - 1) * 100;
    }
}