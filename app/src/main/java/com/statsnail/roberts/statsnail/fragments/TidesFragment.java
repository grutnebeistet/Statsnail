package com.statsnail.roberts.statsnail.fragments;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.adapters.TidesDataAdapter;
import com.statsnail.roberts.statsnail.models.LocationData;
import com.statsnail.roberts.statsnail.models.Station;
import com.statsnail.roberts.statsnail.sync.NotifyService;
import com.statsnail.roberts.statsnail.sync.SyncUtils;
import com.statsnail.roberts.statsnail.utils.NetworkUtils;
import com.statsnail.roberts.statsnail.utils.NotificationUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesFragment extends Fragment implements OnMapReadyCallback {
    @BindView(R.id.tides_recycler_view)
    RecyclerView mTidesRecyclerView;
    @BindView(R.id.location_name)
    TextView mLocationTextView;
    @BindView(R.id.forecast_date)
    TextView mDateTimeTextView;
    @BindView(R.id.tides_error_tv)
    TextView mErrorTextView;

    @BindView(R.id.button_notify)
    Button satanisme;
/*    @BindView(R.id.image_button_curr_loc)
    ImageButton mCurrentLocButton;*/

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
        String tideDataUrl = Utils.getUrlFromLocation(mLocation);
        new DownloadNearbyXmlTask(getActivity()).execute(tideDataUrl);
        // new DownloadStationsInfoXmlTask().execute("http://api.sehavniva.no/tideapi.php?tide_request=stationlist&type=perm");
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
        Timber.d(TAG + "   onViewCreated");
        MapFragment mapFragment = ((MapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        ButterKnife.bind(this, view);
        satanisme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testNot();
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LAT_LNG, 14));
        setSelectedStyle();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //    findViewById(R.id.cardview).setVisibility(View.INVISIBLE);
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

    protected class DownloadNearbyXmlTask extends AsyncTask<String, Void, LocationData> {

        private Context mContext;

        private DownloadNearbyXmlTask(Context context) {
            mContext = context;
        }

        @Override
        protected LocationData doInBackground(String... urls) {
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
        protected void onPostExecute(LocationData result) {
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
    }

    private void saveResultsToPref(LocationData result) {
        //SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("latitude", result.latitude);
        editor.putString("longitude", result.longitude);

        // get next low tide to notify about
        LocationData.Waterlevel nextLow = null;
        LocationData.Waterlevel nextHighAfterLow = null;
        for (int i = 0; i < result.waterlevels.size(); i++) {
            LocationData.Waterlevel l = result.waterlevels.get(i);
            if (l.flag.equals("low") && Utils.timeIsAfterNow(Utils.getFormattedTime(l.dateTime))) {
                nextLow = (nextLow == null || (l.dateTime.compareTo(nextLow.dateTime) < 0) ? l : nextLow);
                if (i + 1 < result.waterlevels.size())
                    nextHighAfterLow = result.waterlevels.get(i + 1);
            }


        }

        if (nextLow != null) {
            Timber.d("nextLow not null....");
/*            editor.putString("latitude", result.latitude);
            editor.putString("longitude", result.longitude);
            editor.putString("nextLowTideTime", nextLow.dateTime);
                editor.putString("nextLowTideLevel", nextLow.waterValue);*/

            Intent myIntent = new Intent(getActivity(), NotifyService.class);
            myIntent.putExtra("nextLowTideTime", Utils.getFormattedTime(nextLow.dateTime));
            myIntent.putExtra("nextLowTideLevel", nextLow.waterValue);
            if (nextHighAfterLow != null) {
                myIntent.putExtra("nextHighTideTime", Utils.getFormattedTime(nextHighAfterLow.dateTime));
                myIntent.putExtra("nextHighTideLevel", nextHighAfterLow.waterValue);
            }
            AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(getActivity().getApplicationContext(), 0, myIntent, 0);

            String lowTideTime = Utils.getFormattedTime(nextLow.dateTime);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(lowTideTime.substring(0, 2)));
            calendar.set(Calendar.MINUTE, Integer.valueOf(lowTideTime.substring(3, 5)));
            long offset = TimeUnit.HOURS.toMillis(3);
            long notificationTime = calendar.getTimeInMillis() - offset;
            if ((notificationTime + offset) > System.currentTimeMillis())
                notificationTime = System.currentTimeMillis() + 63000;

            Timber.d("Notific time: " + Utils.getTime(notificationTime) + "\nTidelevel: " + nextLow.waterValue);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
            //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);  //set repeating every 24 hours
        }

        // SyncUtils.initialize(getActivity());
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
