package com.example.trivia.store;
import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ApiService {
    // URL של השרת
    private static final String BASE_URL = "https://stocksapi-184402629474.europe-west3.run.app";
    private static ApiService instance;
    private RequestQueue requestQueue;
    private Context context;

    private ApiService(Context context) {
        this.context = context.getApplicationContext();
        requestQueue = Volley.newRequestQueue(this.context);
    }

    public static synchronized ApiService getInstance(Context context) {
        if (instance == null) {
            instance = new ApiService(context);
        }
        return instance;
    }

    /**
     * קבלת היסטוריית מחירים למניה
     */
    public void getStockHistory(String symbol, String range, String interval,
                                String startDate, String endDate,
                                final ApiResponseListener<List<StockPricePoint>> listener) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("symbol", symbol);
            requestBody.put("range", range);
            requestBody.put("interval", interval);

            // אם סופקו תאריכים ספציפיים, נוסיף אותם לבקשה
            if (startDate != null && endDate != null) {
                requestBody.put("start_date", startDate);
                requestBody.put("end_date", endDate);
            }
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/get_stock_history",
                requestBody,
                response -> {
                    try {
                        if (!response.has("status") || !"success".equals(response.getString("status"))) {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                            return;
                        }

                        String responseSymbol = response.getString("symbol");
                        List<StockPricePoint> pricePoints = new ArrayList<>();

                        // פענוח מערך המחירים מהתשובה
                        JSONArray pricesArray = response.getJSONArray("prices");
                        for (int i = 0; i < pricesArray.length(); i++) {
                            JSONObject point = pricesArray.getJSONObject(i);
                            String date = point.getString("date");
                            double price = point.getDouble("price");
                            pricePoints.add(new StockPricePoint(date, price));
                        }

                        listener.onSuccess(pricePoints);
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח התשובה: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * קבלת היסטוריית התיק
     */
    public void getPortfolioHistory(final ApiResponseListener<List<StockPricePoint>> listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onError("לא מחובר משתמש");
            return;
        }

        String userId = currentUser.getUid();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/get_portfolio_history",
                requestBody,
                response -> {
                    try {
                        if (!response.has("status") || !"success".equals(response.getString("status"))) {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                            return;
                        }

                        List<StockPricePoint> historyPoints = new ArrayList<>();

                        // פענוח היסטוריית הפורטפוליו
                        if (response.has("portfolio_history")) {
                            JSONArray historyArray = response.getJSONArray("portfolio_history");
                            for (int i = 0; i < historyArray.length(); i++) {
                                JSONObject point = historyArray.getJSONObject(i);
                                String date = point.getString("date");
                                double value = point.getDouble("value");
                                historyPoints.add(new StockPricePoint(date, value));
                            }
                        }

                        listener.onSuccess(historyPoints);
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח התשובה: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * קבלת מחיר עדכני למניה
     */
    public void getStockPrice(String symbol, final ApiResponseListener<Double> listener) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("symbol", symbol);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/get_stock_price_endpoint",
                requestBody,
                response -> {
                    try {
                        if (!response.has("status") || !"success".equals(response.getString("status"))) {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                            return;
                        }

                        double price = response.getDouble("price");
                        listener.onSuccess(price);
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח התשובה: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * חיפוש מניות לפי מילת מפתח
     */
    public void searchStocks(String query, final ApiResponseListener<List<SearchResultItem>> listener) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("query", query);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/search_stocks",
                requestBody,
                response -> {
                    try {
                        if (!response.has("status") || !"success".equals(response.getString("status"))) {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                            return;
                        }

                        List<SearchResultItem> results = new ArrayList<>();
                        JSONArray resultsArray = response.getJSONArray("results");

                        for (int i = 0; i < resultsArray.length(); i++) {
                            JSONObject stock = resultsArray.getJSONObject(i);
                            String symbol = stock.getString("symbol");
                            String name = stock.getString("name");
                            double price = stock.getDouble("price");

                            results.add(new SearchResultItem(symbol, name, price));
                        }

                        listener.onSuccess(results);
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח תוצאות החיפוש: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * קנייה או מכירה של מניה
     */
    public void handleStockTransaction(String symbol, String action, int amount, double price,
                                       final ApiResponseListener<TransactionResult> listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onError("לא מחובר משתמש");
            return;
        }

        String userId = currentUser.getUid();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
            requestBody.put("symbol", symbol);
            requestBody.put("action", action);
            requestBody.put("amount", amount);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/handle_stock_request",
                requestBody,
                response -> {
                    try {
                        String status = response.optString("status");
                        String message = response.optString("message", "");

                        if ("success".equals(status)) {
                            int newBalance = response.optInt("new_balance", 0);
                            int holdings = response.optInt("holdings", 0);
                            double transactionPrice = response.optDouble("purchased_at", price);
                            String transactionDate = response.optString("purchase_date", "");

                            if (action.equals("sell")) {
                                transactionPrice = response.optDouble("sold_at", price);
                                transactionDate = response.optString("sell_date", "");
                            }

                            TransactionResult result = new TransactionResult(
                                    symbol,
                                    action,
                                    amount,
                                    transactionPrice,
                                    transactionPrice * amount,
                                    newBalance,
                                    holdings,
                                    message,
                                    transactionDate
                            );

                            listener.onSuccess(result);
                        } else {
                            listener.onError(message);
                        }
                    } catch (Exception e) {
                        listener.onError("שגיאה בפענוח תשובת השרת: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * קבלת נתוני משתמש מעודכנים
     */
    public void getUserData(final ApiResponseListener<UserDataResult> listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onError("לא מחובר משתמש");
            return;
        }

        String userId = currentUser.getUid();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/get_user_data",
                requestBody,
                response -> {
                    try {
                        if (!response.has("status") || !"success".equals(response.getString("status"))) {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                            return;
                        }

                        int coins = response.getInt("coins");
                        JSONObject portfolioSummary = response.getJSONObject("portfolio_summary");
                        JSONObject portfolioDetails = response.getJSONObject("portfolio_details");
                        JSONObject pricesJson = response.getJSONObject("current_prices");
                        double portfolioValue = response.getDouble("portfolio_value");
                        JSONArray transactionsJson = response.getJSONArray("transactions");

                        // המרת סיכום תיק ההשקעות למבנה הנתונים שלנו
                        Map<String, StockHolding> portfolio = new HashMap<>();
                        Map<String, Double> currentPrices = new HashMap<>();
                        List<Transaction> transactions = new ArrayList<>();

                        // פענוח סיכום תיק ההשקעות
                        for (Iterator<String> it = portfolioSummary.keys(); it.hasNext();) {
                            String symbol = it.next();
                            JSONObject stockInfo = portfolioSummary.getJSONObject(symbol);

                            int quantity = stockInfo.getInt("quantity");
                            double avgPrice = stockInfo.getDouble("avg_price");
                            String earliestPurchase = stockInfo.optString("earliest_purchase", "");

                            if (quantity > 0) {
                                portfolio.put(symbol, new StockHolding(quantity, avgPrice, earliestPurchase));

                                // אם יש מחיר נוכחי למניה זו
                                if (pricesJson.has(symbol)) {
                                    currentPrices.put(symbol, pricesJson.getDouble(symbol));
                                }
                            }
                        }

                        // פענוח היסטוריית עסקאות
                        for (int i = 0; i < transactionsJson.length(); i++) {
                            JSONObject tx = transactionsJson.getJSONObject(i);

                            Transaction transaction = new Transaction(
                                    tx.getString("symbol"),
                                    tx.getString("action"),
                                    tx.getInt("quantity"),
                                    tx.getDouble("price"),
                                    tx.getDouble(tx.has("total_cost") ? "total_cost" : "total_value"),
                                    tx.getString("date")
                            );

                            transactions.add(transaction);
                        }

                        UserDataResult result = new UserDataResult(
                                coins,
                                portfolio,
                                currentPrices,
                                portfolioValue,
                                transactions
                        );

                        listener.onSuccess(result);
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח נתוני משתמש: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * איתחול משתמש חדש
     */
    public void initializeUser(final ApiResponseListener<Boolean> listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onError("לא מחובר משתמש");
            return;
        }

        String userId = currentUser.getUid();

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
        } catch (JSONException e) {
            listener.onError(e.getMessage());
            return;
        }

        makeApiCall(
                Request.Method.POST,
                "/initialize_user",
                requestBody,
                response -> {
                    try {
                        if (response.has("status") && "success".equals(response.getString("status"))) {
                            listener.onSuccess(true);
                        } else {
                            listener.onError(response.optString("message", "שגיאה לא ידועה"));
                        }
                    } catch (JSONException e) {
                        listener.onError("שגיאה בפענוח תשובת השרת: " + e.getMessage());
                    }
                },
                error -> listener.onError(getErrorMessage(error))
        );
    }

    /**
     * פונקציית עזר לביצוע קריאות API
     */
    private void makeApiCall(int method, String endpoint, JSONObject requestBody,
                             Response.Listener<JSONObject> successListener,
                             Response.ErrorListener errorListener) {
        JsonObjectRequest request = new JsonObjectRequest(
                method,
                BASE_URL + endpoint,
                requestBody,
                successListener,
                errorListener
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    /**
     * פונקציית עזר להפקת הודעת שגיאה מתאימה
     */
    private String getErrorMessage(VolleyError error) {
        String errorMessage = "שגיאת רשת";

        if (error.networkResponse != null) {
            errorMessage += " (קוד: " + error.networkResponse.statusCode + ")";

            // ניסיון לקבל את גוף התשובה עבור פרטים נוספים
            if (error.networkResponse.data != null) {
                try {
                    String responseBody = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorJson = new JSONObject(responseBody);
                    if (errorJson.has("message")) {
                        errorMessage += ": " + errorJson.getString("message");
                    }
                } catch (Exception e) {
                    // התעלם משגיאות פענוח תוכן השגיאה
                }
            }
        }

        if (error.getMessage() != null) {
            errorMessage += "\n" + error.getMessage();
        }

        return errorMessage;
    }

    /**
     * ממשק לתגובות API
     */
    public interface ApiResponseListener<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    /**
     * מחלקה לתוצאת עסקה
     */
    public static class TransactionResult {
        private String symbol;
        private String action;
        private int quantity;
        private double price;
        private double totalCost;
        private int newBalance;
        private int holdings;
        private String message;
        private String transactionDate;

        public TransactionResult(String symbol, String action, int quantity, double price,
                                 double totalCost, int newBalance, int holdings,
                                 String message, String transactionDate) {
            this.symbol = symbol;
            this.action = action;
            this.quantity = quantity;
            this.price = price;
            this.totalCost = totalCost;
            this.newBalance = newBalance;
            this.holdings = holdings;
            this.message = message;
            this.transactionDate = transactionDate;
        }

        public String getSymbol() { return symbol; }
        public String getAction() { return action; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getTotalCost() { return totalCost; }
        public int getNewBalance() { return newBalance; }
        public int getHoldings() { return holdings; }
        public String getMessage() { return message; }
        public String getTransactionDate() { return transactionDate; }
    }

    /**
     * מחלקה לנתוני משתמש
     */
    public static class UserDataResult {
        private int coins;
        private Map<String, StockHolding> portfolio;
        private Map<String, Double> currentPrices;
        private double portfolioValue;
        private List<Transaction> transactions;

        public UserDataResult(int coins, Map<String, StockHolding> portfolio,
                              Map<String, Double> currentPrices, double portfolioValue,
                              List<Transaction> transactions) {
            this.coins = coins;
            this.portfolio = portfolio;
            this.currentPrices = currentPrices;
            this.portfolioValue = portfolioValue;
            this.transactions = transactions;
        }

        public int getCoins() { return coins; }
        public Map<String, StockHolding> getPortfolio() { return portfolio; }
        public Map<String, Double> getCurrentPrices() { return currentPrices; }
        public double getPortfolioValue() { return portfolioValue; }
        public List<Transaction> getTransactions() { return transactions; }
    }

    /**
     * מחלקה לעסקה בודדת
     */
    public static class Transaction {
        private String symbol;
        private String action;
        private int quantity;
        private double price;
        private double totalValue;
        private String date;

        public Transaction(String symbol, String action, int quantity,
                           double price, double totalValue, String date) {
            this.symbol = symbol;
            this.action = action;
            this.quantity = quantity;
            this.price = price;
            this.totalValue = totalValue;
            this.date = date;
        }

        public String getSymbol() { return symbol; }
        public String getAction() { return action; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getTotalValue() { return totalValue; }
        public String getDate() { return date; }
    }
}