package com.solveast.geo_tracker.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.solveast.geo_tracker.GlobalKeys;
import com.solveast.geo_tracker.eventbus.RadioStateChangeEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by igor shaula - to react on GPS availability \
 */
public class GpsStateReceiver extends BroadcastReceiver {
   @Override
   public void onReceive(Context context, Intent intent) {

      LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

      if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
         EventBus.getDefault().post(new RadioStateChangeEvent(GlobalKeys.EVENT_GPS_ON));
      else
         EventBus.getDefault().post(new RadioStateChangeEvent(GlobalKeys.EVENT_GPS_OFF));
   }
}