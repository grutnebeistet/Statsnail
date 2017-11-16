package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.util.Xml;

import com.statsnail.roberts.statsnail.data.TidesContract;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Created by Adrian on 25/10/2017.
 */

public class YrApiXmlParser {
    private static final String ns = null;
    private final static int WINDS_MAX_SIZE = 89;

    public ContentValues[] parseWinds(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();

            return readWindsXml(parser);

        } finally {
            in.close();
        }
    }

    private ContentValues[] readWindsXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        int evType = parser.getEventType();
        ContentValues[] windsValues = new ContentValues[WINDS_MAX_SIZE];
        String name = "";
        int index = 0;
        while (evType != XmlPullParser.END_DOCUMENT) {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                name = parser.getName();
                if ("time".equals(name)) {
                    String time = parser.getAttributeValue(1);

                    String windDir = "";
                    String winDirDeg = "";
                    String windSpeed = "";

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG)
                            continue;

                 /*                       && (Utils.getFormattedTime(time).equals("06:00") ||
                                Utils.getFormattedTime(time).equals("09:00") ||
                                Utils.getFormattedTime(time).equals("12:00") ||
                                Utils.getFormattedTime(time).equals("15:00") ||
                                Utils.getFormattedTime(time).equals("18:00") ||
                                Utils.getFormattedTime(time).equals("21:00"))*/

                        //<windDirection id="dd" name="SW" deg="214.3"/>
                        name = parser.getName();

                        if ("location".equals(name)) {
                            //  Timber.d("location == name");
                            ContentValues wind = new ContentValues();
                            while (parser.next() != XmlPullParser.END_TAG && index < WINDS_MAX_SIZE) {
                                if (parser.getEventType() != XmlPullParser.START_TAG)
                                    continue;
                                name = parser.getName();
                                //      Timber.d("name: " + name);
                                if ("windDirection".equals(name)) {
                                    windDir = parser.getAttributeValue(2);
                                    winDirDeg = parser.getAttributeValue(1);

                                    wind.put(TidesContract.TidesEntry.COLUMN_WIND_DIRECTION, windDir);
                                    wind.put(TidesContract.TidesEntry.COLUMN_WIND_DIR_DEG, winDirDeg);
                                }
                                if (("windSpeed".equals(name))) {
                                    windSpeed = parser.getAttributeValue(1);
                                    wind.put(TidesContract.TidesEntry.COLUMN_WIND_SPEED, windSpeed);
                                    //   Timber.d("Time: " + Utils.getFormattedTime(time) + "\nDate: " + Utils.getFormattedDate(time));
                                    wind.put(TidesContract.TidesEntry.COLUMN_WINDS_DATE,
                                            Utils.getFormattedDate(time));
                                    wind.put(TidesContract.TidesEntry.COLUMN_TIME_OF_WIND,
                                            Utils.getFormattedTime(time));

                                    windsValues[index] = wind;
                                    index++;
                                }
                                parser.nextTag(); // TODO make it skip through the rest (humidity etc)
                            }
                        }
                    }
                }
            }
            //Timber.d("YrParse get name: " + parser.getName());
            evType = parser.next();
        }
        Timber.d("windsvalues on return: " + windsValues[0].get(TidesContract.TidesEntry.COLUMN_WIND_SPEED));
        return windsValues;
    }
}
