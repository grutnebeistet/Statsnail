package com.statsnail.roberts.statsnail.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.statsnail.roberts.statsnail.utils.NetworkUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

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

    String TAG = TidesFragment.class.getSimpleName();
    private FusedLocationProviderClient mFusedLocationClient;
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
        // new DownloadAllXmlTask().execute("http://api.sehavniva.no/tideapi.php?tide_request=stationlist&type=perm");
        //getActivity().getWindow().findViewById(R.id.cardview).setVisibility(View.INVISIBLE);
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
        Timber.d(TAG + "   onCreateView");
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        Timber.d("onCreateview");
        ButterKnife.bind(this, view);


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


    protected class DownloadAllXmlTask extends AsyncTask<String, Void, ArrayList<Station>> {
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

            if (result.errorResponse != null) {
                mErrorTextView.setText(result.errorResponse);
                Toast.makeText(getActivity(), "Error: " + result.errorResponse, Toast.LENGTH_SHORT).show();
            } else if (result != null) {
                Timber.d("Result NOT NULL");
                TidesDataAdapter mAdapter = new TidesDataAdapter(getActivity(), result.waterlevels);
                mTidesRecyclerView.setAdapter(mAdapter);
                mTidesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                mLocationTextView.setText(result.stationName + "\n(" + result.stationCode + ")");
                mDateTimeTextView.setText(Utils.getDate());


            }
        }
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
        // mStyleIds stores each style's resource ID, and we extract the names here, rather
        // than using an XML array resource which AlertDialog.Builder.setItems() can also
        // accept. We do this since using an array resource would mean we would not have
        // constant values we can switch/case on, when choosing which style to apply.
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
