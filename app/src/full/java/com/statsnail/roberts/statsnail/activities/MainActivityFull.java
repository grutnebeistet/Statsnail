/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.statsnail.roberts.statsnail.activities;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.statsnail.roberts.statsnail.BuildConfig;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.fragments.HarvestChooserFragment;
import com.statsnail.roberts.statsnail.fragments.TidesFragment;
import com.statsnail.roberts.statsnail.sync.SyncUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * This shows how to style a map with JSON.
 */
public class MainActivityFull extends AppCompatActivity {
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    // Home location is actual GPS location, primarily used for notifaction
    public static final String HOME_LAT = "home_lat";
    public static final String HOME_LON = "home_lon";
    private static final String LOCATION = "location";
    public static MainActivityFull instance;
    private TidesFragment mTidesFragment;
    private HarvestChooserFragment mHarvestFragment;
    @BindView(R.id.tab_layout)
    TabLayout mTabs;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    /**
     * Provides the entry point to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    private static final String TAG = MainActivityFull.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @BindView(R.id.pager)
    ViewPager mViewPager;
    SharedPreferences mPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setContentView(R.layout.activity_main_full);
        ButterKnife.bind(this);
        Timber.plant(new Timber.DebugTree());
        if (savedInstanceState == null) {
            if (!checkPermissions()) {
                requestPermissions();
            } else {
                getLastLocation();
            }
        } else {
            mLastLocation = savedInstanceState.getParcelable(LOCATION);
            bindWidgetsWithAnEvent();
            setupTabLayout();
        }
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

    }

    public static MainActivityFull getInstance() {
        return instance;
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION, mLastLocation);
        super.onSaveInstanceState(outState);
    }

    private void setupTabLayout() {
        android.support.v4.app.Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frag_container);
        if (fragment == null) {
            Timber.d("setupTab frag NULL");
            mHarvestFragment = HarvestChooserFragment.NewInstance(mLastLocation);
            mTidesFragment = TidesFragment.newInstance(mLastLocation);
        } else if (fragment instanceof TidesFragment) {
            Timber.d("setupTab frag Tides");
            mTidesFragment = (TidesFragment) fragment;
            mHarvestFragment = HarvestChooserFragment.NewInstance(mLastLocation);
        } else if (fragment instanceof HarvestChooserFragment) {
            Timber.d("setupTab frag Harvest");
            mHarvestFragment = (HarvestChooserFragment) fragment;
            mTidesFragment = TidesFragment.newInstance(mLastLocation);
        }

        if (mTabs.getTabCount() == 0) {
            mTabs.addTab(mTabs.newTab().setText(getString(R.string.tab_tides)), true);
            mTabs.addTab(mTabs.newTab().setText(getString(R.string.tab_harvest)));


        }



    }

    private void bindWidgetsWithAnEvent() {
        mTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setCurrentTabFragment(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setCurrentTabFragment(int tabPosition) {
        switch (tabPosition) {
            case 0:
                replaceFragment(mTidesFragment);
                break;
            case 1:
                replaceFragment(mHarvestFragment);
                break;
        }
    }

    public void replaceFragment(android.support.v4.app.Fragment fragment) {
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.frag_container, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivityFull.this);
                            SharedPreferences.Editor editor = preferences.edit();
                            // TODO  convert to doubleToRawLongBits
                            editor.putString(EXTRA_LATITUDE, String.valueOf(mLastLocation.getLatitude()));
                            editor.putString(EXTRA_LONGITUDE, String.valueOf(mLastLocation.getLongitude()));
                            editor.putString(HOME_LAT, String.valueOf(mLastLocation.getLatitude()));
                            editor.putString(HOME_LON, String.valueOf(mLastLocation.getLongitude()));
                            editor.commit();

                            SyncUtils.initialize(MainActivityFull.this);
                            try {
                                Timber.d(Utils.getPlaceDirName(MainActivityFull.this, mLastLocation));
                            } catch (IOException e) {

                            }
                            // Location retrieved means it's okay to initiate fragments
                            bindWidgetsWithAnEvent();
                            setupTabLayout();

                        } else {
                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }


    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.main_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivityFull.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.menu_main, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}