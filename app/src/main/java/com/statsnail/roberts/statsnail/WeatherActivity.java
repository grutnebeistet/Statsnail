package com.statsnail.roberts.statsnail;

import android.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.statsnail.roberts.statsnail.fragments.TidesFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class WeatherActivity extends AppCompatActivity {
    @BindView(R.id.weather_pager)
    ViewPager mViewPager;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());

        mViewPager.setAdapter(new WeatherPagerAdapter(getFragmentManager()));

    }

    private class WeatherPagerAdapter extends android.support.v13.app.FragmentPagerAdapter {
        public WeatherPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.app.Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return null;//  return TidesFragment.newInstance();
                default:
                    return null; //return TidesFragment.newInstance();
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }

}
