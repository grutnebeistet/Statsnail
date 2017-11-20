package com.statsnail.roberts.statsnail.fragments;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.adapters.TidesDataAdapter;
import com.statsnail.roberts.statsnail.adapters.WindsDataAdapter;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.sync.StatsnailSyncTask;
import com.statsnail.roberts.statsnail.utils.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesFragment extends android.support.v4.app.Fragment implements
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        OnMapReadyCallback, android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {
    @BindView(R.id.tides_recycler_view)
    RecyclerView mTidesRecyclerView;
    @BindView(R.id.winds_recycler_view)
    RecyclerView mWindsRecyclerView;
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

    private Marker mCurrentMarker;

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

    public static final String[] WINDS_PROJECTION = {
            TidesContract.TidesEntry.COLUMN_WINDS_DATE,
            TidesContract.TidesEntry.COLUMN_TIME_OF_WIND,
            TidesContract.TidesEntry.COLUMN_WIND_DIR_DEG,
            TidesContract.TidesEntry.COLUMN_WIND_SPEED,
            TidesContract.TidesEntry.COLUMN_WIND_DIRECTION
    };
    public static final int INDEX_WIND_DATE = 0;
    public static final int INDEX_WIND_TIME = 1;
    public static final int INDEX_WIND_DIR_DEG = 2;
    public static final int INDEX_WIND_SPEED = 3;
    public static final int INDEX_WIND_DIR = 4;


    public static final String EXTRA_TIDE_QUERY_DATE = "tides_date";

    String TAG = TidesFragment.class.getSimpleName();
    private static final String SELECTED_STYLE = "selected_style";
    private static final String MAP_ZOOM = "map_zoom";
    private static final String PLACE_NAME = "location_name";
    private static final String LOCATION = "location";
    private static final String CONTAINER_VISIBILITY = "visibility";

    private static final int FORECAST_DAYS = 7;
    private GoogleMap mMap = null;
    private View mLocationButton;
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
    private static final int MAP_ZOOM_DEFAULT = 14;
    private static LatLng LAT_LNG;
    private Location mLocation;
    private TidesDataAdapter mTidesAdapter;
    private WindsDataAdapter mWindsAdapter;
    private float mMapZoom = MAP_ZOOM_DEFAULT;
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
            Timber.d("Saved not null");
            //  mSelectedStyleId = savedInstanceState.getInt(SELECTED_STYLE);
            mMapZoom = savedInstanceState.getFloat(MAP_ZOOM);
            double longitude = savedInstanceState.getDouble(MainActivityFull.EXTRA_LONGITUDE);
            double latitude = savedInstanceState.getDouble(MainActivityFull.EXTRA_LATITUDE);

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(latitude));
            editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(longitude));
            editor.apply();
            Timber.d("Loaded LAT: " + latitude);
            LAT_LNG = new LatLng(latitude, longitude);
        }

        mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis())).apply();
        mTidesAdapter = new TidesDataAdapter(getActivity());
        mWindsAdapter = new WindsDataAdapter(getActivity());

    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        mLocationButton = ((View) view.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) mLocationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        rlp.setMargins(0, 240, 32, 0);


        ButterKnife.bind(this, view);

        if (savedInstanceState != null)

        {
            mVisibility = savedInstanceState.getInt(CONTAINER_VISIBILITY);
            mContainer.setVisibility(mVisibility);
//            mMap.getUiSettings().setZoomGesturesEnabled(!(mVisibility == View.VISIBLE)); TODO maps er null her vett
        }

        mTidesRecyclerView.setLayoutManager(new

                LinearLayoutManager(getActivity()));
        mTidesRecyclerView.setAdapter(mTidesAdapter);

        mWindsRecyclerView.setLayoutManager(new

                LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        mWindsRecyclerView.setAdapter(mWindsAdapter);
        return view;
    }

    private void initLocation() {
        try {
            mLocation = getArguments().getParcelable(LOCATION);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        boolean useHomeLocation = true;
        SharedPreferences.Editor editor = mPreferences.edit();
        String longitude, latitude;
        if (mLocation != null) {
            LAT_LNG = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            longitude = String.valueOf(mLocation.getLongitude());
            latitude = String.valueOf(mLocation.getLatitude());
        } else {
            longitude = getString(R.string.default_longitude);
            latitude = getString(R.string.default_latitude);
            LAT_LNG = new LatLng(Double.valueOf(latitude),
                    Double.valueOf(longitude));
            useHomeLocation = false;
            showSnackbar("Set to default location: Trondheim");
        }
        editor.putString(MainActivityFull.EXTRA_LONGITUDE, longitude);
        editor.putString(MainActivityFull.EXTRA_LATITUDE, latitude);
        editor.commit();

        updateValuesOnLocationChange(useHomeLocation);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (savedInstanceState == null) {
            Timber.d("saved == null");
            initLocation();
        } else restartLoader(false);

        mResetLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initLocation();
                onMapReady(mMap);
                mMapZoom = MAP_ZOOM_DEFAULT;
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
                        mDateTimeTextView.setText(Utils.getPrettyDate(Utils.getDateInMillisec(tomorrow))); // TODO not very efficient, lag egen metode
                        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_WINDS, null, TidesFragment.this);
                    }
                } catch (ParseException e) {
                    Timber.e("failed to increase date");
                    e.printStackTrace();
                }
                mPrevDay.setVisibility(View.VISIBLE);
            }
        });
        mPrevDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()));
                try {
                    String yesterday = Utils.getDateMinusOne(currentDate);
                    mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, yesterday).apply();
                    mDateTimeTextView.setText(Utils.getPrettyDate(Utils.getDateInMillisec(yesterday)));
                    getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                    getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_WINDS, null, TidesFragment.this);
                } catch (ParseException e) {
                    Timber.e("failed to decrease date");
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Utils.workingConnection(getActivity())) {
            mNextDayImg.setVisibility(View.INVISIBLE);
            mPrevDay.setVisibility(View.INVISIBLE);
            showSnackbar(getString(R.string.connection_error));
        }
    }

    private void showSnackbar(final String text) {
        View container = getActivity().findViewById(R.id.tide_content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    private void restartLoader(boolean homeLocation) {
        try {
            //mLocationTextView.setText(Utils.getPlaceName(getActivity(), homeLocation));
            mLocationTextView.setText(Utils.getAccuratePlaceName(getActivity(), homeLocation));
            String dateShown = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()));
            mDateTimeTextView.setText(Utils.getPrettyDate(Utils.getDateInMillisec(dateShown)));
        } catch (IOException | NullPointerException | ParseException e) {
            e.printStackTrace();
            mErrorTextView.setText(R.string.error_unknown);
        }
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_TIDES, null, this);
        getActivity().getSupportLoaderManager().restartLoader(LOADER_ID_WINDS, null, this);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Timber.d("onCr Loader");
        String sortOrder, selection;
        String[] selectionArgs = new String[]{mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                Utils.getDate(System.currentTimeMillis()))};
        switch (i) {
            case LOADER_ID_TIDES:
                sortOrder = TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC";
                selection = TidesContract.TidesEntry.COLUMN_TIDES_DATE + "=?";

                return new android.support.v4.content.CursorLoader(getActivity(), TidesContract.TidesEntry.CONTENT_URI_TIDES,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder);

            case LOADER_ID_WINDS:
                sortOrder = TidesContract.TidesEntry.COLUMN_TIME_OF_WIND + " ASC";
                selection = TidesContract.TidesEntry.COLUMN_WINDS_DATE + "=?";

                return new android.support.v4.content.CursorLoader(getActivity(), TidesContract.TidesEntry.CONTENT_URI_WINDS,
                        WINDS_PROJECTION, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cursor) {
        Timber.d("onLoad finies, count: " + cursor.getCount());
        mContainer.setVisibility(mVisibility);
        mMap.getUiSettings().setZoomControlsEnabled(!(mContainer.getVisibility() == View.VISIBLE));
        String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                Utils.getDate(System.currentTimeMillis()));
        if (currentDate.compareTo(Utils.getDate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))) < 0) {
            mPrevDay.setVisibility(View.INVISIBLE);
        }
        if (loader.getId() == LOADER_ID_WINDS) {
            mWindsAdapter.swapCursor(cursor);
        }
        if (loader.getId() == LOADER_ID_TIDES) { // TODO method
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
                mTidesAdapter.swapCursor(null);
            } else {
                mTidesAdapter.swapCursor(cursor);
                mErrorTextView.setVisibility(View.GONE);
                mNextDayImg.setVisibility(View.VISIBLE);
            }
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mTidesAdapter.swapCursor(null);
        mWindsAdapter.swapCursor(null);
    }

    private void updateValuesOnLocationChange(final boolean homeLocation) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                StatsnailSyncTask.syncData(getActivity(), homeLocation);
            }
        });
        thread.start();
        restartLoader(homeLocation);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mResetLoc.setVisibility(View.GONE);
   /*     mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(!(mVisibility == View.VISIBLE));
        mMap.getUiSettings().setZoomGesturesEnabled(true);*/

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG, mMapZoom));

        String maptype = mPreferences.getString(getString(R.string.pref_map_type_key), getString(R.string.map_type_def_value));
        if (maptype.equals(String.valueOf(GoogleMap.MAP_TYPE_HYBRID)) ||
                maptype.equals(String.valueOf(GoogleMap.MAP_TYPE_SATELLITE)))
            mResetLoc.setBackgroundColor(Color.WHITE);
        if (maptype.equals(String.valueOf(GoogleMap.MAP_TYPE_NONE)))
            mResetLoc.setVisibility(View.GONE);

        mMap.setMapType(Integer.parseInt(maptype));
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

                updateValuesOnLocationChange(false);
                mVisibility = View.VISIBLE;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                LAT_LNG = latLng;
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()));
                editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(latLng.latitude));
                editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(latLng.longitude));
                editor.commit();
                String place = "";
                try {
                    place = Utils.getAccuratePlaceName(getActivity(), latLng);
                    Timber.d(Utils.getAccuratePlaceName(getActivity(), latLng));
                } catch (IOException e) {

                }
                if (mCurrentMarker != null) mCurrentMarker.remove();
                mCurrentMarker = mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("View conditions")
                        .snippet(place));
                mCurrentMarker.showInfoWindow();
                mMap.setOnInfoWindowClickListener(TidesFragment.this);
            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                mContainer.setVisibility(View.GONE);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }
        });
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis()));
        editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(marker.getPosition().latitude));
        editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(marker.getPosition().longitude));
        editor.commit();
        updateValuesOnLocationChange(false);
        marker.hideInfoWindow();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mCurrentMarker.showInfoWindow();
        return false;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        initLocation();
        //onMapReady(mMap);
        mMapZoom = MAP_ZOOM_DEFAULT;
        //mVisibility = View.VISIBLE;
        return false;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the selected map style, so we can assign it when the activity resumes.
        //   outState.putInt(SELECTED_STYLE, mSelectedStyleId);
        if (mMap != null) outState.putFloat(MAP_ZOOM, mMap.getCameraPosition().zoom);
        // outState.putParcelable(LOCATION, mLocation);
        outState.putDouble(MainActivityFull.EXTRA_LATITUDE, LAT_LNG.latitude);
        outState.putDouble(MainActivityFull.EXTRA_LONGITUDE, LAT_LNG.longitude);
        Timber.d("SAVED LAT: " + LAT_LNG.latitude);
        //outState.putString(PLACE_NAME, mLocationTextView.getText().toString());
        outState.putInt(CONTAINER_VISIBILITY, mContainer.getVisibility());
        super.onSaveInstanceState(outState);
    }

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Creates a {@link MapStyleOptions} object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the {@link GoogleMap} instance,
     * via the setMapStyle() method.
     */
    private void setSelectedStyle() {
/*        mSelectedStyleId = Integer.valueOf(
                mPreferences.getString(getString(R.string.map_pref_key), getString(R.string.style_value_default)));*/
        String mapStyle = mPreferences.getString(getString(R.string.map_pref_key), getString(R.string.style_label_default));

        Timber.d("setSelStyle style: " + getString(mSelectedStyleId));
        MapStyleOptions style;
        switch (mapStyle) {
            case "Retro":
                // Sets the retro style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_retro);
                break;
            case "Night":
                // Sets the night style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_night);
                break;
            case "Grayscale":
                // Sets the grayscale style via raw resource JSON.
                style = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.mapstyle_grayscale);
                break;
            case "No POIs or transit":
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
            case "Default":
                // Removes previously set style, by setting it to null.
                style = null;
                break;
            default:
                return;
        }
        mMap.setMapStyle(style);
    }
}