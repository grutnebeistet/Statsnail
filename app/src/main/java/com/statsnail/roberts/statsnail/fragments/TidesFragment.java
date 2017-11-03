package com.statsnail.roberts.statsnail.fragments;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
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
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.adapters.TidesDataAdapter;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.models.Station;
import com.statsnail.roberts.statsnail.sync.NotifyService;
import com.statsnail.roberts.statsnail.sync.StatsnailSyncTask;
import com.statsnail.roberts.statsnail.utils.NetworkUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesFragment extends Fragment implements OnMapReadyCallback, LoaderManager.LoaderCallbacks<Cursor> {
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
    @BindView(R.id.button_notify)
    Button satanisme;

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
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mTides = mRootRef.child("tides");
    private TidesDataAdapter mAdapter;
    private TidesData mTidesData;

    SharedPreferences mPreferences;

    public static TidesFragment newInstance(Location location) {
        Bundle args = new Bundle();
        args.putParcelable("location", location);
        TidesFragment f = new TidesFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedStyleId = savedInstanceState.getInt(SELECTED_STYLE);
        }

        mLocation = getArguments().getParcelable("location");
        LAT_LNG = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDate(System.currentTimeMillis())).commit();
        mAdapter = new TidesDataAdapter(getActivity());

        getLoaderManager().initLoader(LOADER_ID_TIDES, null, this);
        //getActivity().getWindow().findViewById(R.id.cardview).setVisibility(View.INVISIBLE);


    }


    private void testNot() {
        Intent myIntent = new Intent(getActivity(), NotifyService.class);
        myIntent.putExtra("nextLowTideTime", "13:29");
        myIntent.putExtra("nextLowTideLevel", "123cm");

        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(getActivity().getApplicationContext(), 0, myIntent, 0);

        long notificationTime = System.currentTimeMillis();

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MapFragment mapFragment = ((MapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);

        mNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()));
                try {
                    mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDatePlusOne(currentDate)).commit();
                    getLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                } catch (ParseException e) {
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
                    mPreferences.edit().putString(EXTRA_TIDE_QUERY_DATE, Utils.getDateMinusOne(currentDate)).commit();
                    getLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                currentDate = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()));
                if (currentDate.equals(Utils.getDate(System.currentTimeMillis())))
                    mPrevDay.setVisibility(View.INVISIBLE);
            }
        });
        mContainer.setVisibility(View.VISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        ButterKnife.bind(this, view);
        mTidesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mTidesRecyclerView.setAdapter(mAdapter);
        satanisme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testNot();
            }
        });

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOADER_ID_TIDES:
                String sortOrder = TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL + " ASC";
                String selection = TidesContract.TidesEntry.COLUMN_TIDES_DATE + "=?";
                String[] selectionArgs = new String[]{mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                        Utils.getDate(System.currentTimeMillis()))};

                return new CursorLoader(getActivity(), TidesContract.TidesEntry.CONTENT_URI,
                        TIDES_PROJECTION, selection, selectionArgs, sortOrder);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Timber.d("cursor size.: " + cursor.getCount());
        cursor.setNotificationUri(getActivity().getContentResolver(), TidesContract.TidesEntry.CONTENT_URI);

        if (cursor == null || cursor.getCount() == 0) {
            mErrorTextView.setText(R.string.connection_error);
        } else if (cursor.getCount() == 1) {
            cursor.moveToFirst();
            mErrorTextView.setVisibility(View.VISIBLE);
            mErrorTextView.setText(cursor.getString(INDEX_ERROR));
            //  Toast.makeText(getActivity(), "Error: " + cursor.getString(INDEX_ERROR), Toast.LENGTH_SHORT).show();
            mAdapter.swapCursor(null);
        } else {
            mAdapter.swapCursor(cursor);
            mErrorTextView.setVisibility(View.GONE);
        }
        try {
            mLocationTextView.setText(Utils.getPlaceName(getActivity()));
            String dateShown = mPreferences.getString(EXTRA_TIDE_QUERY_DATE,
                    Utils.getDate(System.currentTimeMillis()));
            mDateTimeTextView.setText(Utils.getPrettyDate(Utils.getDateInMillisec(dateShown)));
        } catch (IOException | NullPointerException | ParseException e) {
            e.printStackTrace();
            mErrorTextView.setText("Unknown Error");
            return;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG, 8));

        setSelectedStyle();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Timber.d("Map Clicked latitude: " + latLng.latitude);

                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(MainActivityFull.EXTRA_LATITUDE, String.valueOf(latLng.latitude));
                editor.putString(MainActivityFull.EXTRA_LONGITUDE, String.valueOf(latLng.longitude));
                editor.commit();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StatsnailSyncTask.syncData(getActivity());

                    }
                });
                thread.start();

                getLoaderManager().restartLoader(LOADER_ID_TIDES, null, TidesFragment.this);
                mContainer.setVisibility(View.VISIBLE);
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
                //getActivity().findViewById(R.id.cardview_container).setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the selected map style, so we can assign it when the activity resumes.
        outState.putInt(SELECTED_STYLE, mSelectedStyleId);
        super.onSaveInstanceState(outState);
    }


    protected class DownloadStationsInfoXmlTask extends AsyncTask<String, Void, ArrayList<Station>> {
        @Override
        protected ArrayList<Station> doInBackground(String... urls) {
            try {
                return NetworkUtils.loadAllStationsXml(urls[0]);
            } catch (IOException e) {
                Timber.e("IOException");
                return null;
            } catch (XmlPullParserException e) {
                Timber.e("XmlPullParserException");
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stations) {
            super.onPostExecute(stations);
            Timber.d("post ex ALL: " + stations.get(1).stationName + "   " + stations.get(1).longitude);
            Timber.d("post ex ALL: " + stations.get(2).stationName + "   " + stations.get(2).longitude);
        }
    }

 /*   protected class DownloadNearbyXmlTask extends AsyncTask<String, Void, TidesData> {

        private Context mContext;

        private DownloadNearbyXmlTask(Context context) {
            mContext = context;
        }

        @Override
        protected TidesData doInBackground(String... urls) {
            try {
                return NetworkUtils.loadNearbyXml(urls[0]);
            } catch (IOException e) {
                Timber.e("IOException");
                return null;
            } catch (XmlPullParserException e) {
                Timber.e("XmlPullParserException");
                return null;
            }
        }

        @Override
        protected void onPostExecute(TidesData result) {
            try {
                mLocationTextView.setText(Utils.getPlaceName(getActivity(), mLocation));
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
            mDateTimeTextView.setText(Utils.getPrettyDate(System.currentTimeMillis()));

            if (result.errorResponse != null) {
                mErrorTextView.setText(result.errorResponse);
                Toast.makeText(getActivity(), "Error: " + result.errorResponse, Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                Timber.d("Result NOT NULL");
                TidesDataAdapter mAdapter = new TidesDataAdapter(getActivity(), result.waterlevels);
                mTidesRecyclerView.setAdapter(mAdapter);
                mTidesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                saveResultsToPref(result);
                //  mLocationTextView.setText(result.stationName + "\n(" + result.stationCode + ")");
            }
        }
    }*/


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
