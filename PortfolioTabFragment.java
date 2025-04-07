package com.example.trivia.store;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trivia.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PortfolioTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyPortfolio;
    private StockListAdapter adapter;
    private List<StockListItem> stockList = new ArrayList<>();

    // שדות לטיפול בעדכונים דחויים
    private Map<String, StockHolding> pendingStocks;
    private Map<String, Double> pendingPrices;
    private boolean viewsInitialized = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_portfolio_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_stocks);
        tvEmptyPortfolio = view.findViewById(R.id.tv_empty_portfolio);

        setupRecyclerView();

        // סימון שהתצוגות אותחלו
        viewsInitialized = true;

        // טיפול בעדכונים שנדחו
        if (pendingStocks != null && pendingPrices != null) {
            updateStockList(pendingStocks, pendingPrices);
            pendingStocks = null;
            pendingPrices = null;
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StockListAdapter(stockList, this::onStockClicked, this::onSellRequested);
        recyclerView.setAdapter(adapter);
    }

    private void onStockClicked(StockListItem stock) {
        // טיפול בלחיצה על מניה - אולי להציג דיאלוג פרטים
    }

    private void onSellRequested(String symbol, int quantity, double price) {
        // העבר את בקשת המכירה לפרגמנט האב
        if (getParentFragment() instanceof StoreFragment) {
            ((StoreFragment) getParentFragment()).sellStock(symbol, quantity, price);
        }
    }

    // עדכון רשימת המניות
    public void updateStockList(Map<String, StockHolding> stocks, Map<String, Double> currentPrices) {
        // אם התצוגות לא אותחלו עדיין, שמור את הנתונים לעדכון מאוחר יותר
        if (!viewsInitialized || recyclerView == null || tvEmptyPortfolio == null) {
            this.pendingStocks = stocks;
            this.pendingPrices = currentPrices;
            return;
        }

        stockList.clear();

        if (stocks == null || stocks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyPortfolio.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        tvEmptyPortfolio.setVisibility(View.GONE);

        for (Map.Entry<String, StockHolding> entry : stocks.entrySet()) {
            String symbol = entry.getKey();
            StockHolding holding = entry.getValue();
            Double currentPrice = currentPrices != null ? currentPrices.get(symbol) : null;

            if (currentPrice != null && holding.getQuantity() > 0) {
                StockListItem item = new StockListItem(
                        symbol,
                        holding.getQuantity(),
                        holding.getAveragePurchasePrice(),
                        currentPrice
                );
                stockList.add(item);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewsInitialized = false;
        recyclerView = null;
        tvEmptyPortfolio = null;
        adapter = null;
    }
}