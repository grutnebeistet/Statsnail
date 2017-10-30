package com.statsnail.roberts.statsnail.utils;

import android.util.Xml;

import com.statsnail.roberts.statsnail.models.LocationData;
import com.statsnail.roberts.statsnail.models.Station;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by Adrian on 23/10/2017.
 */

public class HydrographicsXmlParser {
    private static final String ns = null;

    public LocationData parseNearbyStation(InputStream in) throws XmlPullParserException, IOException {
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
                Timber.d("Error: " + parser.getText());
            }
        }
        parser.require(XmlPullParser.START_TAG, ns, "tide");
        //

        String stationName = null;
        String stationCode = null;
        String latitude = null;
        String longitude = null;

     /*   while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }*/
        while (!(parser.getName().equals("location"))) parser.nextTag();
        Timber.d("Naaaaame" + parser.getName());
//        if (parser.getName().equals("location")) {
        while ((parser.getName().equals("location"))) {
            int attrCount = parser.getAttributeCount();
            for (int i = 0; i < attrCount; i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                Timber.d("VAlue all stations " + value);
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
            Timber.d("STATIONS: " + parser.getName());
        }

        return stations;
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.

    private LocationData readNearbyStationEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getName().equals("error")) {
            if (parser.next() == XmlPullParser.TEXT) {
                Timber.d("Error: " + parser.getText());

                return new LocationData(null, null, null, null, null, null, parser.getText());
            }
        }
        parser.require(XmlPullParser.START_TAG, ns, "tide");


        String locName = null;
        String locCode = null;
        String latitude = null;
        String longitude = null;

        //   while (!(parser.getName().equals("location"))) parser.nextTag();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equals("nodata")) {
                Timber.d("nodata  DATA FOUND");
                if (parser.getAttributeName(0).equals("info"))
                    return new LocationData(null, null, null, null, null, null, parser.getAttributeValue(0));

            }
            if (parser.getName().equals("location")) {
                int attributeCount = parser.getAttributeCount();
                for (int i = 0; i < attributeCount; i++) {
                    String attrName = parser.getAttributeName(i);
                    String attrValue = parser.getAttributeValue(i);
                    Timber.d("Attr Value: " + attrValue);
                    switch (attrName) {
                        case "name":
                            locName = attrValue;
                            break;
                        case "code":
                            locCode = attrValue;
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

        ArrayList<LocationData.Waterlevel> waterlevels = new ArrayList<>();
        int levelsIndex = 0;
        Timber.d(parser.next() + " = next");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            if (name.equals("data")) {
                dataType = parser.getAttributeValue(0);
                Timber.d("datatype" + dataType);
                while (parser.next() != XmlPullParser.END_TAG) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    name = parser.getName();
                    if (name.equals("waterlevel")) {
                        waterValue = parser.getAttributeValue(0);
                        Timber.d("water " + waterValue);
                        atTime = parser.getAttributeValue(1);
                        flag = parser.getAttributeValue(2);
                        waterlevels.add(levelsIndex, new LocationData.Waterlevel(waterValue, atTime, flag));
                        levelsIndex++;
                    }
                    parser.nextTag();
                }

            } else {
                skip(parser); // nødvendig?
            }
        }
        return new LocationData(locName, locCode, latitude, longitude, dataType, waterlevels, null);
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
