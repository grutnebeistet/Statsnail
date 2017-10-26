package com.statsnail.roberts.statsnail.utils;

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

public class NetworkUtils {
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
