package com.statsnail.roberts.statsnail.utils;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivity;
import com.statsnail.roberts.statsnail.models.LocationData;
import com.statsnail.roberts.statsnail.models.Station;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by Adrian on 23/10/2017.
 */

public final class NetworkUtils {
    static Location mLocation;

    public static void updateTidesData(Context context, Location location) {

    }


    public static LocationData loadNearbyXml(String url) throws XmlPullParserException, IOException {
        InputStream inputStream = null;
        HydrographicsXmlParser parser = new HydrographicsXmlParser();
        LocationData locationData = null;

        try {
            inputStream = downloadUrl(url);
            locationData = parser.parseNearbyStation(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return locationData;
    }

    public static ArrayList<Station> loadAllStationsXml(String url) throws XmlPullParserException, IOException {
        InputStream inputStream = null;
        HydrographicsXmlParser parser = new HydrographicsXmlParser();
        ArrayList<Station> stations = null;

        try {
            inputStream = downloadUrl(url);
            stations = parser.parseStations(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return stations;
    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    private static InputStream downloadUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        InputStream stream = conn.getInputStream();

        return stream;
    }
}
