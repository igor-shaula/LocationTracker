package com.igor_shaula.location_tracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.igor_shaula.location_tracker.events.RadioStateChangeEvent;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;

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