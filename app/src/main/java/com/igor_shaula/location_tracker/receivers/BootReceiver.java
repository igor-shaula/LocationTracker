package com.igor_shaula.location_tracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.igor_shaula.location_tracker.service.MainService;
import com.igor_shaula.location_tracker.utilities.MyLog;

/**
 * Created by igor shaula - to reload the service after system reboot \
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, MainService.class));
        MyLog.i("onReceive worked = service is launched");
    }
}