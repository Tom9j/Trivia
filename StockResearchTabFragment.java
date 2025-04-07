package com.example.trivia.store;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trivia.R;

import java.util.ArrayList;
import java.util.List;

public class StockResearchTabFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchResultAdapter adapter;
    private List<SearchResultItem> searchResults = new ArrayList<>();
    private ApiService apiService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stock_research_tab, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search);
        recyclerView = view.findViewById(R.id.recycler_view_search_results);
        progressBar = view.findViewById(R.id.progress_bar);

        apiService = ApiService.getInstance(requireContext());

        setupRecyclerView();
        setupSearchListener();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchResultAdapter(searchResults, this::onStockSelected);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Empty implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Empty implementation
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    searchStocks(s.toString());
                } else {
                    searchResults.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void searchStocks(String query) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.searchStocks(query, new ApiService.ApiResponseListener<List<SearchResultItem>>() {
            @Override
            public void onSuccess(List<SearchResultItem> result) {
                progressBar.setVisibility(View.GONE);

                searchResults.clear();
                searchResults.addAll(result);
                adapter.notifyDataSetChanged();

                if (result.isEmpty()) {
                    Toast.makeText(requireContext(), "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onStockSelected(SearchResultItem stock) {
        if (getParentFragment() instanceof StoreFragment) {
            StoreFragment parent = (StoreFragment) getParentFragment();
            parent.showBuyDialog(stock.getSymbol(), stock.getName(), stock.getPrice());
        }
    }
}