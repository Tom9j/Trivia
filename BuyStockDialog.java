package com.example.trivia.store;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.trivia.R;

import java.text.NumberFormat;
import java.util.Locale;

public class BuyStockDialog extends DialogFragment {

    private String symbol;
    private String name;
    private double price;
    private int availableCoins;
    private BuyStockListener listener;

    public interface BuyStockListener {
        void onStockBought(String symbol, int quantity, double price);
    }

    public static BuyStockDialog newInstance(String symbol, String name, double price, int availableCoins) {
        BuyStockDialog dialog = new BuyStockDialog();
        Bundle args = new Bundle();
        args.putString("symbol", symbol);
        args.putString("name", name);
        args.putDouble("price", price);
        args.putInt("availableCoins", availableCoins);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(BuyStockListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            symbol = getArguments().getString("symbol");
            name = getArguments().getString("name");
            price = getArguments().getDouble("price");
            availableCoins = getArguments().getInt("availableCoins");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_buy_stock, null);

        TextView tvSymbol = view.findViewById(R.id.tv_dialog_symbol);
        TextView tvName = view.findViewById(R.id.tv_dialog_name);
        TextView tvPrice = view.findViewById(R.id.tv_dialog_price);
        TextView tvAvailableCoins = view.findViewById(R.id.tv_dialog_available_coins);
        EditText etQuantity = view.findViewById(R.id.et_dialog_quantity);
        TextView tvTotalCost = view.findViewById(R.id.tv_dialog_total_cost);
        Button btnBuy = view.findViewById(R.id.btn_dialog_buy);
        Button btnCancel = view.findViewById(R.id.btn_dialog_cancel);

        // הגדרת ערכים התחלתיים
        tvSymbol.setText(symbol);
        tvName.setText(name);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        tvPrice.setText(currencyFormat.format(price));
        tvAvailableCoins.setText(String.valueOf(availableCoins));

        // חישוב עלות כוללת כאשר הכמות משתנה
        etQuantity.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int quantity = s.length() > 0 ? Integer.parseInt(s.toString()) : 0;
                    double totalCost = quantity * price;
                    tvTotalCost.setText(currencyFormat.format(totalCost));

                    // הפעלה/השבתה של כפתור הקנייה בהתאם למטבעות זמינים
                    btnBuy.setEnabled(totalCost <= availableCoins && quantity > 0);
                } catch (NumberFormatException e) {
                    tvTotalCost.setText(currencyFormat.format(0));
                    btnBuy.setEnabled(false);
                }
            }
        });

        btnBuy.setOnClickListener(v -> {
            try {
                int quantity = Integer.parseInt(etQuantity.getText().toString());
                if (quantity > 0 && quantity * price <= availableCoins) {
                    if (listener != null) {
                        listener.onStockBought(symbol, quantity, price);
                    }
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), "כמות לא תקינה או אין מספיק מטבעות", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "הזן כמות תקינה", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view);
        return builder.create();
    }
}