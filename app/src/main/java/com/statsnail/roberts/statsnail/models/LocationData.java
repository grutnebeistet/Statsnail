package com.statsnail.roberts.statsnail.models;

import java.util.ArrayList;

/**
 * Created by Adrian on 23/10/2017.
 */

public class LocationData {
    public String stationName;
    public String stationCode;
    public String latitude;
    public String longitude;
    public String dataType;
    public String errorResponse;
    public ArrayList<Waterlevel> waterlevels;

    public LocationData(String name, String code, String lat, String lon, String type, ArrayList<Waterlevel> levels, String error) {
        stationName = name;
        stationCode = code;
        latitude = lat;
        longitude = lon;
        dataType = type;
        waterlevels = levels;
        errorResponse = error;
    }

    public static class Waterlevel {
        public String waterValue;
        public String dateTime;
        public String flag;

        public Waterlevel(String value, String dateTime, String flag) {
            this.waterValue = value;
            this.dateTime = dateTime;
            this.flag = flag;
        }

    }

}
