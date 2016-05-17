package com.solveast.geo_tracker;

/**
 * Created by igor shaula
 */
public class GlobalKeys {

   public static final String S_P_NAME = "url from qrCode";
   public static final String S_P_QR_KEY = "shared preferences key for QR-code";
   public static final String P_I_KEY = "pendingIntent";
   public static final String QR_RESULT = "QrResult";
   public static final String QR_KEY = "QR-code key";
   public static final String QR_KEY_INVALID = "QR-code key is not valid";

   public static final int REQUEST_CODE_QR_ACTIVITY = 10;
   public static final int REQUEST_CODE_MAIN_SERVICE = 20;
   public static final int P_I_CODE_DATA_FROM_GPS = 100;
   public static final int P_I_CODE_QR_KEY_INVALID = -100;
   public static final int P_I_CONNECTION_OFF = 201;
   public static final int P_I_CONNECTION_ON = 202;

   public static final String GPS_LATITUDE = "latitude received from GPS via service";
   public static final String GPS_LONGITUDE = "longitude received from GPS via service";
   public static final String GPS_TAKING_TIME = "time of taking coordinates from GPS via service";
   public static final String DISTANCE = "total distance in meters, calculated with location data";
}