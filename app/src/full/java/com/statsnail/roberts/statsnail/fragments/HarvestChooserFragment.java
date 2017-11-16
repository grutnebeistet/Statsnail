package com.statsnail.roberts.statsnail.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.statsnail.roberts.statsnail.activities.HarvestActivity;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.adapters.HarvestLogAdapter;
import com.statsnail.roberts.statsnail.utils.HarvestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_DATE;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_GRADED_BY;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_ID;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_USER;
import static com.statsnail.roberts.statsnail.data.LogContract.CONTENT_URI_HARVEST_LOG;

public class HarvestChooserFragment extends android.support.v4.app.Fragment
        implements EasyPermissions.PermissionCallbacks,
        View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    public static final String EXTRA_LOCATION = "location";

    private static final String TAG = HarvestChooserFragment.class.getSimpleName();
    private Bundle mInfo;
    private SharedPreferences mSharedPreferences;
    private HarvestLogAdapter mLogAdapter;
    GoogleAccountCredential mCredential;
    private com.google.api.services.sheets.v4.Sheets mService = null;

    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;
    DrawerLayout mDrawerLayout;
    private Location mLocation;

    private Spinner mSpinnerHarvestNo;

    private static final String[] PROJECTION = {
            COLUMN_HARVEST_ID,
            COLUMN_HARVEST_DATE,
            COLUMN_HARVEST_USER,
            COLUMN_HARVEST_GRADED_BY
    };
    public static final int INDEX_HARVEST_ID = 0;
    public static final int INDEX_HARVEST_DATE = 1;
    public static final int INDEX_HARVEST_USER = 2;
    public static final int INDEX_HARVEST_GRADED = 3;

    private static final int HARVEST_LOG_LOADER_ID = 1349;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    private ValueRange mExistingRows;

    @BindView(R.id.open_grading_button)
    Button mGrading;
    @BindView(R.id.open_weighing_button)
    Button mWeighing;
    @BindView(R.id.recycler_view_harvest_log)
    RecyclerView recyclerView;
    private GoogleSignInAccount mGoogleAccount;
    private static final String SIGN_IN = "GoogleSignInAccount";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};

    private ArrayList<Integer> ungradedHarvests;

    public static HarvestChooserFragment NewInstance(Location location) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_LOCATION, location);
        HarvestChooserFragment chooserFragment = new HarvestChooserFragment();
        chooserFragment.setArguments(args);

        return chooserFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chooser, container, false);
        ButterKnife.bind(this, view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mLogAdapter);

        mGrading.setOnClickListener(this);
        mWeighing.setOnClickListener(this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getAccountsPermission();
        mInfo = getActivity().getIntent().getExtras();
        mGoogleAccount = (GoogleSignInAccount) mInfo.get(SIGN_IN);
        mCredential = GoogleAccountCredential.usingOAuth2(getActivity().getApplicationContext(),
                Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(mGoogleAccount.getAccount() != null ? mGoogleAccount.getAccount().name : "Username not available");

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName(getString(R.string.name_logger))
                .build();


        getResultsFromApi();
        Thread readSheet = new Thread(new Runnable() {
            @Override
            public void run() {
                mExistingRows = HarvestUtils.readSheet(getActivity(), mService);
            }
        });
        readSheet.start();

        getLoaderManager().initLoader(HARVEST_LOG_LOADER_ID, null, this);

        //recyclerView.smoothScrollToPosition(0);

        mLogAdapter = new HarvestLogAdapter(getContext());

        //recyclerView.setLayoutManager(new LinearLayoutManager(this));

        try {
            mInfo.putParcelable(EXTRA_LOCATION, getArguments().getParcelable(EXTRA_LOCATION));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        mSharedPreferences = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }
    private void getResultsFromApi() {
        Log.i(TAG, "getResFromApi");
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            mCredential.setSelectedAccountName(mGoogleAccount.getAccount().name);
        } else if (!isDeviceOnline()) {
            Toast.makeText(getActivity(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }
    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCLoader");
        return new CursorLoader(getContext(),
                CONTENT_URI_HARVEST_LOG,
                PROJECTION,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        recyclerView.scrollToPosition(recyclerView.getLayoutManager().getItemCount() - 1);
        mLogAdapter.swapCursor(data);
        ungradedHarvests = new ArrayList<>();
        data.moveToFirst();
        // Looping backwards through harvestnumbers, adding every ungraded to the list
        data.moveToLast();
        while (!data.isBeforeFirst()) {
            if (data.getString(INDEX_HARVEST_GRADED) == null) {
                ungradedHarvests.add(data.getInt(INDEX_HARVEST_ID));
            }
            data.moveToPrevious();
        }


    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    @Override
    public void onResume() {
        getLoaderManager().restartLoader(HARVEST_LOG_LOADER_ID, null, this);
        super.onResume();
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
        String toastMsg = "";
        switch (requestCode) {
            case REQUEST_PERMISSION_GET_ACCOUNTS:
                toastMsg = "Accounts permissions required!";
                mWeighing.setEnabled(false);
                mGrading.setEnabled(false);
                // request contact permissions again
                //   getAccountsPermission(); // TODO not necessary not  - maybe later if sheet should be picked from the account
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
    public void getAccountsPermission() {
        if (!EasyPermissions.hasPermissions(getActivity(),
                Manifest.permission.GET_ACCOUNTS)) {
            EasyPermissions.requestPermissions(this,
                    "Accounts permissions are required to communicate with Statsnail's spreadsheet",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);


        }
    }

    @Override
    public void onClick(View v) {
        Intent loggingActivity = new Intent(getActivity(), HarvestActivity.class);
        loggingActivity.putExtras(mInfo);

        SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
        Bundle bundle = new Bundle();
        switch (v.getId()) {
            case (R.id.open_weighing_button):
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), false);
                break;

            case R.id.open_grading_button:
                prefEditor.putBoolean(getString(R.string.logging_mode_grading), true);
                prefEditor.putBoolean(getString(R.string.logging_mode_weighing), false);
                bundle.putIntegerArrayList("ungradedHarvests", ungradedHarvests);
                loggingActivity.putExtra("ungradedHarvests", ungradedHarvests);
                // prefEditor.putString(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                //  loggingActivity.putExtra(getString(R.string.snail_logging_mode), getString(R.string.logging_mode_grading));
                break;
        }
        prefEditor.commit();

        startActivity(loggingActivity);

    }
}
