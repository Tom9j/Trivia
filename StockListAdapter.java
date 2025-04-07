package com.example.trivia.store;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trivia.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StockListAdapter extends RecyclerView.Adapter<StockListAdapter.StockViewHolder> {

    private final List<StockListItem> stockList;
    private final OnStockClickListener clickListener;
    private final OnSellRequestListener sellListener;

    public interface OnStockClickListener {
        void onStockClick(StockListItem stock);
    }

    public interface OnSellRequestListener {
        void onSellRequest(String symbol, int quantity, double price);
    }

    public StockListAdapter(List<StockListItem> stockList,
                            OnStockClickListener clickListener,
                            OnSellRequestListener sellListener) {
        this.stockList = stockList;
        this.clickListener = clickListener;
        this.sellListener = sellListener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        StockListItem stock = stockList.get(position);
        holder.bind(stock, clickListener, sellListener);
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSymbol;
        private final TextView tvQuantity;
        private final TextView tvAvgPrice;
        private final TextView tvCurrentPrice;
        private final TextView tvProfitLoss;
        private final TextView tvProfitLossPercent;
        private final Button btnSell;
        private final Button btnDetails;

        public StockViewHolder(View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tv_stock_symbol);
            tvQuantity = itemView.findViewById(R.id.tv_stock_quantity);
            tvAvgPrice = itemView.findViewById(R.id.tv_avg_price);
            tvCurrentPrice = itemView.findViewById(R.id.tv_current_price);
            tvProfitLoss = itemView.findViewById(R.id.tv_profit_loss);
            tvProfitLossPercent = itemView.findViewById(R.id.tv_profit_loss_percent);
            btnSell = itemView.findViewById(R.id.btn_sell);
            btnDetails = itemView.findViewById(R.id.btn_details);
        }

        public void bind(StockListItem stock,
                         OnStockClickListener clickListener,
                         OnSellRequestListener sellListener) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.getDefault());
            percentFormat.setMaximumFractionDigits(2);

            tvSymbol.setText(stock.getSymbol());
            tvQuantity.setText(String.valueOf(stock.getQuantity()) + " מניות");
            tvAvgPrice.setText(currencyFormat.format(stock.getAveragePurchasePrice()));
            tvCurrentPrice.setText(currencyFormat.format(stock.getCurrentPrice()));

            double profitLoss = stock.getProfitLoss();
            double profitLossPercent = stock.getProfitLossPercentage();

            tvProfitLoss.setText(currencyFormat.format(profitLoss));
            tvProfitLossPercent.setText(percentFormat.format(profitLossPercent / 100)); // המרה לעשרוני עבור פורמט אחוזים

            // הגדרת צבע בהתאם לרווח/הפסד
            int color = profitLoss >= 0 ? Color.GREEN : Color.RED;
            tvProfitLoss.setTextColor(color);
            tvProfitLossPercent.setTextColor(color);

            // הגדרת מאזינים לאירועים
            itemView.setOnClickListener(v -> clickListener.onStockClick(stock));

            btnSell.setOnClickListener(v -> {
                showSellDialog(v.getContext(), stock, sellListener);
            });

            btnDetails.setOnClickListener(v -> {
                // ניתן להרחיב להצגת פרטים נוספים
                clickListener.onStockClick(stock);
            });
        }

        // הצגת דיאלוג מכירת מניות
        private void showSellDialog(Context context, StockListItem stock, OnSellRequestListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_sell_stock, null);

            TextView tvSymbol = dialogView.findViewById(R.id.tv_dialog_symbol);
            TextView tvCurrentPrice = dialogView.findViewById(R.id.tv_dialog_price);
            TextView tvOwnedQuantity = dialogView.findViewById(R.id.tv_dialog_owned_quantity);
            EditText etQuantity = dialogView.findViewById(R.id.et_dialog_quantity);
            TextView tvTotalValue = dialogView.findViewById(R.id.tv_dialog_total_value);
            Button btnSellAll = dialogView.findViewById(R.id.btn_sell_all);

            // הגדרת ערכים התחלתיים
            tvSymbol.setText(stock.getSymbol());
            tvCurrentPrice.setText(NumberFormat.getCurrencyInstance().format(stock.getCurrentPrice()));
            tvOwnedQuantity.setText(String.valueOf(stock.getQuantity()));

            // עדכון ערך כולל כאשר הכמות משתנה
            etQuantity.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int quantity = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                        double totalValue = quantity * stock.getCurrentPrice();
                        tvTotalValue.setText(NumberFormat.getCurrencyInstance().format(totalValue));
                    } catch (NumberFormatException e) {
                        tvTotalValue.setText(NumberFormat.getCurrencyInstance().format(0));
                    }
                }
            });

            // הגדרת כפתור "מכור הכל"
            btnSellAll.setOnClickListener(v -> {
                etQuantity.setText(String.valueOf(stock.getQuantity()));
            });

            builder.setView(dialogView)
                    .setTitle("מכור מניות")
                    .setPositiveButton("מכור", (dialog, which) -> {
                        try {
                            int quantity = Integer.parseInt(etQuantity.getText().toString());
                            if (quantity > 0 && quantity <= stock.getQuantity()) {
                                listener.onSellRequest(stock.getSymbol(), quantity, stock.getCurrentPrice());
                            } else {
                                Toast.makeText(context, "כמות לא תקינה", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "הזן כמות תקינה", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        }
    }
}