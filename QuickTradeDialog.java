package com.example.trivia.store;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.trivia.R;

import java.util.List;

public class QuickTradeDialog extends DialogFragment {

    private EditText etSymbol;
    private Button btnSearch;
    private ProgressBar progressBar;
    private QuickTradeListener listener;
    private ApiService apiService;

    public interface QuickTradeListener {
        void onSymbolSelected(String symbol, String name, double price);
    }

    public void setListener(QuickTradeListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_quick_trade, null);

        apiService = ApiService.getInstance(requireContext());

        etSymbol = view.findViewById(R.id.et_symbol);
        btnSearch = view.findViewById(R.id.btn_search);
        progressBar = view.findViewById(R.id.progress_bar);

        btnSearch.setOnClickListener(v -> {
            String symbol = etSymbol.getText().toString().trim().toUpperCase();
            if (!symbol.isEmpty()) {
                searchSymbol(symbol);
            }
        });

        etSymbol.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                btnSearch.setEnabled(s.length() > 0);
            }
        });

        builder.setView(view)
                .setTitle("מסחר מהיר")
                .setNegativeButton("ביטול", (dialog, id) -> dialog.cancel());

        return builder.create();
    }

    private void searchSymbol(String symbol) {
        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);

        // קבלת מחיר עדכני למניה
        apiService.getStockPrice(symbol, new ApiService.ApiResponseListener<Double>() {
            @Override
            public void onSuccess(Double price) {
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);

                if (listener != null) {
                    listener.onSymbolSelected(symbol, symbol, price);
                    dismiss();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);

                // אם לא הצלחנו למצוא מחיר, ננסה לחפש באמצעות חיפוש כללי
                searchWithStockSearch(symbol);
            }
        });
    }

    private void searchWithStockSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.searchStocks(query, new ApiService.ApiResponseListener<List<SearchResultItem>>() {
            @Override
            public void onSuccess(List<SearchResultItem> results) {
                progressBar.setVisibility(View.GONE);

                if (results.isEmpty()) {
                    Toast.makeText(requireContext(), "לא נמצאו תוצאות עבור " + query, Toast.LENGTH_SHORT).show();
                    return;
                }

                // בחר את התוצאה הראשונה
                SearchResultItem firstResult = results.get(0);

                if (listener != null) {
                    listener.onSymbolSelected(
                            firstResult.getSymbol(),
                            firstResult.getName(),
                            firstResult.getPrice()
                    );
                    dismiss();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}