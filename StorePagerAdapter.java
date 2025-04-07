package com.example.trivia.store;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.HashMap;
import java.util.Map;

public class StorePagerAdapter extends FragmentStateAdapter {

    private Map<Integer, Fragment> fragmentMap = new HashMap<>();

    public StorePagerAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;

        switch (position) {
            case 0:
                fragment = new PortfolioTabFragment();
                break;
            case 1:
                fragment = new StockResearchTabFragment();
                break;
            case 2:
                fragment = new LeadersTabFragment();
                break;
            default:
                fragment = new PortfolioTabFragment();
                break;
        }

        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    /**
     * Get a fragment at a specific position
     */
    public Fragment getFragment(int position) {
        return fragmentMap.get(position);
    }
}