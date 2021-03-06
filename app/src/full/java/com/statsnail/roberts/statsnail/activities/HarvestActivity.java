package com.statsnail.roberts.statsnail.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class HarvestActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks,
        View.OnTouchListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = HarvestActivity.class.getSimpleName();

    GoogleAccountCredential mCredential;
    private GoogleSignInAccount mGoogleAccount;


    private EditText mUserInputCatch;
    private EditText mEditTextSuperJumbo;
    private EditText mEditTextJumbo;
    private EditText mEditTextLarge;

    private Spinner mSpinnerHarvestNo;
    private Location mLocation;


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


    /*    @BindView(R.id.fab_post_data)
        FloatingActionButton mFab;*/
    @BindView(R.id.confirm_checkbox)
    CheckBox mCheckBox;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.reg_harvest_button)
    Button mRegisterButton;
    private static final String JUMBO = "jumbo";
    private static final String SUPER_JUMBO = "super_jumbo";
    private static final String LARGE = "large";
    private static final String SELECTED_HARVEST = "sel_harvest";
    private static final String TOTAL_WEIGHT = "total_weight";
    private static final String CHECKBOX_CHECKED = "checkbox";
    private static final String LOCATION = "location";
    private static final String SIGN_IN = "GoogleSignInAccount";
    private int mSpinnerPosition = 0;

    /**
     * This activity runs either in Weighing or Grading mode
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        try {
            mGoogleAccount = (GoogleSignInAccount) getIntent().getExtras().get(SIGN_IN);
            mLocation = getIntent().getExtras().getParcelable(LOCATION);
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        }
        mWeighingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_weighing), false);
        mGradingMode = mSharedPreferences.getBoolean(getString(R.string.logging_mode_grading), false);


        if (mWeighingMode) setupWeighingUi();
        else if (mGradingMode) setupGradingUi();
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            if (mGradingMode) {
                mEditTextLarge.setText(savedInstanceState.getString(LARGE));
                mEditTextJumbo.setText(savedInstanceState.getString(JUMBO));
                mEditTextSuperJumbo.setText(savedInstanceState.getString(SUPER_JUMBO));
                // mSpinnerHarvestNo.setSelection(savedInstanceState.getInt(SELECTED_HARVEST));
                mSpinnerPosition = savedInstanceState.getInt(SELECTED_HARVEST);
                Timber.d("saved selection: " + savedInstanceState.getInt(SELECTED_HARVEST));
            }
            if (mWeighingMode) mUserInputCatch.setText(savedInstanceState.getString(TOTAL_WEIGHT));

            mCheckBox.setChecked(savedInstanceState.getBoolean(CHECKBOX_CHECKED));
        }
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mToolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptExit();
                }
            });
        }

        mCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),
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
                mExistingRows = HarvestUtils.readSheet(HarvestActivity.this, mService);
            }
        });
        readSheet.start();


//        mFab.setEnabled(false);
        mRegisterButton.setOnClickListener(new View.OnClickListener()

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
                    mRegisterButton.setEnabled(true);
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
                    mRegisterButton.setEnabled(false);

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

        }
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.harvest_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }


    private void setupWeighingUi() {
        setContentView(R.layout.activity_weighing);
        mCheckBox = findViewById(R.id.confirm_checkbox);

        mUserInputCatch = findViewById(R.id.catch_edit_text);
        ((TextView) findViewById(R.id.user)).setText(
                getString(R.string.user_logged_in, mGoogleAccount.getDisplayName()));
        mUserInputCatch.setOnTouchListener(this);
    }

    private void setupGradingUi() {
        setContentView(R.layout.activity_grading);
        mCheckBox = findViewById(R.id.confirm_checkbox);

        mSpinnerHarvestNo = findViewById(R.id.spinner_harvest_no);
        mSpinnerHarvestNo.setFocusable(true);
        //mSpinnerHarvestNo.setFocusableInTouchMode(true);

        mEditTextSuperJumbo = findViewById(R.id.super_jumbo_et);
        mEditTextJumbo = findViewById(R.id.jumbo_et);
        mEditTextLarge = findViewById(R.id.large_et);

        ArrayList<Integer> ungradedHarvests =
                getIntent().getIntegerArrayListExtra("ungradedHarvests");
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ungradedHarvests);
        adapter.notifyDataSetChanged();

        // disable mFab and checkbox if there's no harvests left to be graded
        if (ungradedHarvests.size() == 0) {
            //mFab.setEnabled(false);
            mCheckBox.setVisibility(View.INVISIBLE);
            mCheckBox.setEnabled(false);
        } else {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setEnabled(true);
        }

        mSpinnerHarvestNo.setAdapter(adapter);
        mSpinnerHarvestNo.setSelection(mSpinnerPosition);

        mSpinnerHarvestNo.setOnTouchListener(this);
        mEditTextSuperJumbo.setOnTouchListener(this);
        mEditTextJumbo.setOnTouchListener(this);
        mEditTextLarge.setOnTouchListener(this);

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mGradingMode) {
            outState.putString(JUMBO, mEditTextJumbo.getText().toString());
            outState.putString(SUPER_JUMBO, mEditTextSuperJumbo.getText().toString());
            outState.putString(LARGE, mEditTextLarge.getText().toString());
            outState.putInt(SELECTED_HARVEST, mSpinnerHarvestNo.getSelectedItemPosition());
            Timber.d("SAVING selection: " + mSpinnerHarvestNo.getSelectedItemPosition());
        }
        if (mWeighingMode) outState.putString(TOTAL_WEIGHT, mUserInputCatch.getText().toString());
        outState.putBoolean(CHECKBOX_CHECKED, mCheckBox.isChecked());
        Timber.d("saving checbox checked: " + mCheckBox.isChecked());
        super.onSaveInstanceState(outState);
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
        if (mCredential.getSelectedAccountName() == null) {
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
        range = getString(R.string.spreadsheet_read_range) + "!G" + currentHarvestNo + ":L" + currentHarvestNo; //TODO R.string

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
        range = getString(R.string.spreadsheet_read_range) + "!A1:F1";

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
            toastMessage = getString(R.string.error_register_failed);
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
                    showSnackbar(getString(R.string.common_google_play_services_enable_text));
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName = mGoogleAccount.getAccount().name;
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
            try {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            } catch (NullPointerException ne) {
                ne.printStackTrace();
            }

            //  imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // TODO forenkling ang edittexts?
        }
        if (v instanceof RecyclerView) {

        }
        return false;
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        promptExit();
    }

    private void promptExit() {
        if (mExitWithoutPrompt) HarvestActivity.this.finish();
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.discard))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            HarvestActivity.this.finish();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
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