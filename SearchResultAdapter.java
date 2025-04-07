package com.example.trivia.store;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trivia.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private final List<SearchResultItem> resultList;
    private final OnStockSelectedListener listener;

    public interface OnStockSelectedListener {
        void onStockSelected(SearchResultItem stock);
    }

    public SearchResultAdapter(List<SearchResultItem> resultList, OnStockSelectedListener listener) {
        this.resultList = resultList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        SearchResultItem item = resultList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return resultList.size();
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSymbol;
        private final TextView tvName;
        private final TextView tvPrice;

        public SearchResultViewHolder(View itemView) {
            super(itemView);
            tvSymbol = itemView.findViewById(R.id.tv_result_symbol);
            tvName = itemView.findViewById(R.id.tv_result_name);
            tvPrice = itemView.findViewById(R.id.tv_result_price);
        }

        public void bind(SearchResultItem item, OnStockSelectedListener listener) {
            tvSymbol.setText(item.getSymbol());
            tvName.setText(item.getName());

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            tvPrice.setText(currencyFormat.format(item.getPrice()));

            itemView.setOnClickListener(v -> listener.onStockSelected(item));
        }
    }
}