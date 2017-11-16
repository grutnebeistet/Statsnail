package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.models.Station;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Created by Adrian on 23/10/2017.
 */

public final class NetworkUtils {
    private final static String TIDES_BASE_URL = "http://api.sehavniva.no/tideapi.php?";
    private final static String TIDES_PARAM_LAT = "lat";
    private final static String TIDES_PARAM_LONG = "lon";
    private final static String TIDES_PARAM_FROM = "fromtime";
    private final static String TIDES_PARAM_UNTIL = "totime";
    private final static String TIDES_PARAM_TIME_SUFFIX = "T00%3A00";
    private final static String TIDES_LANGUAGE_PREFIX = "datatype=tab&refcode=cd&place=&file=&lang";
    private final static String TIDES_LANGUAGE_SUFFIX = "&interval=60&dst=0&tzone=1&tide_request=locationdata";
    static Location mLocation;

    public static String buildTidesRequestUrl(Context context, boolean homeLocation) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultLat = context.getResources().getString(R.string.default_latitude);
        String defaultLon = context.getResources().getString(R.string.default_longitude);

        String latitude = homeLocation ? preference.getString(MainActivityFull.HOME_LAT, defaultLat) :
                preference.getString(MainActivityFull.EXTRA_LATITUDE, defaultLat);
        Timber.d("LAT i buildTidesRequestUrl: " + latitude);
        String longitude = homeLocation ? preference.getString(MainActivityFull.HOME_LON, defaultLon) :
                preference.getString(MainActivityFull.EXTRA_LONGITUDE, defaultLon);

        String language = "en"; // TODO language setting
        String fromDate = Utils.getDate(System.currentTimeMillis());
        long offset = TimeUnit.DAYS.toMillis(7);
        String tillDate = Utils.getDate(System.currentTimeMillis() + offset);

        return "http://api.sehavniva.no/tideapi.php?lat=" + //63.4581662 +
                latitude +
                "&lon=" + //10.2795140 +
                longitude +
                "&fromtime=" +
                fromDate + "T00%3A00" +
                "&totime=" +
                tillDate +
                "T00%3A00" +
                "&datatype=tab&refcode=cd&place=&file=&lang=" + language + "&interval=60&dst=0&tzone=1&tide_request=locationdata";

    }

    public static String buildWindsRequestUrl(Context context, boolean homeLocation) {
       // String base = "https://beta.api.met.no/weatherapi/spotwind/1.0/?";
        String base = "http://api.met.no/weatherapi/locationforecast/1.9/";
        //?lat=60.10;lon=9.58";

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultLat = context.getResources().getString(R.string.default_latitude);
        String defaultLon = context.getResources().getString(R.string.default_longitude);

        String latitude = homeLocation ? preference.getString(MainActivityFull.HOME_LAT, defaultLat) :
                preference.getString(MainActivityFull.EXTRA_LATITUDE, defaultLat);
        Timber.d("LAT i buildTidesRequestUrl: " + latitude);
        String longitude = homeLocation ? preference.getString(MainActivityFull.HOME_LON, defaultLon) :
                preference.getString(MainActivityFull.EXTRA_LONGITUDE, defaultLon);

        Uri queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(TIDES_PARAM_LAT, latitude)
                .appendQueryParameter(TIDES_PARAM_LONG, longitude)
                .build();

        return queryUri.toString();

    }

    public static ContentValues[] loadNearbyXml(Context context, String url) throws XmlPullParserException, IOException {
        InputStream inputStream = null;
        HydrographicsXmlParser parser = new HydrographicsXmlParser();
        ContentValues[] tidesValues;

        try {
            inputStream = downloadUrl(url);
            //tidesData = parser.parseNearbyStation(inputStream);
            tidesValues = parser.parseNearbyStation(context, inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return tidesValues;
    }

    public static ContentValues[] loadWindsXml(String url) throws XmlPullParserException, IOException {
        InputStream inputStream = null;
        YrApiXmlParser parser = new YrApiXmlParser();
        ContentValues[] windsValues;

        try {
            inputStream = downloadUrl(url);
            windsValues = parser.parseWinds(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return  windsValues;
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
        Timber.d("Url: " + urlString);
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
