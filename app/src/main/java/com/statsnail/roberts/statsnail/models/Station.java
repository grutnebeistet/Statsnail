package com.statsnail.roberts.statsnail.models;

/**
 * Created by Adrian on 25/10/2017.
 */

public class Station {
    public String stationName;
    public String stationCode;
    public String latitude;
    public String longitude;

    public Station(String stationName, String stationCode, String latitude, String longitude) {
        this.stationName = stationName;
        this.stationCode = stationCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
