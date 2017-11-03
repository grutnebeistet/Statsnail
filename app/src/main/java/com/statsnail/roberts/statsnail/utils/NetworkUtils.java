package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.models.Station;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

    public static void updateTidesData(Context context, Location location) {

    }

    public static String buildTidesRequestUrl(Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String latitude = preference.getString(
                MainActivityFull.EXTRA_LATITUDE, context.getResources().getString(R.string.default_latitude));
        Timber.d("LAT i buildTidesRequestUrl: " + latitude);
        String longitude = preference.getString(
                MainActivityFull.EXTRA_LONGITUDE, context.getResources().getString(R.string.default_longitude));
        String language = "en";
        String fromDate = Utils.getDate(System.currentTimeMillis());
        long offset = TimeUnit.DAYS.toMillis(5);
        String tillDate = Utils.getDate(System.currentTimeMillis() + offset);

/*        Uri queryUri = Uri.parse(TIDES_BASE_URL).buildUpon()
                .appendQueryParameter(TIDES_PARAM_LAT, latitude)
                .appendQueryParameter(TIDES_PARAM_LONG, longitude)
                .appendQueryParameter(TIDES_PARAM_FROM, fromDate)
                .appendQueryParameter(TIDES_PARAM_UNTIL, tillDate)
                .appendQueryParameter(TIDES_LANGUAGE_PREFIX, language)
                .appendPath(TIDES_LANGUAGE_SUFFIX)
                .build();
        return queryUri.toString();
    }*//**/
   /*     try {
            return new URL(queryUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }*/

        http:
//api.sehavniva.no/tideapi.php?lat=58.974339&lon=5.730121&fromtime=2017-11-01T00%3A00&totime=2017-11-04T00%3A00&datatype=tab&refcode=cd&place=&file=&lang=en&interval=10&dst=0&tzone=1&tide_request=locationdata
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

    public static String buildWindsRequestUrl(Context context) {
        String base = "http://api.met.no/weatherapi/locationforecast/1.9/";  //?lat=60.10;lon=9.58

        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        String latitude = preference.getString(
                MainActivityFull.EXTRA_LATITUDE, context.getResources().getString(R.string.default_latitude));
        String longitude = preference.getString(
                MainActivityFull.EXTRA_LONGITUDE, context.getResources().getString(R.string.default_longitude));
        Uri queryUri = Uri.parse(base).buildUpon()
                .appendQueryParameter(TIDES_PARAM_LAT, latitude)
                .appendQueryParameter(TIDES_PARAM_LONG, longitude)
                .build();

        return queryUri.toString();

    }

    public static ContentValues[] loadNearbyXml(Context context,String url) throws XmlPullParserException, IOException {
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
           // windsValues = parser.parseNearbyStation(inputStream);
        } finally {
            if (inputStream != null) inputStream.close();
        }
        return null;// windsValues;
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
