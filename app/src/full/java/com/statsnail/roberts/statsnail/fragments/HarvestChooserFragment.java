package com.statsnail.roberts.statsnail.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import com.statsnail.roberts.statsnail.activities.HarvestActivity;
import com.statsnail.roberts.statsnail.R;

import java.util.List;

import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

public class HarvestChooserFragment extends Fragment
        implements EasyPermissions.PermissionCallbacks,
        View.OnClickListener {
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String TAG = HarvestChooserFragment.class.getSimpleName();
    private Bundle mInfo;
    private SharedPreferences mSharedPreferences;

    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    DrawerLayout mDrawerLayout;

    private Button mGrading;
    private Button mWeighing;
    private Location mLocation;

    public static HarvestChooserFragment NewInstance(Location location) {
        Bundle args = new Bundle();
        args.putParcelable("location", location);
        HarvestChooserFragment f = new HarvestChooserFragment();
        f.setArguments(args);
        Timber.d("Harvest new Instance...");

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chooser, container, false);
        ButterKnife.bind(this, view);
        mGrading = (Button) view.findViewById(R.id.open_grading_button);
        mWeighing = (Button) view.findViewById(R.id.open_weighing_button);
        mGrading.setOnClickListener(this);
        mWeighing.setOnClickListener(this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getContactPermission();
        mInfo = getActivity().getIntent().getExtras();
        try {
            mInfo.putParcelable("location", getArguments().getParcelable("location"));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        mSharedPreferences = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        switch (requestCode) {
            case REQUEST_PERMISSION_GET_ACCOUNTS:
                mWeighing.setEnabled(true);
                mGrading.setEnabled(true);
                break;
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        String toastMsg = "";
        switch (requestCode) {
            case REQUEST_PERMISSION_GET_ACCOUNTS:
                toastMsg = "Contact permissions required!";
                mWeighing.setEnabled(false);
                mGrading.setEnabled(false);
                // request contact permissions again
                getContactPermission();
                break;
        }
        Toast.makeText(getActivity(), toastMsg, Toast.LENGTH_SHORT).show();

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(getActivity(), perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(getActivity(), R.string.returned_from_app_settings_to_activity, Toast.LENGTH_SHORT)
                    .show();
        }
    }


    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    public void getContactPermission() {

        Log.i(TAG, "getContactPerm");
        if (!EasyPermissions.hasPermissions(getActivity(),
                Manifest.permission.GET_ACCOUNTS)) {
            EasyPermissions.requestPermissions(this,
                    "Contact permissions are required to communicate with Statsnail's spreadsheet",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);


        }
    }

    @Override
    public void onClick(View v) {
        Intent loggingActivity = new Intent(getActivity(), HarvestActivity.class);
        loggingActivity.putExtras(mInfo);

        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();

        switch (v.getId()) {
            case (R.id.open_weighing_button):
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), false);
                break;

            case R.id.open_grading_button:
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), false);
                // prefEditor.putString(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                //  loggingActivity.putExtra(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                break;
        }
        prefEditor.commit();
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(getActivity(), getView().findViewById(R.id.logo_bg), "profile");
        startActivity(loggingActivity, options.toBundle());

    }
}
