package com.mol.muleteer;

/**
 * Created by igor shaula
 */
public class GlobalKeys {

   public static final String LOCAL_BROADCAST_SERVICE_CHECK = "com.mol.muleteer - check service state";
   public static final String START_SERVICE_CHECK = "checking the state of the service at the start of activity";
   public static final String PENDING_INTENT_KEY = "pendingIntent";
   public static final String EXTRA_QR_RESULT = "QrResult";
   public static final String QR_KEY = "QR-code key";

   public static final int QR_ACTIVITY_KEY = 10;
   public static final int P_I_CODE_DATA_FROM_GPS = 100;
   public static final int P_I_CONNECTION_OFF = 201;
   public static final int P_I_CONNECTION_ON = 202;

   public static final String GPS_LATITUDE = "latitude received from GPS via service";
   public static final String GPS_LONGITUDE = "longitude received from GPS via service";
   public static final String GPS_TAKING_TIME = "time of taking coordinates from GPS via service";
}