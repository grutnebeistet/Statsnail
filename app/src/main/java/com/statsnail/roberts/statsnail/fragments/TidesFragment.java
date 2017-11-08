package com.statsnail.roberts.statsnail.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.adapters.TidesDataAdapter;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.sync.StatsnailSyncTask;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesFragment extends android.support.v4.app.Fragment implements
        OnMapReadyCallback, android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    @BindView(R.id.tides_recycler_view)
    RecyclerView mTidesRecyclerView;
    @BindView(R.id.location_name)
    TextView mLocationTextView;
    @BindView(R.id.forecast_date)
    TextView mDateTimeTextView;
    @BindView(R.id.tides_error_tv)
    TextView mErrorTextView;
    @BindView(R.id.next_day_button)
    RelativeLayout mNextDay;
    @BindView(R.id.prev_day_button)
    RelativeLayout mPrevDay;
    @BindView(R.id.cardview_container)
    CardView mContainer;
    @BindView(R.id.image_button_curr_loc)
    ImageView mResetLoc;
    @BindView(R.id.next_day_image)
    ImageView mNextDayImg;
    @BindView(R.id.prev_day_image)
    ImageView mPrevDayImg;

    private static final int LOADER_ID_TIDES = 1349;
    private static final int LOADER_ID_WINDS = 1350;

    public static final String[] TIDES_PROJECTION = {
            TidesContract.TidesEntry.COLUMN_TIDES_DATE,
            TidesContract.TidesEntry.COLUMN_WATER_LEVEL,
            TidesContract.TidesEntry.COLUMN_LEVEL_FLAG,
            TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL,
            TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG
    };
    public static final int INDEX_TIDE_DATE = 0;
    public static final int INDEX_TIDE_LEVEL = 1;
    public static final int INDEX_LEVEL_TIME = 3;
    public static final int INDEX_FLAG = 2;
    public static final int INDEX_ERROR = 4;

    public static final String EXTRA_TIDE_QUERY_DATE = "tides_date";

    String TAG = TidesFragment.class.getSimpleName();
    private static final String SELECTED_STYLE = "selected_style";
    private static final String MAP_ZOOM = "map_zoom";
    private static final String PLACE_NAME = "location_name";
    private static final String LOCATION = "location";
    private static final String CONTAINER_VISIBILITY = "visibility";

    private static final int FORECAST_DAYS = 7;
    private GoogleMap mMap = null;
    // Stores the ID of the currently selected style, so that we can re-apply it when
    // the activity restores state, for example when the device changes orientation.
    private int mSelectedStyleId = R.string.style_label_default;

    // These are simply the string resource IDs for each of the style names. We use them
    // as identifiers when choosing which style to apply.
    private int mStyleIds[] = {
            R.string.style_label_retro,
            R.string.style_label_night,
            R.string.style_label_grayscale,
            R.string.style_label_no_pois_no_transit,
            R.string.style_label_default,
    };
    private static LatLng LAT_LNG;
    private Location mLocation;
    private TidesDataAdapter mAdapter;
    private TidesData mTidesData;
    private float mMapZoom = 8;
    SharedPreferences mPreferences;
    private int mVisibility = View.VISIBLE;

    public static TidesFragment newInstance(Location location) {
        Bundle args = new Bundle();
        args.putParcelable(LOCATION, location);
        TidesFragment f = new TidesFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (savedInstanceState != null) {
            Timber.d("savedInstance != NUll");
            mSelectedStyleId = savedInstanceState.getInt(SELECTED_STYLE);
            mMapZoom = savedInstanceState.getFloat(MAP_ZOOM);
            double longitude = savedInstanceState.getDouble(MainActivityFull.EXTRA_LONGITUDE);
            double latitude = savedInstanceState.getDouble(MainActivityFull.EXTRA_LATITUDE);
            Timber.d("longitude saved : " + longitude);
            LAT_LNG = new LatLng(latitude, longitude);
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(latitude));
            editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(longitude));
            Timber.d("longitude to preference : " + String.valueOf(longitude));
            editor.commit();
        }
        if (savedInstanceState == null) {
            initLocation();
        }

        mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis())).apply();
        mAdapter = new TidesDataAdapter(getActivity());


        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, this);

    }

    private void initLocation() {
        mLocation = getArguments().getParcelable(LOCATION);
        if (mLocation != null)
            LAT_LNG = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(mLocation.getLongitude()));
        editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(mLocation.getLatitude()));
        editor.commit();

        updateValuesOnLocationChange();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mResetLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initLocation();
                onMapReady(mMap);
                mMapZoom = 8;
                mVisibility = View.VISIBLE;
            }
        });

        mNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()));
                try {
                    String tomorrow = Utils.getDatePlusOne(currentDate);

                    if (Utils.isTomorrowLast(tomorrow)) {
                        mNextDayImg.setVisibility(View.GONE);
                        return;
                    } else {
                        mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, tomorrow).apply();
                        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                    }


                } catch (ParseException e) {
                    Timber.e("failed to increase date");
                    e.printStackTrace();
                }
                mPrevDayImg.setVisibility(View.VISIBLE);
            }
        });
        mPrevDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()));
                try {
                    mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDateMinusOne(currentDate)).apply();
                    getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                } catch (ParseException e) {
                    e.printStackTrace();
                }


            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");
        if (!Utils.workingConnection(getActivity())) {
            Timber.d("noConnect");
            mNextDayImg.setVisibility(View.INVISIBLE);
            mPrevDayImg.setVisibility(View.INVISIBLE);
            showSnackbar(getString(R.string.connection_error));
        }
    }

    private void showSnackbar(final String text) {
        View container = getActivity().findViewById(R.id.tide_content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        ButterKnife.bind(this, view);
        if (savedInstanceState != null) {
            //      mLocationTextView.setText(savedInstanceState.getString(PLACE_NAME));
            mVisibility = savedInstanceState.getInt(CONTAINER_VISIBILITY);
            mContainer.setVisibility(mVisibility);
        }

        mTidesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mTidesRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOADER_ID_TIDES:
                String sortOrder = TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC";
                String selection = TidesContract.TidesEntry.COLUMN_TIDES_DATE + "=?";
                String[] selectionArgs = new String[]{mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()))};

                return new android.support.v4.content.CursorLoader(getActivity(), TidesContract.TidesEntry.CONTENT_URI,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        mContainer.setVisibility(mVisibility);
        Timber.d("Cursor count: " + cursor.getCount() + " position: " + cursor.getPosition());
        String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                Utils.getDate(System.currentTimeMillis()));
        if (currentDate.compareTo(Utils.getDate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))) < 0) {
            mPrevDayImg.setVisibility(View.INVISIBLE);
        }

        if (cursor == null || cursor.getCount() == 0) {
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(R.string.connection_error);

            mNextDayImg.setVisibility(View.INVISIBLE);
        } else if (cursor.getCount() <= 2) {
            cursor.moveToFirst();
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(cursor.getString(INDEX_ERROR));
            //  Toast.makeText(getActivity(), "Error: " + cursor.getString(INDEX_ERROR), Toast.LENGTH_SHORT).show();
            mNextDayImg.setVisibility(View.INVISIBLE);
            mAdapter.swapCursor(null);
        } else {
            mAdapter.swapCursor(cursor);
            mErrorTextView.setVisibility(View.GONE);
            mNextDayImg.setVisibility(View.VISIBLE);
        }
        try {
            mLocationTextView.setText(Utils.getPlaceName(getActivity()));
            String dateShown = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()));
            mDateTimeTextView.setText(Utils.getPrettyDate(Utils.getDateInMillisec(dateShown)));
        } catch (IOException | NullPointerException | ParseException e) {
            e.printStackTrace();
            mErrorTextView.setText(R.string.error_unknown);
            return;
        }
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private void updateValuesOnLocationChange() {
        Timber.d("updateValuesOnLocationChange");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StatsnailSyncTask.syncData(getActivity());
            }
        });
        thread.start();
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        Timber.d("onMapReady....");
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG, mMapZoom));

        setSelectedStyle();
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                LAT_LNG = latLng;
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()));
                editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(latLng.latitude));
                editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(latLng.longitude));
                editor.commit();

                updateValuesOnLocationChange();
                mVisibility = View.VISIBLE;
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                mContainer.setVisibility(View.INVISIBLE);
            }
        });
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the selected map style, so we can assign it when the activity resumes.
        outState.putInt(SELECTED_STYLE, mSelectedStyleId);
        outState.putFloat(MAP_ZOOM, mMap.getCameraPosition().zoom);
        // outState.putParcelable(LOCATION, mLocation);
        outState.putDouble(MainActivityFull.EXTRA_LATITUDE, LAT_LNG.latitude);
        outState.putDouble(MainActivityFull.EXTRA_LONGITUDE, LAT_LNG.longitude);
        //outState.putString(PLACE_NAME, mLocationTextView.getText().toString());
        outState.putInt(CONTAINER_VISIBILITY, mContainer.getVisibility());
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.styled_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_style_choose) {
            showStylesDialog();
        }
        return true;
    }

    /**
     * Shows a dialog listing the styles to choose from, and applies the selected
     * style when chosen.
     */
    private void showStylesDialog() {
        List<String> styleNames = new ArrayList<>();
        for (int style : mStyleIds) {
            styleNames.add(getString(style));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.style_choose));
        builder.setItems(styleNames.toArray(new CharSequence[styleNames.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedStyleId = mStyleIds[which];
                        String msg = getString(R.string.style_set_to, getString(mSelectedStyleId));
                        Toast.makeText(getActivity().getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Timber.d(msg);
                        setSelectedStyle();
                    }
                });
        builder.show();
    }

    /**
     * Creates a {@link MapStyleOptions} object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the {@link GoogleMap} instance,
     * via the setMapStyle() method.
     */
    private void setSelectedStyle() {
        MapStyleOptions style;
        switch (mSelectedStyleId) {
            case R.string.style_label_retro:
                // Sets the retro style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_retro);
                break;
            case R.string.style_label_night:
                // Sets the night style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_night);
                break;
            case R.string.style_label_grayscale:
                // Sets the grayscale style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_grayscale);
                break;
            case R.string.style_label_no_pois_no_transit:
                // Sets the no POIs or transit style via JSON string.
                style = new MapStyleOptions("[" +
                        "  {" +
                        "    \"featureType\":\"poi.business\"," +
                        "    \"elementType\":\"all\"," +
                        "    \"stylers\":[" +
                        "      {" +
                        "        \"visibility\":\"off\"" +
                        "      }" +
                        "    ]" +
                        "  }," +
                        "  {" +
                        "    \"featureType\":\"transit\"," +
                        "    \"elementType\":\"all\"," +
                        "    \"stylers\":[" +
                        "      {" +
                        "        \"visibility\":\"off\"" +
                        "      }" +
                        "    ]" +
                        "  }" +
                        "]");
                break;
            case R.string.style_label_default:
                // Removes previously set style, by setting it to null.
                style = null;
                break;
            default:
                return;
        }
        mMap.setMapStyle(style);
    }
}