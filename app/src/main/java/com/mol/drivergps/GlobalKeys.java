package com.mol.drivergps;

/**
 * Created by igor shaula
 */
public class GlobalKeys {

   public static final String LOCAL_BROADCAST_SERVICE_CHECK = "com.mol.drivergps - check service state";
   public static final String START_SERVICE_CHECK = "checking the state of the service at the start of activity";
   public static final String EXTRA_QR_RESULT = "QrResult";
   public static final String QR_KEY = "QR-code key";
   public static final String PENDING_INTENT_KEY = "pendingIntent";

   public static final int QR_ACTIVITY_KEY = 10;
   public static final int P_I_CODE_DATA_FROM_GPS = -100;
   public static final int P_I_CODE_CONNECTION_OK = -200;

   public static final int P_I_PROVIDER_DISABLED = 100;
   public static final int P_I_PROVIDER_ENABLED = 101;
   public static final int P_I_LOCATION_CHANGED = 102;
   public static final int P_I_STATUS_CHANGED = 103;
   public static final int P_I_CONNECTION_OFF = 200;
   public static final int P_I_CONNECTION_ON = 201;

   public static final String GPS_LATITUDE = "latitude received from GPS via service";
   public static final String GPS_LONGITUDE = "longitude received from GPS via service";
   public static final String GPS_TAKING_TIME = "time of taking coordinates from GPS via service";

   public static final String CONNECTION_RESULT = "result from connection after onResponse-method";
}