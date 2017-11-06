package com.statsnail.roberts.statsnail.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.adapters.HarvestLogAdapter;
import com.statsnail.roberts.statsnail.utils.HarvestUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import pub.devrel.easypermissions.EasyPermissions;
import timber.log.Timber;

import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_DATE;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_GRADED_BY;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_ID;
import static com.statsnail.roberts.statsnail.data.LogContract.COLUMN_HARVEST_USER;
import static com.statsnail.roberts.statsnail.data.LogContract.CONTENT_URI_HARVEST_LOG;

public class HarvestActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        View.OnTouchListener,
        LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = HarvestActivity.class.getSimpleName();

    GoogleAccountCredential mCredential;
    private GoogleSignInAccount mGoogleAccount;
    private GoogleCredential mCredentiall;

    private EditText mUserInputCatch;
    private EditText mEditTextSuperJumbo;
    private EditText mEditTextJumbo;
    private EditText mEditTextLarge;

    private Spinner mSpinnerHarvestNo;
    private Location mLocation;


    private HarvestLogAdapter mLogAdapter;
    RecyclerView recyclerView;

    private boolean mExitWithoutPrompt = true;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;


    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};
    private com.google.api.services.sheets.v4.Sheets mService = null;


    private boolean mWeighingMode;
    private boolean mGradingMode;
    private SharedPreferences mSharedPreferences;

    private ValueRange mExistingRows;


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

    @BindView(R.id.fab_post_data)
    FloatingActionButton mFab;
    @BindView(R.id.confirm_checkbox)
    CheckBox mCheckBox;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;


    /**
     * This activity runs either in Weighing or Grading mode
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        mGoogleAccount = (GoogleSignInAccount) getIntent().getExtras().get("GoogleSignInAccount");
        mLocation = getIntent().getExtras().getParcelable("location");
        mWeighingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_weighing), false);
        mGradingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_grading), false);

        if (mWeighingMode) setupWeighingUi();
        else if (mGradingMode) setupGradingUi();
        ButterKnife.bind(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
                Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(mGoogleAccount.getAccount().name);

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Statsnail Catch Logger")
                .build();


        getResultsFromApi();
        Thread readSheet = new Thread(new Runnable() {
            @Override
            public void run() {
                mExistingRows = HarvestUtils.readSheet(HarvestActivity.this, mService);
            }
        });
        readSheet.start();


//        mFab.setEnabled(false);
        mFab.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                Timber.d("fab onclick");
                if (!mCheckBox.isChecked()) {
                    showSnackbar("Fill in weights and confirm checkbox");
                } else if (!Utils.workingConnection(HarvestActivity.this)) {
                    Toast.makeText(HarvestActivity.this, "No Internet connection", Toast.LENGTH_SHORT).show();
                } else {
                    showConfirmationDialog();
                }
            }
        });

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()

        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mExitWithoutPrompt = false;
                    // mRegButton.setEnabled(true);
                    mFab.setEnabled(true);
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    if (mWeighingMode) {
                        imm.hideSoftInputFromWindow(mUserInputCatch.getWindowToken(), 0);
                        mUserInputCatch.setEnabled(false);
                    }
                    if (mGradingMode) {
                        imm.hideSoftInputFromWindow(mEditTextSuperJumbo.getWindowToken(), 0);
                        imm.hideSoftInputFromWindow(mEditTextJumbo.getWindowToken(), 0);
                        imm.hideSoftInputFromWindow(mEditTextLarge.getWindowToken(), 0);

                        mEditTextSuperJumbo.setEnabled(false);
                        mEditTextJumbo.setEnabled(false);
                        mEditTextLarge.setEnabled(false);
                        mSpinnerHarvestNo.setEnabled(false);
                    }

                } else {
                    //mRegButton.setEnabled(false);
                    mFab.setEnabled(false);

                    if (mWeighingMode) {
                        mUserInputCatch.setEnabled(true);

                    }
                    if (mGradingMode) {
                        mEditTextSuperJumbo.setEnabled(true);
                        mEditTextJumbo.setEnabled(true);
                        mEditTextLarge.setEnabled(true);
                        mSpinnerHarvestNo.setEnabled(true);
                    }
                }
            }
        });

        if (mGradingMode) {
            getSupportLoaderManager().initLoader(HARVEST_LOG_LOADER_ID, null, this);
        }
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.harvest_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.i(TAG, "onCLoader");
        return new CursorLoader(this,
                CONTENT_URI_HARVEST_LOG,
                PROJECTION,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        recyclerView.scrollToPosition(recyclerView.getLayoutManager().getItemCount() - 1);

        if (mGradingMode) {
            ArrayList<Integer> ungradedHarvests = new ArrayList<>();
            data.moveToFirst();
            // Looping backwards through harvestnumbers, adding every ungraded to the list
            data.moveToLast();
            while (!data.isBeforeFirst()) {
                if (data.getString(INDEX_HARVEST_GRADED) == null) {
                    ungradedHarvests.add(data.getInt(INDEX_HARVEST_ID));
                }
                data.moveToPrevious();
            }
            // disable mFab and checkbox if there's no harvests left to be graded
            if (ungradedHarvests.size() == 0) {
                //mFab.setEnabled(false);
                mCheckBox.setVisibility(View.INVISIBLE);
                mCheckBox.setEnabled(false);
            } else {
                mCheckBox.setVisibility(View.VISIBLE);
                mCheckBox.setEnabled(true);
            }
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ungradedHarvests);
            adapter.notifyDataSetChanged();

            mSpinnerHarvestNo.setAdapter(adapter);
        }
        mLogAdapter.swapCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    private void setupWeighingUi() {
        setContentView(R.layout.activity_weighing);

        mUserInputCatch = findViewById(R.id.catch_edit_text);
        ((TextView) findViewById(R.id.user)).setText(
                getString(R.string.user_logged_in, mGoogleAccount.getDisplayName()));
        mUserInputCatch.setOnTouchListener(this);
    }

    private void setupGradingUi() {
        setContentView(R.layout.activity_grading);
        recyclerView = findViewById(R.id.recycler_view_harvest_log);
        //recyclerView.smoothScrollToPosition(0);

        mLogAdapter = new HarvestLogAdapter(this);

        //recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mLogAdapter);
        recyclerView.setOnTouchListener(this);

        mSpinnerHarvestNo = findViewById(R.id.spinner_harvest_no);
        mSpinnerHarvestNo.setFocusable(true);
        //mSpinnerHarvestNo.setFocusableInTouchMode(true);

        mEditTextSuperJumbo = findViewById(R.id.super_jumbo_et);
        mEditTextJumbo = findViewById(R.id.jumbo_et);
        mEditTextLarge = findViewById(R.id.large_et);

        mSpinnerHarvestNo.setOnTouchListener(this);
        mEditTextSuperJumbo.setOnTouchListener(this);
        mEditTextJumbo.setOnTouchListener(this);
        mEditTextLarge.setOnTouchListener(this);

    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        Log.i(TAG, "getResFromApi");
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            mCredential.setSelectedAccountName(mGoogleAccount.getAccount().name);
        } else if (!isDeviceOnline()) {
            Toast.makeText(this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void postGradingData() {
        String range;
        String spreadsheetId = getString(R.string.spreadsheet_id); // TODO if sheet doesn't exist

        int selectedHarvestNo = (Integer) mSpinnerHarvestNo.getSelectedItem();

        if (mExistingRows.getValues() == null) {
            toastFromThread(getString(R.string.error_reading_sheet));

            supportFinishAfterTransition();
            finish();
            return;
        }


        List<Object> currentRow = mExistingRows.getValues().get(selectedHarvestNo);

        // Get the integer value of current row's registered weight, removing 'kg'
        int registeredCatch = 1;
        try {
            registeredCatch = Integer.valueOf((currentRow.get(4).toString()).replaceAll("\\D+", ""));
        } catch (NumberFormatException ne) {
            ne.printStackTrace();
        }

        String large = (mEditTextLarge.getText().toString().isEmpty()) ? "0" : mEditTextLarge.getText().toString();
        String jumbo = (mEditTextJumbo.getText().toString().isEmpty()) ? "0" : mEditTextJumbo.getText().toString();
        String superJumbo = (mEditTextSuperJumbo.getText().toString().isEmpty()) ? "0" : mEditTextSuperJumbo.getText().toString();

        int loss = registeredCatch - (Integer.valueOf(large) + Integer.valueOf(jumbo) + Integer.valueOf(superJumbo));

        String currentHarvestNo = String.valueOf(selectedHarvestNo + 1); // +1 cos row 1 == labels
        range = "sheet4!G" + currentHarvestNo + ":L" + currentHarvestNo; //TODO R.string

        List<List<Object>> values = new ArrayList<>();

        List<Object> gradingData = new ArrayList<>();


        gradingData.add(getDate());
        //gradingData.add(getTime());
        // catchData.add(location);
        gradingData.add(getString(R.string.measure_in_kg, large));
        gradingData.add(getString(R.string.measure_in_kg, jumbo));
        gradingData.add(getString(R.string.measure_in_kg, superJumbo));
        gradingData.add(mGoogleAccount.getDisplayName());
        gradingData.add(getString(R.string.measure_in_kg_int, loss));

        values.add(gradingData);

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(range);
        valueRange.setValues(values);

        String toastMessage;

        if (mService == null || !Utils.workingConnection(this)) {
            toastMessage = getString(R.string.error_register_failed_connection);
            toastFromThread(toastMessage);
            finish();
            return;
        }
        try {
            this.mService.spreadsheets().values()
                    .append(spreadsheetId, range, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (NullPointerException | IOException e) {
            Log.i(TAG, e.getMessage());
        }

        mExistingRows.getValues().add(selectedHarvestNo, values.get(0));
        HarvestUtils.updateDbSingle(this, valueRange, selectedHarvestNo);
        toastMessage = getString(R.string.toast_harvest_graded, selectedHarvestNo);
        toastFromThread(toastMessage);
        finish();
    }

    public void postWeighingData() {
        String range;
        String spreadsheetId = getString(R.string.spreadsheet_id);
        range = "sheet4!A1:F1";

        int harvestNum;
        if (mExistingRows == null || mExistingRows.getValues() == null) {
            finish();
            return;
        } else harvestNum = mExistingRows.getValues().size();

        List<List<Object>> values = new ArrayList<>();
        List<Object> catchData = new ArrayList<>();

        String location = null;
        try {
            location = HarvestUtils.getLocationUrl(mLocation);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        String snailCatch = (mUserInputCatch.getText().toString().isEmpty()) ? "0" : mUserInputCatch.getText().toString();
        String harvestNumber = String.valueOf(harvestNum);
        catchData.add(harvestNumber);
        catchData.add(getDate());
        catchData.add(getTime());
        catchData.add(location);
        catchData.add(getString(R.string.measure_in_kg, snailCatch));
        catchData.add(mGoogleAccount.getDisplayName());

        values.add(catchData);

        //Create the valuerange object and set its fields
        ValueRange valueRange = new ValueRange();
        valueRange.setMajorDimension("ROWS");
        valueRange.setRange(range);
        valueRange.setValues(values);

        String toastMessage;
        if (mService == null || !Utils.workingConnection(this)) {
            toastMessage = getString(R.string.error_register_failed_connection);
            toastFromThread(toastMessage);
            finish();
            return;
        }
        try {
            this.mService.spreadsheets().values()
                    .append(spreadsheetId, range, valueRange)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (NullPointerException | IOException e) {
            Log.i(TAG, e.getMessage());
            toastMessage = "Failed to register catch!";
            toastFromThread(toastMessage);
            finish();
            return;

        }
        // updating the db immediately in order for the log to get updated (clumsy?)
        mExistingRows.getValues().add(harvestNum, values.get(0));
        HarvestUtils.updateDb(this, mExistingRows);
        toastMessage = getString(R.string.toast_harvest_added, harvestNum);
        toastFromThread(toastMessage);
        finish();
    }

    private void toastFromThread(final String message) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(HarvestActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // TODO occasional wrong format - due to threadsafe?
    private String getDate() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());

        return dateFormat.format(date);
    }

    // TODO sett i utils
    private String getTime() {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());

        return dateFormat.format(date);
    }

    private String confirmationAttr(EditText editText) {
        return TextUtils.isEmpty(editText.getText()) ? "-" : getString(R.string.measure_in_kg, editText.getText());
    }

    private void showConfirmationDialog() {
        String msg;
        if (mGradingMode)
            msg = getString(R.string.grade_confirmation,
                    confirmationAttr(mEditTextSuperJumbo), confirmationAttr(mEditTextJumbo), confirmationAttr(mEditTextLarge));
        else msg = getString(R.string.weight_confirmation, confirmationAttr(mUserInputCatch));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm registration")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mWeighingMode) {
                            Thread postWeightsThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    postWeighingData();
                                }
                            });
                            postWeightsThread.start();
                        } else if (mGradingMode) {
                            Thread postGradingsThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    postGradingData();
                                }
                            });
                            postGradingsThread.start();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();


    }


    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //  mOutputText.setText( //TODO
                    //         "This app requires Google Play Services. Please install " +
                    //               "Google Play Services on your device and relaunch this app.");
                    Toast.makeText(this, "Install Google Play Services", Toast.LENGTH_SHORT).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = mGoogleAccount.getAccount().name;
                    // data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.i(TAG, "onActResult" + "accName1: " + accountName + "\naccName2: " + mGoogleAccount.getAccount().name);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


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
                HarvestActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mExitWithoutPrompt = false;
        if (v instanceof EditText || v instanceof Spinner) {
            mCheckBox.setChecked(false);
        }
        if (v instanceof Spinner) {
            mSpinnerHarvestNo.requestFocus();
            mSpinnerHarvestNo.performClick();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            //  imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // TODO forenkling ang edittexts?
        }
        if (v instanceof RecyclerView) {

        }
        return false;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (mExitWithoutPrompt) HarvestActivity.this.finish();
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Discard?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            HarvestActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showSnackbar(getString(R.string.connection_error));
    }

}