package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.util.Xml;

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
        ContentValues[] windsValues = null;

        parser.require(XmlPullParser.START_TAG, ns, "weatherdata");

        while (parser.next() != XmlPullParser.END_TAG) {
            Timber.d("parser: " + parser.getName());
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            String name = parser.getName();
            if (name.equals("product")) {
                Timber.d("parser: " + parser.getName());
            }
        }
        return windsValues;
    }
}
