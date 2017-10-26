package com.statsnail.roberts.statsnail.fragments;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.adapters.TidesDataAdapter;
import com.statsnail.roberts.statsnail.models.LocationData;
import com.statsnail.roberts.statsnail.models.Station;
import com.statsnail.roberts.statsnail.utils.NetworkUtils;
import com.statsnail.roberts.statsnail.utils.Utils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Created by Adrian on 24/10/2017.
 */

public class TidesFragment extends Fragment {
    @BindView(R.id.tides_recycler_view)
    RecyclerView mTidesRecyclerView;
    @BindView(R.id.location_name)
    TextView mLocationTextView;
    @BindView(R.id.forecast_date)
    TextView mDateTimeTextView;

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
        Timber.d("TIIIMEEE:: " + Utils.getDate());
        String tideDataUrl = Utils.getUrlFromLocation((Location) getArguments().getParcelable("location"));
        new DownloadNearbyXmlTask(getActivity()).execute(tideDataUrl);
        //new DownloadAllXmlTask().execute("http://api.sehavniva.no/tideapi.php?tide_request=stationlist&type=perm");
        //getActivity().getWindow().findViewById(R.id.cardview).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStart() {
        super.onStart();

        mTides.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    DataSnapshot todays = dataSnapshot.child(Utils.getDate());
                    DataSnapshot waterlevels = todays.child("waterlevels");

                    TidesDataAdapter mAdapter = new TidesDataAdapter(getActivity(), waterlevels);
                    mTidesRecyclerView.setAdapter(mAdapter);
                    mTidesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

                    for (DataSnapshot snapshot : todays.child("waterlevels").getChildren()) {
                        Timber.d("snapshots: " + snapshot.child("time").getValue());
                    }
                    String stationName = todays.child("stationName").getValue().toString();
                    String stationCode = todays.child("stationCode").getValue().toString();


                    mLocationTextView.setText(stationName + "\n(" + stationCode + ")");
                    mDateTimeTextView.setText(Utils.getDate());

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tides, container, false);
        ButterKnife.bind(this, view);

        return view;
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
            if (result != null) {
                DatabaseReference tides = mRootRef.child("tides");
                DatabaseReference date = tides.child(Utils.getDate());
                DatabaseReference waterlevels = date.child("waterlevels");
                DatabaseReference stationName = date.child("stationName");
                DatabaseReference stationCode = date.child("stationCode");

                stationName.setValue(result.stationName);
                stationCode.setValue(result.stationCode);
                if (result.waterlevels != null) {
                    for (int i = 0; i < result.waterlevels.size(); i++) {
                        DatabaseReference number = waterlevels.child("" + i);
                        DatabaseReference time = number.child("time");
                        DatabaseReference level = number.child("level");
                        DatabaseReference flag = number.child("flag");

                        level.setValue(result.waterlevels.get(i).waterValue);
                        flag.setValue(result.waterlevels.get(i).flag);
                        time.setValue(Utils.getFormattedTime(result.waterlevels.get(i).dateTime));
                    }
                }
            } else

                Toast.makeText(getActivity(), "Error: " + result.errorResponse, Toast.LENGTH_SHORT).show();
        }
    }
}
