package com.statsnail.roberts.statsnail.utils;

import android.content.ContentValues;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

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

    private ContentValues[] readWindsXml(XmlPullParser pullParser) throws XmlPullParserException, IOException {
        return null;
    }
}
