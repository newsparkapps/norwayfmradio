package com.newsparkapps.norwayfmradio.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.newsparkapps.norwayfmradio.activities.Home;

public abstract class BaseFragment extends Fragment {
    protected abstract String getToolbarTitle();
    protected boolean showBackButton() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof Home) {
            ((Home) getActivity()).setupToolbar(
                    getToolbarTitle(),
                    showBackButton()
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requireActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar()
                        .setDisplayHomeAsUpEnabled(false);
            }
        }
    }
}

