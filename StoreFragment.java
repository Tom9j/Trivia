package com.example.trivia.store;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.trivia.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StoreFragment extends Fragment implements BuyStockDialog.BuyStockListener {

    private TextView tvPortfolioValue;
    private TextView tvDailyChange;
    private TextView tvWeeklyChange;
    private SimpleLineChartView portfolioChart;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton fabQuickTrade;
    private View loadingView;

    private ApiService apiService;
    private ApiService.UserDataResult userData;
    private PortfolioTabFragment portfolioTabFragment;
    private StorePagerAdapter pagerAdapter;
    private List<StockPricePoint> portfolioHistory;
    private boolean isActive = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_store, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isActive = true;
        apiService = ApiService.getInstance(requireContext());

        initViews(view);
        setupTabLayout();
        setupFab();

        // בדיקה שהמשתמש מחובר
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "אנא התחבר כדי לצפות בנתונים", Toast.LENGTH_SHORT).show();
            return;
        }

        // איתחול משתמש אם צריך ולאחר מכן טעינת נתונים
        initializeUserIfNeeded();
    }

    private void initViews(View view) {
        try {
            tvPortfolioValue = view.findViewById(R.id.tv_portfolio_value);
            tvDailyChange = view.findViewById(R.id.tv_daily_change);
            tvWeeklyChange = view.findViewById(R.id.tv_weekly_change);

            portfolioChart = view.findViewById(R.id.portfolio_chart);
            tabLayout = view.findViewById(R.id.tab_layout);
            viewPager = view.findViewById(R.id.view_pager);
            fabQuickTrade = view.findViewById(R.id.fab_quick_trade);
            loadingView = view.findViewById(R.id.loading_view);

            // ניתן להוסיף לוג לבדיקה שהכל אותחל נכון
            if (tvPortfolioValue == null) {
                Log.e("StoreFragment", "tvPortfolioValue is null");
            }
            if (tvDailyChange == null) {
                Log.e("StoreFragment", "tvDailyChange is null");
            }
            if (tvWeeklyChange == null) {
                Log.e("StoreFragment", "tvWeeklyChange is null");
            }

        } catch (Exception e) {
            Log.e("StoreFragment", "Error initializing views", e);
        }
    }

    private void setupTabLayout() {
        if (viewPager == null || tabLayout == null) {
            Log.e("StoreFragment", "viewPager or tabLayout is null");
            return;
        }

        pagerAdapter = new StorePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("התיק שלי");
                    break;
                case 1:
                    tab.setText("חקר מניות");
                    break;
                case 2:
                    tab.setText("מובילים");
                    break;
            }
        }).attach();
    }

    private void initializeUserIfNeeded() {
        showLoading(true);

        apiService.initializeUser(new ApiService.ApiResponseListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (!isActive) return;
                // המשתמש אותחל או היה קיים כבר, טען את הנתונים
                loadUserData();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isActive) return;
                showLoading(false);
                Toast.makeText(requireContext(), "שגיאה באיתחול המשתמש: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData() {
        showLoading(true);

        apiService.getUserData(new ApiService.ApiResponseListener<ApiService.UserDataResult>() {
            @Override
            public void onSuccess(ApiService.UserDataResult result) {
                if (!isActive) return;

                userData = result;
                updateUI();
                updatePortfolioTab();
                fetchPortfolioHistory();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isActive) return;

                showLoading(false);
                Toast.makeText(requireContext(), "שגיאה בטעינת נתוני משתמש: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (userData == null || !isActive) return;

        try {
            // עדכון מטבעות

            // עדכון ערך תיק
            if (tvPortfolioValue != null) {
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                tvPortfolioValue.setText(currencyFormat.format(userData.getPortfolioValue()));
            }
        } catch (Exception e) {
            Log.e("StoreFragment", "Error in updateUI", e);
        }
    }

    private void updatePortfolioTab() {
        if (!isActive || userData == null) return;

        try {
            // קבלת התייחסות לפרגמנט תיק ההשקעות
            if (portfolioTabFragment == null && pagerAdapter != null) {
                portfolioTabFragment = (PortfolioTabFragment) pagerAdapter.getFragment(0);
            }

            if (portfolioTabFragment != null) {
                // העבר את המידע המעודכן על תיק ההשקעות לפרגמנט הרלוונטי
                portfolioTabFragment.updateStockList(userData.getPortfolio(), userData.getCurrentPrices());
            }
        } catch (Exception e) {
            Log.e("StoreFragment", "Error in updatePortfolioTab", e);
        }
    }

    private void fetchPortfolioHistory() {
        if (!isActive) return;

        // קבלת היסטוריית התיק באמצעות ה-API החדש
        apiService.getPortfolioHistory(new ApiService.ApiResponseListener<List<StockPricePoint>>() {
            @Override
            public void onSuccess(List<StockPricePoint> historyPoints) {
                if (!isActive) return;

                showLoading(false);
                portfolioHistory = historyPoints;
                updatePortfolioChart();
                calculateReturns();
            }

            @Override
            public void onError(String errorMessage) {
                if (!isActive) return;

                showLoading(false);
                Toast.makeText(requireContext(), "שגיאה בקבלת היסטוריית התיק: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePortfolioChart() {
        if (!isActive || portfolioHistory == null || portfolioHistory.isEmpty() || portfolioChart == null) {
            return;
        }

        try {
            // המר את נתוני ההיסטוריה לפורמט המתאים לגרף
            ArrayList<Float> chartData = new ArrayList<>();
            for (StockPricePoint point : portfolioHistory) {
                chartData.add((float) point.getPrice());
            }

            // הגדרת הנתונים בגרף
            portfolioChart.setDataPoints(chartData);
        } catch (Exception e) {
            Log.e("StoreFragment", "Error in updatePortfolioChart", e);
        }
    }

    private void calculateReturns() {
        if (!isActive || portfolioHistory == null || portfolioHistory.isEmpty()) {
            if (tvDailyChange != null) tvDailyChange.setText("0.00%");
            if (tvWeeklyChange != null) tvWeeklyChange.setText("0.00%");
            return;
        }

        try {
            // קבלת ערך התיק הנוכחי
            double currentValue = userData.getPortfolioValue();

            // חיפוש נתוני היסטוריה של יום קודם ושבוע קודם
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            String lastWeek = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());

            // מציאת ערכים מתאימים בהיסטוריה
            Double yesterdayValue = null;
            Double lastWeekValue = null;

            for (StockPricePoint point : portfolioHistory) {
                if (point.getDate().equals(yesterday)) {
                    yesterdayValue = point.getPrice();
                }
                if (point.getDate().equals(lastWeek)) {
                    lastWeekValue = point.getPrice();
                }
            }

            // חישוב השינוי היומי
            if (tvDailyChange != null) {
                if (yesterdayValue != null && yesterdayValue > 0) {
                    double dailyChange = ((currentValue / yesterdayValue) - 1) * 100;
                    updateChangeText(tvDailyChange, dailyChange);
                } else {
                    tvDailyChange.setText("--");
                }
            }

            // חישוב השינוי השבועי
            if (tvWeeklyChange != null) {
                if (lastWeekValue != null && lastWeekValue > 0) {
                    double weeklyChange = ((currentValue / lastWeekValue) - 1) * 100;
                    updateChangeText(tvWeeklyChange, weeklyChange);
                } else {
                    tvWeeklyChange.setText("--");
                }
            }
        } catch (Exception e) {
            Log.e("StoreFragment", "Error in calculateReturns", e);
        }
    }

    private void updateChangeText(TextView textView, double changePercent) {
        if (textView == null || !isActive) return;

        try {
            String prefix = changePercent >= 0 ? "+" : "";
            String formattedChange = String.format(Locale.US, "%s%.2f%%", prefix, changePercent);
            textView.setText(formattedChange);
            textView.setTextColor(getResources().getColor(
                    changePercent >= 0 ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));
        } catch (Exception e) {
            Log.e("StoreFragment", "Error in updateChangeText", e);
        }
    }

    private void setupFab() {
        if (fabQuickTrade == null) return;

        fabQuickTrade.setOnClickListener(v -> {
            showQuickTradeDialog();
        });
    }

    private void showQuickTradeDialog() {
        if (!isActive) return;

        // הצגת דיאלוג לחיפוש וקניית מניה מהיר
        QuickTradeDialog dialog = new QuickTradeDialog();
        dialog.setListener(new QuickTradeDialog.QuickTradeListener() {
            @Override
            public void onSymbolSelected(String symbol, String name, double price) {
                if (!isActive) return;
                showBuyDialog(symbol, name, price);
            }
        });
        dialog.show(getChildFragmentManager(), "quick_trade_dialog");
    }

    public void showBuyDialog(String symbol, String name, double price) {
        if (!isActive) return;

        BuyStockDialog dialog = BuyStockDialog.newInstance(
                symbol,
                name,
                price,
                userData != null ? userData.getCoins() : 0
        );
        dialog.setListener(this);
        dialog.show(getChildFragmentManager(), "buy_dialog");
    }

    @Override
    public void onStockBought(String symbol, int quantity, double price) {
        if (!isActive) return;

        showLoading(true);

        apiService.handleStockTransaction(
                symbol,
                "buy",
                quantity,
                price,
                new ApiService.ApiResponseListener<ApiService.TransactionResult>() {
                    @Override
                    public void onSuccess(ApiService.TransactionResult result) {
                        if (!isActive) return;

                        // רענון נתוני המשתמש לאחר רכישה
                        Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                        loadUserData();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isActive) return;

                        showLoading(false);
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void sellStock(String symbol, int quantity, double price) {
        if (!isActive) return;

        showLoading(true);

        apiService.handleStockTransaction(
                symbol,
                "sell",
                quantity,
                price,
                new ApiService.ApiResponseListener<ApiService.TransactionResult>() {
                    @Override
                    public void onSuccess(ApiService.TransactionResult result) {
                        if (!isActive) return;

                        // רענון נתוני המשתמש לאחר מכירה
                        Toast.makeText(requireContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                        loadUserData();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isActive) return;

                        showLoading(false);
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showLoading(boolean show) {
        if (loadingView != null && isActive) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isActive = true;

        // רענון נתונים בכל חזרה למסך
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadUserData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isActive = false;

        // שחרור המשאבים
        tvPortfolioValue = null;
        tvDailyChange = null;
        tvWeeklyChange = null;
        portfolioChart = null;
        tabLayout = null;
        viewPager = null;
        fabQuickTrade = null;
        loadingView = null;
    }
}