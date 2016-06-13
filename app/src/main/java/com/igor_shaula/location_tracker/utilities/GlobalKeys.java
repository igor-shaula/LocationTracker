package com.igor_shaula.location_tracker.utilities;

/**
 * Created by igor shaula
 */
public class GlobalKeys {

    public static final String P_I_KEY = "pendingIntent";

    public static final int REQUEST_CODE_MAIN_SERVICE = 10;
    public static final int P_I_CODE_DATA_FROM_GPS = 100;

    public static final String GPS_LATITUDE = "latitude received from GPS via service";
    public static final String GPS_LONGITUDE = "longitude received from GPS via service";
    public static final String GPS_TAKING_TIME = "time of taking coordinates from GPS via service";
    public static final String DISTANCE = "total distance in meters, calculated with location data";

    public static final String EVENT_INET_ON = "internet enabled";
    public static final String EVENT_INET_OFF = "internet disabled";
    public static final String EVENT_GPS_ON = "GPS enabled";
    public static final String EVENT_GPS_OFF = "GPS disabled";
}