package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Xml;

import com.statsnail.roberts.statsnail.R;
import com.statsnail.roberts.statsnail.activities.MainActivityFull;
import com.statsnail.roberts.statsnail.data.TidesContract;
import com.statsnail.roberts.statsnail.models.TidesData;
import com.statsnail.roberts.statsnail.models.Station;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import timber.log.Timber;

import static com.statsnail.roberts.statsnail.fragments.TidesFragment.EXTRA_TIDE_QUERY_DATE;

/**
 * Created by Adrian on 23/10/2017.
 */

public class HydrographicsXmlParser {
    private static final String ns = null;
    Context mContext;

    public ContentValues[] parseNearbyStation(Context context, InputStream in) throws XmlPullParserException, IOException {
        mContext = context;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            return readNearbyStationEntry(parser);


        } finally {
            in.close();
        }
    }

    public ArrayList<Station> parseStations(InputStream in) throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readStationsEntry(parser);
        } finally {
            in.close();
        }
    }

    private ArrayList<Station> readStationsEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<Station> stations = new ArrayList<>();

        if (parser.getName().equals("error")) {
            if (parser.next() == XmlPullParser.TEXT) {
                Timber.e("Error: " + parser.getText());
            }
        }
        parser.require(XmlPullParser.START_TAG, ns, "tide");

        String stationName = null;
        String stationCode = null;
        String latitude = null;
        String longitude = null;

        while (!(parser.getName().equals("location"))) parser.nextTag(); // TODO cleany
        while ((parser.getName().equals("location"))) {
            int attrCount = parser.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                switch (name) {
                    case "name":
                        stationName = value;
                        break;
                    case "code":
                        stationCode = value;
                        break;
                    case "latitude":
                        latitude = value;
                        break;
                    case "longitude":
                        longitude = value;
                        break;
                }
                stations.add(new Station(stationName, stationCode, latitude, longitude));
            }
            parser.nextTag();
        }
        return stations;
    }

    private ContentValues[] readNearbyStationEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        ContentValues[] tidesValues;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (parser.getName().equals("error")) {
            if (parser.next() == XmlPullParser.TEXT) {
                tidesValues = new ContentValues[1];
                Timber.e("Error: " + parser.getText());
                ContentValues error = new ContentValues();
                error.put(TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, parser.getText());
                error.put(TidesContract.TidesEntry.COLUMN_TIDES_DATE,
                        preferences.getString(EXTRA_TIDE_QUERY_DATE,
                                Utils.getDate(System.currentTimeMillis())));
                tidesValues[0] = error;
                return tidesValues;

//                return new TidesData(null, null, null, null, null, null, parser.getText());

            }
        }
        parser.require(XmlPullParser.START_TAG, ns, "tide");

        String stationName = null;
        String stationCode = null;
        String latitude = null;
        String longitude = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equals("nodata") ||
                    parser.getName().equals("service")) {
                if (parser.getAttributeName(0).equals("info") ||
                        parser.getAttributeName(0).equals("cominfo")) {
                    tidesValues = new ContentValues[1];
                    ContentValues error = new ContentValues();
                    error.put(TidesContract.TidesEntry.COLUMN_TIDE_ERROR_MSG, parser.getAttributeValue(0));
                    error.put(TidesContract.TidesEntry.COLUMN_TIDES_DATE,
                            preferences.getString(EXTRA_TIDE_QUERY_DATE,
                                    Utils.getDate(System.currentTimeMillis())));
                    tidesValues[0] = error;
                    return tidesValues;
                }
                //return new TidesData(null, null, null, null, null, null, parser.getAttributeValue(0));

            }
            if (parser.getName().equals("location")) {
                int attributeCount = parser.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    String attrName = parser.getAttributeName(i);
                    String attrValue = parser.getAttributeValue(i);
                    Timber.d("Attr Value: " + attrValue);
                    switch (attrName) {
                        case "name":
                            stationName = attrValue;
                            break;
                        case "code":
                            stationCode = attrValue;
                            break;
                        case "latitude":
                            latitude = attrValue;
                            break;
                        case "longitude":
                            longitude = attrValue;
                            break;
                    }
                }
            }
        }
        // Start parsing from data tag
        String dataType = null;
        String waterValue;
        String atTime;
        String flag;

        ArrayList<TidesData.Waterlevel> waterlevels = new ArrayList<>();
        int levelsIndex = 0;
        Timber.d(parser.next() + " = next");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            if (name.equals("data")) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG)
                        continue;

                    name = parser.getName();
                    if (name.equals("waterlevel")) {
                        waterValue = parser.getAttributeValue(0);
                        atTime = parser.getAttributeValue(1);
                        flag = parser.getAttributeValue(2);
                        waterlevels.add(levelsIndex, new TidesData.Waterlevel(waterValue, atTime, flag));

                        levelsIndex++;
                    }
                    parser.nextTag();
                }

                // prepare notification only if parsed area is actual location
                // No point in checking levels beyond the first 6 regarding notification
                String homeLat = preferences.getString(
                        MainActivityFull.HOME_LAT, mContext.getString(R.string.default_latitude));
                String homeLong = preferences.getString(
                        MainActivityFull.HOME_LON, mContext.getString(R.string.default_longitude));

                Timber.d("Parsing hydrographiics...");
                // and only if the data being parsed is from users actual (home) location
                if (latitude != null && longitude != null &&
                        latitude.substring(0, 7).equals(homeLat.substring(0, 7))
                        && longitude.substring(0, 7).equals(homeLong.substring(0, 7)))
                    NotificationUtils.prepareNotification(mContext.getApplicationContext(), waterlevels.subList(0, 6));

                tidesValues = new ContentValues[waterlevels.size()];

                for (int i = 0; i < waterlevels.size(); i++) {
                    ContentValues values = new ContentValues();
                    values.put(TidesContract.TidesEntry.COLUMN_TIDES_DATE,
                            Utils.getFormattedDate(waterlevels.get(i).dateTime));
                    values.put(TidesContract.TidesEntry.COLUMN_WATER_LEVEL,
                            waterlevels.get(i).waterValue);
                    values.put(TidesContract.TidesEntry.COLUMN_LEVEL_FLAG,
                            waterlevels.get(i).flag);
                    values.put(TidesContract.TidesEntry.COLUMN_TIME_OF_LEVEL,
                            Utils.getFormattedTime(waterlevels.get(i).dateTime));
                    tidesValues[i] = values;
                }
                return tidesValues;

            } else {
                skip(parser);
            }
        }
        return null;
        //return new TidesData(stationName, stationCode, latitude, longitude, dataType, waterlevels, null);
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
