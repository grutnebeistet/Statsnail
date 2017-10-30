package com.statsnail.roberts.statsnail.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * Created by Adrian on 27/10/2017.
 */

public class HarvestLogFragment extends Fragment {
    private ValueRange mValueRange;
    public static HarvestLogFragment newInstance() {

        Bundle args = new Bundle();

        HarvestLogFragment fragment = new HarvestLogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // mValueRange =
    }
}
