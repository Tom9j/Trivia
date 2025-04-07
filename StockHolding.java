package com.example.trivia.store;
public class StockHolding {
    private int quantity;
    private double averagePurchasePrice;
    private String purchaseDate;

    public StockHolding(int quantity, double averagePurchasePrice) {
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.purchaseDate = "";
    }

    public StockHolding(int quantity, double averagePurchasePrice, String purchaseDate) {
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.purchaseDate = purchaseDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    // הוספת שיטות setter
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setAveragePurchasePrice(double averagePurchasePrice) {
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}