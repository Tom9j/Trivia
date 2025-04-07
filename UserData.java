package com.example.trivia;

import com.example.trivia.store.StockHolding;

import java.util.HashMap;
import java.util.Map;

public class UserData {
    private int coins;
    private Map<String, StockHolding> stocks;

    public UserData() {
        // Required empty constructor for Firebase
        stocks = new HashMap<>();
    }

    public UserData(int coins, Map<String, StockHolding> stocks) {
        this.coins = coins;
        this.stocks = stocks != null ? stocks : new HashMap<>();
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public Map<String, StockHolding> getStocks() {
        return stocks;
    }

    public void setStocks(Map<String, StockHolding> stocks) {
        this.stocks = stocks;
    }

    // Calculate total portfolio value based on current prices
    public double calculatePortfolioValue(Map<String, Double> currentPrices) {
        double totalValue = 0;
        for (Map.Entry<String, StockHolding> entry : stocks.entrySet()) {
            String symbol = entry.getKey();
            StockHolding holding = entry.getValue();
            Double currentPrice = currentPrices.get(symbol);

            if (currentPrice != null) {
                totalValue += holding.getQuantity() * currentPrice;
            }
        }
        return totalValue;
    }

    // Add stock to portfolio
    public boolean buyStock(String symbol, int quantity, double price) {
        int cost = (int) (quantity * price);
        if (coins < cost) {
            return false; // Not enough coins
        }

        // Update coins
        coins -= cost;

        // Update stocks
        StockHolding holding = stocks.get(symbol);
        if (holding == null) {
            holding = new StockHolding(quantity, price);
            stocks.put(symbol, holding);
        } else {
            // Calculate new average purchase price
            int newQuantity = holding.getQuantity() + quantity;
            double newAvgPrice = ((holding.getQuantity() * holding.getAveragePurchasePrice())
                    + (quantity * price)) / newQuantity;
            holding.setQuantity(newQuantity);
            holding.setAveragePurchasePrice(newAvgPrice);
        }

        return true;
    }

    // Sell stock from portfolio
    public boolean sellStock(String symbol, int quantity, double price) {
        StockHolding holding = stocks.get(symbol);
        if (holding == null || holding.getQuantity() < quantity) {
            return false; // Not enough shares
        }

        // Update coins
        int earnings = (int) (quantity * price);
        coins += earnings;

        // Update stocks
        int newQuantity = holding.getQuantity() - quantity;
        if (newQuantity == 0) {
            stocks.remove(symbol);
        } else {
            holding.setQuantity(newQuantity);
        }

        return true;
    }
}